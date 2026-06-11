package com.docflow.ai.common.pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("消费者限流器测试")
class ConsumerRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("未超限时应允许消费")
    void shouldAllowWhenUnderLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        ConsumerRateLimiter limiter = new ConsumerRateLimiter(redisTemplate);
        boolean acquired = limiter.tryAcquire("test-consumer", 10);

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("超出限时应拒绝消费")
    void shouldRejectWhenOverLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(11L);

        ConsumerRateLimiter limiter = new ConsumerRateLimiter(redisTemplate);
        boolean acquired = limiter.tryAcquire("test-consumer", 10);

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("获取当前计数")
    void shouldGetCurrentCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("5");

        ConsumerRateLimiter limiter = new ConsumerRateLimiter(redisTemplate);
        long count = limiter.getCurrentCount("test-consumer");

        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("重置计数")
    void shouldReset() {
        ConsumerRateLimiter limiter = new ConsumerRateLimiter(redisTemplate);
        limiter.reset("test-consumer");

        verify(redisTemplate).delete(anyString());
    }
}
