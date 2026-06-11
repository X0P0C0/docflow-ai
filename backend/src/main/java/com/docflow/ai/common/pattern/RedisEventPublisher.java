package com.docflow.ai.common.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

/**
 * Redis Pub/Sub 异步事件发布器。
 * 增强版：发布同时持久化到 Redis Stream，支持消息回溯。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL = "docflow:events";
    private static final String STREAM_KEY = "docflow:events:stream";

    @Async
    public void publish(DomainEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            // 1. Pub/Sub 实时通知
            redisTemplate.convertAndSend(CHANNEL, json);
            // 2. Stream 持久化（支持消息回溯和消费者组）
            redisTemplate.opsForStream().add(STREAM_KEY, java.util.Map.of("data", json));
            log.info("Event published: eventId={}, type={}, entity={}({})",
                    event.getEventId(), event.getEventType(), event.getEntityType(), event.getEntityId());
        } catch (Exception e) {
            log.error("Event publish failed: {}", e.getMessage(), e);
        }
    }
}
