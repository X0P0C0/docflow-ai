package com.docflow.ai.common.security;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Component
public class PasswordPolicy {

    private static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";
    private static final String ACCOUNT_LOCK_PREFIX = "auth:account:lock:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+=\\-\\[\\]{};:,.<>?]");

    private final StringRedisTemplate redisTemplate;

    public PasswordPolicy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void validatePassword(String password, String username) {
        if (password == null || password.length() < 8) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "密码长度不能少于 8 位");
        }
        if (password.length() > 64) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "密码长度不能超过 64 位");
        }
        if (username != null && password.toLowerCase().contains(username.toLowerCase())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "密码不能包含用户名");
        }
        int typeCount = 0;
        if (LOWERCASE.matcher(password).find()) typeCount++;
        if (UPPERCASE.matcher(password).find()) typeCount++;
        if (DIGIT.matcher(password).find()) typeCount++;
        if (SPECIAL.matcher(password).find()) typeCount++;
        if (typeCount < 3) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "密码必须包含大小写字母、数字、特殊字符中的至少 3 种");
        }
    }

    public int recordLoginFailure(String username) {
        String key = LOGIN_FAIL_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= MAX_LOGIN_ATTEMPTS) {
            String lockKey = ACCOUNT_LOCK_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, "locked", LOCKOUT_MINUTES, TimeUnit.MINUTES);
        }
        return count != null ? count.intValue() : 0;
    }

    public long checkAccountLocked(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    public void clearLoginFailures(String username) {
        redisTemplate.delete(LOGIN_FAIL_PREFIX + username);
        redisTemplate.delete(ACCOUNT_LOCK_PREFIX + username);
    }
}
