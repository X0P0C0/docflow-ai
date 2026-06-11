package com.docflow.ai.common.pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 消费者限流器 —— 保护消费者不被打垮
 * <p>
 * 算法：令牌桶（Token Bucket）
 * <pre>
 *   ┌─────────────────┐
 *   │   令牌桶         │ ← 固定速率放入令牌
 *   │   ████████      │
 *   │   ████████      │
 *   └────────┬────────┘
 *            │
 *   ┌────────▼────────┐
 *   │   消费者         │ ← 有令牌才能消费
 *   └─────────────────┘
 * </pre>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>令牌桶 vs 漏桶 vs 滑动窗口</li>
 *   <li>分布式限流：Redis + Lua 脚本保证原子性</li>
 *   <li>限流维度：QPS、并发数、用户级、接口级</li>
 * </ul>
 */
@Slf4j
@Component
public class ConsumerRateLimiter {

    private static final String RATE_LIMIT_PREFIX = "rate:limit:";
    private static final long DEFAULT_PERMITS_PER_SECOND = 10;

    private final StringRedisTemplate redisTemplate;

    public ConsumerRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取令牌
     * @param key 限流 key（如消费者名称）
     * @return true 表示获取成功，可以消费
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, DEFAULT_PERMITS_PER_SECOND);
    }

    /**
     * 尝试获取令牌（自定义速率）
     */
    public boolean tryAcquire(String key, long permitsPerSecond) {
        String redisKey = RATE_LIMIT_PREFIX + key;

        // 简化版：使用 Redis INCR 实现固定窗口限流
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1L) {
            // 第一个请求，设置窗口过期时间
            redisTemplate.expire(redisKey, 1, TimeUnit.SECONDS);
        }

        if (count != null && count > permitsPerSecond) {
            log.debug("Rate limit exceeded for key: {}, count: {}", key, count);
            return false;
        }

        return true;
    }

    /**
     * 获取当前计数
     */
    public long getCurrentCount(String key) {
        String value = redisTemplate.opsForValue().get(RATE_LIMIT_PREFIX + key);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * 重置计数
     */
    public void reset(String key) {
        redisTemplate.delete(RATE_LIMIT_PREFIX + key);
    }
}
