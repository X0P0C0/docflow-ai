package com.docflow.ai.common.pattern;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MessageDeduplicationServiceTest {

    @Test
    void tryMarkProcessedShouldReturnTrueForNewEvent() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        MessageDeduplicationService dedup = new MessageDeduplicationService(redisTemplate);
        assertTrue(dedup.tryMarkProcessed("event-123"));
    }

    @Test
    void tryMarkProcessedShouldReturnFalseForDuplicate() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        MessageDeduplicationService dedup = new MessageDeduplicationService(redisTemplate);
        assertFalse(dedup.tryMarkProcessed("event-123"));
    }

    @Test
    void tryMarkProcessedShouldReturnTrueForNullEventId() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        MessageDeduplicationService dedup = new MessageDeduplicationService(redisTemplate);
        assertTrue(dedup.tryMarkProcessed(null));
        assertTrue(dedup.tryMarkProcessed(""));
    }
}
