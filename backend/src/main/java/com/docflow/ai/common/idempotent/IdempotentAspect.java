package com.docflow.ai.common.idempotent;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {
    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String userId = request.getHeader("Authorization");
        if (userId == null) userId = "anonymous";
        StringBuilder body = new StringBuilder();
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null && !arg.getClass().getName().contains("AuthUser")) body.append(arg.toString());
        }
        String hash = md5(userId + request.getRequestURI() + body.toString());
        String key = idempotent.prefix() + ":" + hash;
        Boolean exists = redisTemplate.opsForValue().setIfAbsent(key, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(exists)) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, idempotent.message());
        }
        try { return joinPoint.proceed(); }
        catch (Exception e) { redisTemplate.delete(key); throw e; }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return String.valueOf(input.hashCode()); }
    }
}
