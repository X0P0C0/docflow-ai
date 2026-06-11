package com.docflow.ai.common.observability;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 日志增强过滤器。
 * 技术点：将请求上下文（用户ID、IP、路径）注入 MDC，实现结构化日志。
 * 结合 LogstashEncoder，每条日志自动携带请求上下文。
 */
@Component
@org.springframework.context.annotation.Profile("!test")
@Order(2)
public class LoggingContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        MDC.put("requestURI", httpRequest.getRequestURI());
        MDC.put("clientIp", getClientIp(httpRequest));
        MDC.put("method", httpRequest.getMethod());

        // 注入当前用户ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            MDC.put("userId", auth.getPrincipal().toString());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestURI");
            MDC.remove("clientIp");
            MDC.remove("method");
            MDC.remove("userId");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
