package com.docflow.ai.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String NULL_VAL = "__NULL__";
    private static final String LOCK = "cache:lock:";

    public <T> T getWithCache(String key, int ttl, Class<T> type, Supplier<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_VAL.equals(cached)) return null;
            try { return objectMapper.readValue(cached, type); }
            catch (Exception e) { redisTemplate.delete(key); }
        }
        String lockKey = LOCK + key;
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS));
        if (locked) {
            try {
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    if (NULL_VAL.equals(cached)) return null;
                    try { return objectMapper.readValue(cached, type); } catch (Exception ignored) {}
                }
                T value = loader.get();
                int jitter = ttl + ThreadLocalRandom.current().nextInt(0, ttl / 5 + 1);
                if (value == null) {
                    redisTemplate.opsForValue().set(key, NULL_VAL, 60, TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), jitter, TimeUnit.SECONDS);
                }
                return value;
            } catch (Exception e) {
                log.error("Cache loader failed: {}: {}", key, e.getMessage());
                return loader.get();
            } finally { redisTemplate.delete(lockKey); }
        } else {
            try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return getWithCache(key, ttl, type, loader);
        }
    }

    public void evict(String key) { redisTemplate.delete(key); }
    public void evictPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }


    /**
     * 缓存穿透防护：缓存空值。
     * 当查询数据库也查不到时，缓存一个特殊标记，避免反复查库。
     * TTL 较短（60s），防止长期占用缓存空间。
     */
    public <T> T getWithPenetrationProtection(String key, int ttl, Class<T> type, Supplier<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_VAL.equals(cached)) return null;
            try { return objectMapper.readValue(cached, type); }
            catch (Exception e) { redisTemplate.delete(key); }
        }
        T value = loader.get();
        if (value == null) {
            // 缓存空值，防止穿透
            redisTemplate.opsForValue().set(key, NULL_VAL, 60, TimeUnit.SECONDS);
        } else {
            // 加随机过期时间，防止缓存雪崩
            int jitter = ttl + ThreadLocalRandom.current().nextInt(0, Math.max(ttl / 5, 1));
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), jitter, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Cache write failed: {}", key, e);
            }
        }
        return value;
    }

    /**
     * 缓存击穿防护：互斥锁重建。
     * 当热点 key 过期时，只允许一个线程去查库重建，其他线程等待。
     * 防止大量并发请求同时穿透到数据库。
     */
    public <T> T getWithBreakdownProtection(String key, int ttl, Class<T> type, Supplier<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_VAL.equals(cached)) return null;
            try { return objectMapper.readValue(cached, type); }
            catch (Exception e) { redisTemplate.delete(key); }
        }
        // 互斥锁：只有一个线程能拿到锁去重建缓存
        String lockKey = "cache:mutex:" + key;
        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS));
        if (locked) {
            try {
                // Double-check：拿到锁后再查一次缓存
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    if (NULL_VAL.equals(cached)) return null;
                    try { return objectMapper.readValue(cached, type); } catch (Exception ignored) {}
                }
                T value = loader.get();
                int jitter = ttl + ThreadLocalRandom.current().nextInt(0, Math.max(ttl / 5, 1));
                if (value == null) {
                    redisTemplate.opsForValue().set(key, NULL_VAL, 60, TimeUnit.SECONDS);
                } else {
                    try {
                        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), jitter, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        log.error("Cache write failed: {}", key, e);
                    }
                }
                return value;
            } catch (Exception e) {
                log.error("Cache breakdown handler failed: {}: {}", key, e.getMessage());
                return loader.get();
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 没拿到锁，短暂等待后重试读缓存
            try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return getWithBreakdownProtection(key, ttl, type, loader);
        }
    }

}