package com.docflow.ai.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        long startTime = System.currentTimeMillis();

        MDC.put(TraceIdConstants.TRACE_ID, traceId);
        response.setHeader(TraceIdConstants.TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("request completed: method={}, uri={}, status={}, durationMs={}, clientIp={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    resolveClientIp(request));
            MDC.remove(TraceIdConstants.TRACE_ID);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TraceIdConstants.TRACE_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
