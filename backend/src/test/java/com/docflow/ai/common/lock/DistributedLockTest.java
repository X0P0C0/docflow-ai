package com.docflow.ai.common.lock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("分布式锁测试")
class DistributedLockTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("成功获取锁并执行")
    void shouldAcquireLockAndExecute() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(valueOps.get(anyString())).thenReturn("test-value");

        DistributedLock lock = new DistributedLock(redisTemplate);
        AtomicBoolean executed = new AtomicBoolean(false);

        lock.executeWithLock("test-key", Duration.ofSeconds(5), () -> executed.set(true));

        assertThat(executed.get()).isTrue();
    }

    @Test
    @DisplayName("获取锁失败应抛异常")
    void shouldThrowWhenLockNotAcquired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        DistributedLock lock = new DistributedLock(redisTemplate);

        assertThatThrownBy(() ->
            lock.executeWithLock("test-key", Duration.ofSeconds(5), () -> {})
        ).isInstanceOf(DistributedLock.LockAcquisitionException.class);
    }

    @Test
    @DisplayName("执行完成后应释放锁（当锁值匹配时）")
    void shouldReleaseLockAfterExecution() {
        AtomicReference<String> capturedLockValue = new AtomicReference<>();

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> {
                    capturedLockValue.set(invocation.getArgument(1));
                    return true;
                });
        when(valueOps.get(anyString()))
                .thenAnswer(invocation -> capturedLockValue.get());

        DistributedLock lock = new DistributedLock(redisTemplate);
        lock.executeWithLock("test-key", Duration.ofSeconds(5), () -> {});

        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("返回值应正确传递")
    void shouldReturnValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(valueOps.get(anyString())).thenReturn("test-value");

        DistributedLock lock = new DistributedLock(redisTemplate);
        String result = lock.executeWithLock("test-key", Duration.ofSeconds(5), () -> "hello");

        assertThat(result).isEqualTo("hello");
    }
}
