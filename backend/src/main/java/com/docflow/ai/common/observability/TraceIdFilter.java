package com.docflow.ai.common.observability;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 分布式追踪过滤器。
 * 技术点：自动生成 TraceId 并注入 MDC，贯穿请求全链路。
 * - 请求入口生成 16 位 traceId
 * - 放入 MDC → Logback 自动输出到日志
 * - 放入响应头 X-Trace-Id → 前端可关联错误日志
 * - 请求结束清理 MDC 防止线程池泄漏
 */
@Component
@org.springframework.context.annotation.Profile("!test")
@Order(1)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 优先使用上游传入的 traceId（微服务链路透传）
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        MDC.put(MDC_KEY, traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
