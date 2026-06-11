package com.docflow.ai.common.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("多级缓存服务测试")
class MultiLevelCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("写入缓存应同时写 L1 和 L2")
    void shouldWriteToBothLevels() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        MultiLevelCacheService cache = new MultiLevelCacheService(redisTemplate);
        cache.put("test-key", "test-value");

        verify(valueOps).set(eq("mlc:test-key"), eq("test-value"), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("L1 命中时不应访问 L2")
    void shouldHitL1WithoutAccessingL2() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        MultiLevelCacheService cache = new MultiLevelCacheService(redisTemplate);
        cache.put("test-key", "test-value");

        String result = cache.get("test-key");
        assertThat(result).isEqualTo("test-value");
    }

    @Test
    @DisplayName("L1 未命中时应从 L2 获取并回填")
    void shouldBackfillL1OnL2Hit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("mlc:test-key")).thenReturn("redis-value");

        MultiLevelCacheService cache = new MultiLevelCacheService(redisTemplate);
        String result = cache.get("test-key");

        assertThat(result).isEqualTo("redis-value");
    }

    @Test
    @DisplayName("删除缓存应同时删 L1 和 L2")
    void shouldEvictBothLevels() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        MultiLevelCacheService cache = new MultiLevelCacheService(redisTemplate);
        cache.put("test-key", "test-value");
        cache.evict("test-key");

        verify(redisTemplate).delete("mlc:test-key");
    }

    @Test
    @DisplayName("缓存统计应正确")
    void shouldReturnStats() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        MultiLevelCacheService cache = new MultiLevelCacheService(redisTemplate);
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        var stats = cache.getStats();
        assertThat(stats.totalEntries()).isEqualTo(2);
        assertThat(stats.validEntries()).isEqualTo(2);
    }
}
