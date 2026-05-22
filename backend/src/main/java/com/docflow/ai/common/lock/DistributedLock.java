package com.docflow.ai.common.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "dist-lock:";

    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString();

        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(fullKey, lockValue, timeout.toSeconds(), TimeUnit.SECONDS));

        if (!acquired) {
            throw new LockAcquisitionException("Could not acquire lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            String current = redisTemplate.opsForValue().get(fullKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(fullKey);
            }
        }
    }

    public void executeWithLock(String lockKey, Duration timeout, Runnable action) {
        executeWithLock(lockKey, timeout, () -> {
            action.run();
            return null;
        });
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
