package com.docflow.ai.monitoring;

import com.docflow.ai.common.domain.ApiResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system/health")
@RequiredArgsConstructor
@Tag(name = "System Health")
public class SystemHealthController {

    private final CircuitBreakerRegistry cbRegistry;
    private final StringRedisTemplate redisTemplate;

    @GetMapping
    @Operation(summary = "Aggregated health check")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", OffsetDateTime.now());

        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            result.put("redis", Map.of("status", "UP"));
        } catch (Exception e) {
            result.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
            result.put("status", "DEGRADED");
        }

        Map<String, Object> breakers = new LinkedHashMap<>();
        for (CircuitBreaker cb : cbRegistry.getAllCircuitBreakers()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("state", cb.getState().name());
            info.put("failureRate", cb.getMetrics().getFailureRate());
            info.put("bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls());
            breakers.put(cb.getName(), info);
            if (cb.getState() == CircuitBreaker.State.OPEN) result.put("status", "DEGRADED");
        }
        result.put("circuitBreakers", breakers);

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean rt = ManagementFactory.getRuntimeMXBean();
        result.put("jvm", Map.of(
                "heapUsedMB", mem.getHeapMemoryUsage().getUsed() / (1024 * 1024),
                "heapMaxMB", mem.getHeapMemoryUsage().getMax() / (1024 * 1024),
                "uptimeMinutes", rt.getUptime() / 60000));

        return ApiResponse.success(result);
    }
}
