package com.docflow.ai.common.pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("竞态条件处理器测试")
class RaceConditionHandlerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("原子递减应返回扣减后的值")
    void shouldAtomicDecrement() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(9L);

        RaceConditionHandler handler = new RaceConditionHandler(redisTemplate);
        Long result = handler.atomicDecrement("stock:key", 1);

        assertThat(result).isEqualTo(9L);
    }

    @Test
    @DisplayName("库存不足应返回 -1")
    void shouldReturnMinusOneWhenInsufficient() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenReturn(-1L);

        RaceConditionHandler handler = new RaceConditionHandler(redisTemplate);
        Long result = handler.atomicDecrement("stock:key", 100);

        assertThat(result).isEqualTo(-1L);
    }
}
