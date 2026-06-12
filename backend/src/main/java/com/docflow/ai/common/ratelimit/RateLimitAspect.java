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
 * 接口限流 AOP 切面 - 基于 Redis 的滑动窗口计数器
 *
 * 【面试考点】（限流是高并发场景必备知识）
 *
 * 【限流算法对比】
 *   1. 固定窗口：简单但有"临界突刺"问题（窗口边界处可能两倍流量）
 *   2. 滑动窗口：更平滑，本项目使用（Redis INCR + EXPIRE）
 *   3. 漏桶算法：恒定速率处理，适合流量整形
 *   4. 令牌桶算法：允许突发流量，适合 API 限流（Guava RateLimiter / Redisson）
 *
 * 【本项目实现】
 *   Redis INCR + EXPIRE 实现简易滑动窗口：
 *   1. 生成 key = rate-limit:{维度}:{标识}:{URI}
 *   2. INCR key，如果值 == 1 则设置 EXPIRE（窗口开始）
 *   3. 如果计数 > maxRequests，抛出异常（返回 429）
 *
 * 【限流维度】
 *   - USER: 按用户 ID 限流（登录用户）
 *   - IP: 按客户端 IP 限流（未登录用户）
 *   - GLOBAL: 全局限流（保护服务整体）
 *
 * 【使用方式】在 Controller 方法上加注解：
 *   @RateLimit(maxRequests = 100, windowSeconds = 60, dimension = LimitDimension.USER)
 *
 * 【生产环境升级方向】
 *   - Redisson 的 RateLimiter（令牌桶算法，更精确）
 *   - Sentinel（阿里开源，支持熔断+限流+降级一体）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final String KEY_PREFIX = "rate-limit:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 环绕通知：拦截所有标注了 @RateLimit 的方法
     *
     * 【AOP 知识点】
     *   @Around 是最强大的通知类型，可以控制是否执行目标方法
     *   @annotation(rateLimit) 匹配方法上的注解，并将注解实例传入参数
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);

        // Redis INCR 原子操作（线程安全）
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            // 第一次访问，设置窗口过期时间
            redisTemplate.expire(key, Duration.ofSeconds(rateLimit.windowSeconds()));
        }

        // 超过限制，直接拒绝
        if (count != null && count > rateLimit.maxRequests()) {
            log.warn("接口限流: key={} count={} max={}", key, count, rateLimit.maxRequests());
            throw new BusinessException(ResultCode.ERROR);
        }

        return joinPoint.proceed();
    }

    /**
     * 构建限流 key
     * 格式：rate-limit:{维度}:{标识}:{URI}
     * 例如：rate-limit:u:123:/api/tickets（用户 123 访问工单接口的计数）
     */
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

    /** 获取客户端真实 IP（考虑反向代理） */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim(); // 第一个是真实 IP
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