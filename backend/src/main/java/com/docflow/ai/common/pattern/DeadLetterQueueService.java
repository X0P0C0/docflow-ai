package com.docflow.ai.common.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 死信队列服务。
 * 处理失败的消息进入 DLQ，定时重试，超过最大重试次数后告警。
 * 技术点：Redis List 作为持久化队列，ZSET 记录重试时间戳。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DLQ_KEY = "docflow:dlq";
    private static final String DLQ_RETRY_TS_KEY = "docflow:dlq:retry_ts";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_INTERVAL_MS = 60_000; // 1 minute

    /**
     * 将处理失败的消息移入死信队列。
     */
    public void addToDeadLetter(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList().rightPush(DLQ_KEY, json);
            redisTemplate.opsForZSet().add(DLQ_RETRY_TS_KEY, event.getEventId(),
                    System.currentTimeMillis() + RETRY_INTERVAL_MS);
            log.warn("Event added to DLQ: eventId={}, type={}, retryCount={}",
                    event.getEventId(), event.getEventType(), event.getRetryCount());
        } catch (Exception e) {
            log.error("Failed to add event to DLQ: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时扫描 DLQ，重试到期的消息。
     * 每 30 秒执行一次。
     */
    @Scheduled(fixedDelay = 30_000)
    public void retryDeadLetters() {
        try {
            long now = System.currentTimeMillis();
            // 获取到期的 eventId 集合
            Set<String> readyIds = redisTemplate.opsForZSet()
                    .rangeByScore(DLQ_RETRY_TS_KEY, 0, now);
            if (readyIds == null || readyIds.isEmpty()) return;

            // 逐个从 DLQ 中取出重试
            Long size = redisTemplate.opsForList().size(DLQ_KEY);
            if (size == null || size == 0) return;

            int retried = 0;
            for (int i = 0; i < size && retried < 10; i++) {
                String json = redisTemplate.opsForList().index(DLQ_KEY, i);
                if (json == null) continue;
                try {
                    DomainEvent event = objectMapper.readValue(json, DomainEvent.class);
                    if (!readyIds.contains(event.getEventId())) continue;

                    if (event.getRetryCount() >= MAX_RETRIES) {
                        // 超过最大重试次数，移除并告警
                        redisTemplate.opsForList().remove(DLQ_KEY, 1, json);
                        redisTemplate.opsForZSet().remove(DLQ_RETRY_TS_KEY, event.getEventId());
                        log.error("DLQ: event exceeded max retries, discarded: eventId={}, type={}",
                                event.getEventId(), event.getEventType());
                    } else {
                        // 更新重试计数，移除旧记录，重新入队等待下次重试
                        redisTemplate.opsForList().remove(DLQ_KEY, 1, json);
                        redisTemplate.opsForZSet().remove(DLQ_RETRY_TS_KEY, event.getEventId());
                        event.setRetryCount(event.getRetryCount() + 1);
                        addToDeadLetter(event);
                        retried++;
                        log.info("DLQ: event re-queued for retry: eventId={}, retryCount={}",
                                event.getEventId(), event.getRetryCount());
                    }
                } catch (Exception e) {
                    log.error("DLQ: failed to process entry at index {}: {}", i, e.getMessage());
                }
            }
            if (retried > 0) {
                log.info("DLQ: retried {} messages", retried);
            }
        } catch (Exception e) {
            log.error("DLQ retry scan failed: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取 DLQ 当前深度（待处理消息数）。
     */
    public long getQueueDepth() {
        Long size = redisTemplate.opsForList().size(DLQ_KEY);
        return size != null ? size : 0;
    }
}
