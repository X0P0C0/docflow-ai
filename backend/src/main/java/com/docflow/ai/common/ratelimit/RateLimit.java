package com.docflow.ai.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 * <p>
 * 使用方式：
 * <pre>
 *   @RateLimit(maxRequests = 10, windowSeconds = 60)
 *   @PostMapping("/login")
 *   public ApiResponse&lt;LoginResponse&gt; login(...) { ... }
 * </pre>
 * <p>
 * 原理：基于 Redis 的滑动窗口计数器，key = IP + URI。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口内最大请求数 */
    int maxRequests() default 60;

    /** 时间窗口（秒） */
    int windowSeconds() default 60;

    /** 限流维度：IP（默认）、USER、GLOBAL */
    LimitDimension dimension() default LimitDimension.IP;

    enum LimitDimension {
        IP, USER, GLOBAL
    }
}