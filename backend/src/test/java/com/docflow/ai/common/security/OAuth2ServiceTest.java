package com.docflow.ai.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2 服务测试")
class OAuth2ServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Test
    @DisplayName("生成授权码应返回 32 位字符串")
    void shouldGenerateAuthCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        OAuth2Service service = new OAuth2Service(redisTemplate);
        String code = service.generateAuthorizationCode("client-1", "user-1", "http://callback");

        assertThat(code).hasSize(32);
        verify(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("验证有效 Token 应返回数据")
    void shouldValidateValidToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("client-1:user-1");

        OAuth2Service service = new OAuth2Service(redisTemplate);
        String data = service.validateToken("valid-token");

        assertThat(data).isEqualTo("client-1:user-1");
    }

    @Test
    @DisplayName("验证无效 Token 应返回 null")
    void shouldReturnNullForInvalidToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        OAuth2Service service = new OAuth2Service(redisTemplate);
        String data = service.validateToken("invalid-token");

        assertThat(data).isNull();
    }
}
