package com.docflow.ai.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存服务 —— 本地缓存 + Redis 缓存
 * <p>
 * 缓存架构：
 * <pre>
 *   请求 → L1 本地缓存 → L2 Redis 缓存 → 数据库
 * </pre>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>L1 vs L2 缓存的适用场景</li>
 *   <li>缓存一致性问题：如何保证 L1 和 L2 同步</li>
 *   <li>缓存穿透、雪崩、击穿的区别和解决方案</li>
 *   <li>Guava Cache vs Caffeine vs ConcurrentHashMap</li>
 * </ul>
 * <p>
 * 性能对比：
 * <ul>
 *   <li>本地缓存：~0.01ms（纳秒级）</li>
 *   <li>Redis 缓存：~1ms（网络延迟）</li>
 *   <li>数据库查询：~10ms</li>
 * </ul>
 */
@Slf4j
@Component
public class MultiLevelCacheService {

    // L1: 本地缓存（进程内，最快但容量有限）
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    // L2: Redis 缓存（分布式，容量大但有网络延迟）
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "mlc:";
    private static final long LOCAL_TTL_SECONDS = 60;      // L1 过期时间：60秒
    private static final long REDIS_TTL_SECONDS = 300;     // L2 过期时间：5分钟
    private static final int MAX_LOCAL_ENTRIES = 1000;      // L1 最大条目数

    public MultiLevelCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取缓存（先 L1，再 L2）
     */
    public String get(String key) {
        // 1. 查 L1
        CacheEntry localEntry = localCache.get(key);
        if (localEntry != null && !localEntry.isExpired()) {
            log.debug("L1 cache hit: {}", key);
            return localEntry.value;
        }

        // 2. 查 L2
        String redisValue = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
        if (redisValue != null) {
            log.debug("L2 cache hit: {}", key);
            // 回填 L1
            putLocal(key, redisValue);
            return redisValue;
        }

        log.debug("Cache miss: {}", key);
        return null;
    }

    /**
     * 写入缓存（同时写 L1 和 L2）
     */
    public void put(String key, String value) {
        putLocal(key, value);
        redisTemplate.opsForValue().set(CACHE_PREFIX + key, value, REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("Cache put: {}", key);
    }

    /**
     * 删除缓存（同时删 L1 和 L2）
     */
    public void evict(String key) {
        localCache.remove(key);
        redisTemplate.delete(CACHE_PREFIX + key);
        log.debug("Cache evict: {}", key);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        localCache.clear();
        // 注意：生产环境慎用 keys 命令
        java.util.Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("All cache cleared");
    }

    /**
     * 获取缓存统计
     */
    public CacheStats getStats() {
        long localCount = localCache.size();
        long localExpired = localCache.values().stream().filter(CacheEntry::isExpired).count();
        return new CacheStats(localCount, localCount - localExpired, localExpired);
    }

    private void putLocal(String key, String value) {
        // 简单的容量保护
        if (localCache.size() >= MAX_LOCAL_ENTRIES) {
            // 清除过期条目
            localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            // 如果还是满了，清除最老的
            if (localCache.size() >= MAX_LOCAL_ENTRIES) {
                String oldest = localCache.keySet().iterator().next();
                localCache.remove(oldest);
            }
        }
        localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + LOCAL_TTL_SECONDS * 1000));
    }

    private record CacheEntry(String value, long expireAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    public record CacheStats(long totalEntries, long validEntries, long expiredEntries) {}
}
