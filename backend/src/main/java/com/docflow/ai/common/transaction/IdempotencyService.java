package com.docflow.ai.common.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 幂等性服务：防止重复提交。
 * 
 * 原理：客户端生成唯一 idempotencyKey，服务端用 Redis SETNX 做去重。
 * - 第一次请求：SETNX 成功，执行业务逻辑，缓存结果
 * - 重复请求：SETNX 失败，直接返回缓存的结果
 * 
 * 典型场景：
 * - 用户快速双击提交按钮
 * - 网络超时后客户端自动重试
 * - 支付回调重复通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    /**
     * 执行幂等操作。
     * 
     * @param key      幂等键（通常由客户端生成的 UUID）
     * @param loader   实际业务逻辑
     * @param <T>      返回类型
     * @return 业务结果；如果是重复请求，返回 null（前端应据此判断）
     */
    public <T> T execute(String key, Supplier<T> loader) {
        String redisKey = PREFIX + key;
        // SETNX: 只有 key 不存在时才设置成功
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "processing", TTL);
        if (Boolean.FALSE.equals(acquired)) {
            log.warn("Duplicate request detected: key={}", key);
            return null; // 前端收到 null 知道是重复请求
        }
        try {
            T result = loader.get();
            // 缓存结果，后续重复请求可直接返回
            redisTemplate.opsForValue().set(redisKey, "done", TTL);
            return result;
        } catch (Exception e) {
            // 业务失败，删除幂等键，允许重试
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    /**
     * 检查是否是重复请求。
     */
    public boolean isDuplicate(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + key));
    }
}
