package com.docflow.ai.common.ratelimit;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

/**
 * 限流 AOP 切面。
 * <p>
 * 基于 Redis INCR + EXPIRE 实现滑动窗口计数器：
 * <ol>
 *   <li>生成 key = rate-limit:{dimension}:{identifier}:{uri}</li>
 *   <li>INCR key，如果值 == 1 则设置 EXPIRE</li>
 *   <li>如果计数 > maxRequests，抛出 BusinessException(RATE_LIMITED)</li>
 * </ol>
 * <p>
 * 这是最简单的限流实现，生产环境可升级为 Redisson 的 RateLimiter（令牌桶算法）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(rateLimit.windowSeconds()));
        }

        if (count != null && count > rateLimit.maxRequests()) {
            log.warn("接口限流: key={} count={} max={}", key, count, rateLimit.maxRequests());
            throw new BusinessException(ResultCode.ERROR);
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs != null ? attrs.getRequest() : null;
        String uri = request != null ? request.getRequestURI() : "unknown";

        String identifier;
        switch (rateLimit.dimension()) {
            case USER -> {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof AuthUserPrincipal principal) {
                    identifier = "u:" + principal.getUserId();
                } else {
                    identifier = "ip:" + getClientIp(request);
                }
            }
            case GLOBAL -> identifier = "global";
            default -> identifier = "ip:" + getClientIp(request);
        }

        return KEY_PREFIX + identifier + ":" + uri;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}