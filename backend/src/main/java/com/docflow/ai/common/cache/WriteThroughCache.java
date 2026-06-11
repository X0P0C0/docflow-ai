package com.docflow.ai.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Write-Through 缓存模式
 * <p>
 * 三种缓存写入模式对比：
 * <pre>
 *   1. Cache-Aside（旁路缓存）：应用同时写缓存和数据库
 *   2. Write-Through（写穿透）：应用写缓存，缓存自动写数据库
 *   3. Write-Behind（写回）：应用写缓存，缓存异步写数据库
 * </pre>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>Cache-Aside 最常用，实现简单</li>
 *   <li>Write-Through 保证一致性，但写入延迟高</li>
 *   <li>Write-Behind 写入快，但有数据丢失风险</li>
 *   <li>读多写少用 Cache-Aside，写多读少用 Write-Behind</li>
 * </ul>
 */
@Slf4j
@Component
public class WriteThroughCache {

    private final StringRedisTemplate redisTemplate;

    public WriteThroughCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Write-Through 写入
     * @param key 缓存 key
     * @param value 缓存值
     * @param dbWriter 数据库写入函数
     */
    public <T> void writeThrough(String key, T value, Function<T, Boolean> dbWriter) {
        // 1. 先写数据库
        boolean success = dbWriter.apply(value);
        if (!success) {
            log.error("Database write failed for key: {}", key);
            throw new RuntimeException("数据库写入失败");
        }

        // 2. 再写缓存
        redisTemplate.opsForValue().set(key, value.toString());
        log.debug("Write-Through completed for key: {}", key);
    }

    /**
     * Write-Behind 写入（异步）
     */
    public <T> void writeBehind(String key, T value, Function<T, Boolean> dbWriter) {
        // 1. 先写缓存（快速返回）
        redisTemplate.opsForValue().set(key, value.toString());

        // 2. 异步写数据库（简化版，实际应使用消息队列）
        new Thread(() -> {
            try {
                boolean success = dbWriter.apply(value);
                if (!success) {
                    log.error("Write-Behind database write failed for key: {}", key);
                    // 应该加入重试队列
                }
            } catch (Exception e) {
                log.error("Write-Behind error for key: {}", key, e);
            }
        }).start();

        log.debug("Write-Behind initiated for key: {}", key);
    }
}
