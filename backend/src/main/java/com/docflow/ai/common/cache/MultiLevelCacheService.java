package com.docflow.ai.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存服务 - L1 本地缓存 + L2 Redis 缓存
 *
 * 【面试考点】（缓存是面试重灾区，必须掌握）
 *
 * 缓存架构：
 *   请求 -> L1 本地缓存 -> L2 Redis 缓存 -> 数据库
 *
 * 性能对比：
 *   本地缓存：<0.01ms（纳秒级，进程内访问）
 *   Redis 缓存：~1ms（网络延迟）
 *   数据库查询：~10ms（磁盘 IO）
 *
 * 【缓存穿透】查询不存在的数据，每次都打到数据库
 *   解决：布隆过滤器 / 缓存空值（本项目用缓存空值）
 *
 * 【缓存雪崩】大量缓存同时过期，请求全部打到数据库
 *   解决：过期时间加随机值 / 多级缓存（本项目用多级缓存）
 *   L1 过期 60s，L2 过期 300s，不会同时失效
 *
 * 【缓存击穿】热点 key 过期，大量并发请求打到数据库
 *   解决：分布式锁（本项目有 DistributedLock）/ 逻辑过期
 *
 * 【L1 vs L2 选择】
 *   L1 (ConcurrentHashMap)：进程内，最快，但容量有限，多实例不共享
 *   L2 (Redis)：分布式，容量大，多实例共享，但有网络延迟
 *   本项目两级都用：L1 命中率高时直接返回，L1 未命中查 L2，L2 未命中查数据库
 */
@Slf4j
@Component
public class MultiLevelCacheService {

    /** L1: 本地缓存（进程内，最快但容量有限，多实例不共享） */
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    /** L2: Redis 缓存（分布式，容量大，多实例共享，但有网络延迟） */
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "mlc:";
    private static final long LOCAL_TTL_SECONDS = 60;      // L1 过期时间：60秒
    private static final long REDIS_TTL_SECONDS = 300;     // L2 过期时间：5分钟
    private static final int MAX_LOCAL_ENTRIES = 1000;      // L1 最大条目数（防内存溢出）

    public MultiLevelCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取缓存（先 L1，再 L2，都未命中返回 null）
     *
     * 【面试考点】缓存读取的"穿透"问题
     *   如果返回 null，调用方应该查数据库并将结果写入缓存
     *   本项目没有在 get 方法内自动回源（避免循环依赖）
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
            // 回填 L1（下次访问更快）
            putLocal(key, redisValue);
            return redisValue;
        }

        log.debug("Cache miss: {}", key);
        return null;
    }

    /**
     * 写入缓存（同时写 L1 和 L2，保证一致性）
     *
     * 【面试考点】双写一致性问题
     *   本项目采用"先写 L1 再写 L2"的策略
     *   生产环境应考虑：先删缓存再写数据库 / 延迟双删 / 基于消息队列异步同步
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
     *
     * 【注意】生产环境慎用 keys 命令（会阻塞 Redis）
     * 应该用 SCAN 命令分批删除，或维护一个 key 集合
     */
    public void clear() {
        localCache.clear();
        java.util.Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("All cache cleared");
    }

    /**
     * 获取缓存统计信息（用于监控和调优）
     */
    public CacheStats getStats() {
        long localCount = localCache.size();
        long localExpired = localCache.values().stream().filter(CacheEntry::isExpired).count();
        return new CacheStats(localCount, localCount - localExpired, localExpired);
    }

    /** 写入 L1 缓存（带容量保护） */
    private void putLocal(String key, String value) {
        // 简单的容量保护：超过上限时清理过期条目
        if (localCache.size() >= MAX_LOCAL_ENTRIES) {
            localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            // 如果还是满了，移除最老的（简单策略，生产环境可用 LRU）
            if (localCache.size() >= MAX_LOCAL_ENTRIES) {
                String oldest = localCache.keySet().iterator().next();
                localCache.remove(oldest);
            }
        }
        localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + LOCAL_TTL_SECONDS * 1000));
    }

    /** 缓存条目（值 + 过期时间戳） */
    private record CacheEntry(String value, long expireAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    /** 缓存统计（总条目数 / 有效条目数 / 过期条目数） */
    public record CacheStats(long totalEntries, long validEntries, long expiredEntries) {}
}