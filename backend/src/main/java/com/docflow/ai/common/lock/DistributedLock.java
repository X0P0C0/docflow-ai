package com.docflow.ai.common.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁 - 基于 Redis 的 SETNX 实现
 *
 * 【面试考点】（分布式锁是高频面试题）
 *
 * 【为什么需要分布式锁？】
 *   单机锁（synchronized/ReentrantLock）只在单个 JVM 内有效
 *   多实例部署时，需要跨进程的锁机制
 *   例如：防止同一工单被多个客服同时认领
 *
 * 【Redis 分布式锁的实现原理】
 *   SET key value NX EX timeout（原子操作：不存在才设置 + 设置过期时间）
 *   NX = Not Exists（只有 key 不存在时才成功）
 *   EX = Expire（设置过期时间，防止死锁）
 *
 * 【释放锁的安全性】
 *   不能直接 DELETE key（可能误删别人的锁）
 *   必须先 GET 检查 value 是否是自己设置的（UUID 标识）
 *   【面试考点】GET + DELETE 不是原子操作，生产环境应该用 Lua 脚本保证原子性
 *
 * 【Redisson vs 手写】
 *   本项目手写用于学习理解原理
 *   生产环境推荐 Redisson（自动续期/可重入/红锁支持）
 *
 * 【应用场景】
 *   - 工单认领：同一工单不能被多人同时认领
 *   - 库存扣减：防止超卖
 *   - 定时任务：防止多实例重复执行
 */
@Component
@ConditionalOnBean(StringRedisTemplate.class)
@RequiredArgsConstructor
public class DistributedLock {

    private final StringRedisTemplate redisTemplate;
    private static final String LOCK_PREFIX = "dist-lock:";

    /**
     * 带锁执行（有返回值版本）
     *
     * @param lockKey 锁的 key（会加前缀防止冲突）
     * @param timeout 锁的超时时间（防死锁）
     * @param action  要执行的业务逻辑
     * @return 业务逻辑的返回值
     * @throws LockAcquisitionException 获取锁失败时抛出
     */
    public <T> T executeWithLock(String lockKey, Duration timeout, Supplier<T> action) {
        String fullKey = LOCK_PREFIX + lockKey;
        String lockValue = UUID.randomUUID().toString(); // 唯一标识，防止误删别人的锁

        // 尝试获取锁（SETNX + EXPIRE 原子操作）
        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(fullKey, lockValue, timeout.toSeconds(), TimeUnit.SECONDS));

        if (!acquired) {
            throw new LockAcquisitionException("Could not acquire lock: " + lockKey);
        }

        try {
            return action.get();
        } finally {
            // 释放锁：先检查是否是自己的锁，再删除（防止误删）
            // 【面试考点】这里不是原子操作，生产环境应该用 Lua 脚本：
            // if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end
            String current = redisTemplate.opsForValue().get(fullKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(fullKey);
            }
        }
    }

    /** 无返回值版本 */
    public void executeWithLock(String lockKey, Duration timeout, Runnable action) {
        executeWithLock(lockKey, timeout, () -> {
            action.run();
            return null;
        });
    }

    /** 获取锁失败时抛出的异常 */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}