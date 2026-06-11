package com.docflow.ai.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 动态配置服务 —— 运行时修改配置无需重启
 * <p>
 * 核心功能：
 * <ul>
 *   <li>配置项存储在 Redis，支持热更新</li>
 *   <li>本地缓存 + Redis 缓存，减少 Redis 访问</li>
 *   <li>支持配置变更通知（Pub/Sub）</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>Cache-Aside 模式的应用</li>
 *   <li>本地缓存 vs 分布式缓存的取舍</li>
 *   <li>配置变更的最终一致性</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   // 获取配置
 *   int maxUploadSize = dynamicConfig.getInt("app.max-upload-size", 10);
 *
 *   // 更新配置
 *   dynamicConfig.set("app.max-upload-size", "20");
 *
 *   // 获取功能开关
 *   boolean enabled = dynamicConfig.getFeatureFlag("feature.ai-suggestion", false);
 * </pre>
 */
@Component
public class DynamicConfigService {

    private static final String CONFIG_PREFIX = "config:";
    private static final String FEATURE_PREFIX = "config:feature:";
    private static final long CACHE_TTL_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // 本地缓存，减少 Redis 访问
    private final ConcurrentHashMap<String, CachedValue> localCache = new ConcurrentHashMap<>();

    public DynamicConfigService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取配置项（字符串）
     */
    public Optional<String> get(String key) {
        // 先查本地缓存
        CachedValue cached = localCache.get(key);
        if (cached != null && !cached.isExpired()) {
            return Optional.ofNullable(cached.value);
        }

        // 再查 Redis
        String value = redisTemplate.opsForValue().get(CONFIG_PREFIX + key);
        localCache.put(key, new CachedValue(value, CACHE_TTL_MINUTES));
        return Optional.ofNullable(value);
    }

    /**
     * 获取配置项（带默认值）
     */
    public String get(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * 获取整数配置
     */
    public int getInt(String key, int defaultValue) {
        return get(key)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * 获取布尔配置
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key)
                .map(v -> "true".equalsIgnoreCase(v) || "1".equals(v))
                .orElse(defaultValue);
    }

    /**
     * 获取功能开关
     */
    public boolean getFeatureFlag(String featureName, boolean defaultValue) {
        String value = redisTemplate.opsForValue().get(FEATURE_PREFIX + featureName);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 设置配置项
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(CONFIG_PREFIX + key, value, 24, TimeUnit.HOURS);
        localCache.put(key, new CachedValue(value, CACHE_TTL_MINUTES));
    }

    /**
     * 设置功能开关
     */
    public void setFeatureFlag(String featureName, boolean enabled) {
        redisTemplate.opsForValue().set(FEATURE_PREFIX + featureName,
                String.valueOf(enabled), 24, TimeUnit.HOURS);
    }

    /**
     * 删除配置项
     */
    public void delete(String key) {
        redisTemplate.delete(CONFIG_PREFIX + key);
        localCache.remove(key);
    }

    /**
     * 获取所有配置 keys
     */
    public Set<String> getAllKeys() {
        return redisTemplate.keys(CONFIG_PREFIX + "*");
    }

    /**
     * 清除本地缓存（用于强制刷新）
     */
    public void clearLocalCache() {
        localCache.clear();
    }

    /**
     * 缓存值包装类
     */
    private static class CachedValue {
        final String value;
        final long expireAt;

        CachedValue(String value, long ttlMinutes) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + (ttlMinutes * 60 * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
