package com.docflow.ai.common.security;

import com.docflow.ai.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("密码策略测试")
class PasswordPolicyTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("密码长度不足应抛异常")
    void shouldRejectShortPassword() {
        PasswordPolicy policy = new PasswordPolicy(redisTemplate);
        assertThatThrownBy(() -> policy.validatePassword("Ab1!", "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码长度不能少于 8 位");
    }

    @Test
    @DisplayName("密码包含用户名应抛异常")
    void shouldRejectPasswordContainingUsername() {
        PasswordPolicy policy = new PasswordPolicy(redisTemplate);
        assertThatThrownBy(() -> policy.validatePassword("Admin123!@#", "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不能包含用户名");
    }

    @Test
    @DisplayName("密码强度不足应抛异常")
    void shouldRejectWeakPassword() {
        PasswordPolicy policy = new PasswordPolicy(redisTemplate);
        assertThatThrownBy(() -> policy.validatePassword("abcdefgh", "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码必须包含");
    }

    @Test
    @DisplayName("强密码应通过验证")
    void shouldAcceptStrongPassword() {
        PasswordPolicy policy = new PasswordPolicy(redisTemplate);
        policy.validatePassword("MyP@ssw0rd", "admin");
        // No exception means pass
    }

    @Test
    @DisplayName("账户锁定检查")
    void shouldCheckAccountLocked() {
        when(redisTemplate.getExpire(anyString(), any())).thenReturn(1800L);
        PasswordPolicy policy = new PasswordPolicy(redisTemplate);
        long ttl = policy.checkAccountLocked("admin");
        assertThat(ttl).isGreaterThan(0);
    }
}
