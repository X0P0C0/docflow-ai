package com.docflow.ai.common.pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息去重服务。
 * 利用 Redis SETNX 实现幂等消费：同一 eventId 只会被处理一次。
 * 技术点：分布式去重 + 过期时间防止 key 无限增长。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageDeduplicationService {

    private final StringRedisTemplate redisTemplate;

    private static final String DEDUP_PREFIX = "docflow:dedup:event:";
    private static final long DEDUP_TTL_HOURS = 24;

    /**
     * 标记事件为已处理。如果已存在则返回 false（重复消息）。
     */
    public boolean tryMarkProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) return true;
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(DEDUP_PREFIX + eventId, "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(result)) {
            log.debug("Duplicate event detected, skipping: eventId={}", eventId);
            return false;
        }
        return true;
    }

    /**
     * 检查事件是否已处理。
     */
    public boolean isProcessed(String eventId) {
        if (eventId == null || eventId.isEmpty()) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(DEDUP_PREFIX + eventId));
    }
}
