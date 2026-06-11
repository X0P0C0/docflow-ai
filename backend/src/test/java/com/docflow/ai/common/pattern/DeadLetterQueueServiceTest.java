package com.docflow.ai.common.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ZSetOperations;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeadLetterQueueServiceTest {

    @Test
    void addToDeadLetterShouldPushToList() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ListOperations<String, String> listOps = mock(ListOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForZSet()).thenReturn(zsetOps);
        when(listOps.rightPush(anyString(), anyString())).thenReturn(1L);
        when(zsetOps.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        DeadLetterQueueService dlq = new DeadLetterQueueService(redisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()));
        DomainEvent event = DomainEvent.of("TEST", 1L, "TICKET", null);
        dlq.addToDeadLetter(event);

        verify(listOps).rightPush(eq("docflow:dlq"), anyString());
        verify(zsetOps).add(eq("docflow:dlq:retry_ts"), eq(event.getEventId()), anyDouble());
    }

    @Test
    void getQueueDepthShouldReturnZeroWhenEmpty() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ListOperations<String, String> listOps = mock(ListOperations.class);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.size("docflow:dlq")).thenReturn(0L);

        DeadLetterQueueService dlq = new DeadLetterQueueService(redisTemplate, new ObjectMapper().registerModule(new JavaTimeModule()));
        assertEquals(0, dlq.getQueueDepth());
    }
}
