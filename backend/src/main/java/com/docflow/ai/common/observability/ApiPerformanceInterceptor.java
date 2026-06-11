package com.docflow.ai.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 性能监控拦截器。
 * 技术点：自动采集每个 API 端点的响应时间，暴露到 Prometheus。
 */
@Component
public class ApiPerformanceInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;

    public ApiPerformanceInterceptor(@Autowired(required = false) MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private static final String TIMER_ATTR = "api_timer_start";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (meterRegistry != null) {
            request.setAttribute(TIMER_ATTR, Timer.start(meterRegistry));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (meterRegistry == null) return;
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_ATTR);
        if (sample != null) {
            String uri = request.getRequestURI();
            String pattern = aggregatePath(uri);
            sample.stop(Timer.builder("docflow_api_response_seconds")
                    .tag("path", pattern)
                    .description("API response time")
                    .register(meterRegistry));
        }
    }

    private String aggregatePath(String uri) {
        String[] parts = uri.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return "root";
    }
}
