package com.docflow.ai.common.pattern;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 竞态条件处理器 —— 防止并发操作导致数据不一致
 * <p>
 * 什么是竞态条件？
 * <pre>
 *   线程A: 读取库存 10
 *   线程B: 读取库存 10
 *   线程A: 扣减库存 → 9
 *   线程B: 扣减库存 → 9（错误！应该是 8）
 * </pre>
 * <p>
 * 解决方案：
 * <ul>
 *   <li>方案1：悲观锁（SELECT ... FOR UPDATE）</li>
 *   <li>方案2：乐观锁（版本号 CAS）</li>
 *   <li>方案3：Redis 原子操作</li>
 *   <li>方案4：分布式锁</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>CAS（Compare And Swap）原理</li>
 *   <li>ABA 问题及解决方案</li>
 *   <li>乐观锁重试机制</li>
 * </ul>
 */
@Slf4j
@Component
public class RaceConditionHandler {

    private final StringRedisTemplate redisTemplate;

    public RaceConditionHandler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 原子递减（库存扣减场景）
     * @param key 库存 key
     * @param amount 扣减数量
     * @return 扣减后的值，-1 表示库存不足
     */
    public Long atomicDecrement(String key, long amount) {
        // 使用 Redis Lua 脚本保证原子性
        String script = """
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current < tonumber(ARGV[1]) then
                return -1
            end
            return redis.call('DECRBY', KEYS[1], ARGV[1])
            """;

        Object result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(key),
                String.valueOf(amount)
        );

        return result != null ? (Long) result : -1L;
    }

    /**
     * CAS 操作（Compare And Swap）
     * @param key 要更新的 key
     * @param expectedValue 期望的当前值
     * @param newValue 新值
     * @return true 表示更新成功
     */
    public boolean compareAndSwap(String key, String expectedValue, String newValue) {
        // 使用 Redis WATCH + MULTI 实现 CAS
        redisTemplate.watch(key);

        String currentValue = redisTemplate.opsForValue().get(key);
        if (!expectedValue.equals(currentValue)) {
            redisTemplate.unwatch();
            return false;
        }

        try {
            redisTemplate.multi();
            redisTemplate.opsForValue().set(key, newValue);
            redisTemplate.exec();
            return true;
        } catch (Exception e) {
            log.warn("CAS operation failed for key: {}", key, e);
            return false;
        }
    }

    /**
     * 乐观锁重试
     * @param maxRetries 最大重试次数
     */
    public boolean compareAndSwapWithRetry(String key, String expectedValue,
                                            String newValue, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            if (compareAndSwap(key, expectedValue, newValue)) {
                return true;
            }
            log.debug("CAS retry {}/{} for key: {}", i + 1, maxRetries, key);
            try {
                Thread.sleep(10); // 短暂等待后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
