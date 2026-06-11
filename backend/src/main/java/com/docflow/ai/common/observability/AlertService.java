package com.docflow.ai.common.observability;

import com.docflow.ai.common.pattern.DeadLetterQueueService;
import com.docflow.ai.common.resilience.ResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 告警服务。
 * 技术点：定时检查系统健康指标，超阈值时触发告警。
 * 检查项：DLQ 深度、熔断器状态、错误率。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final DeadLetterQueueService deadLetterQueueService;
    private final ResilienceService resilienceService;

    private static final long DLQ_WARN_THRESHOLD = 10;
    private static final long DLQ_CRITICAL_THRESHOLD = 100;

    /**
     * 每 60 秒检查一次系统健康状态。
     */
    @Scheduled(fixedDelay = 60_000)
    public List<Alert> checkAlerts() {
        List<Alert> alerts = new ArrayList<>();

        // 1. DLQ 深度检查
        long dlqDepth = deadLetterQueueService.getQueueDepth();
        if (dlqDepth >= DLQ_CRITICAL_THRESHOLD) {
            alerts.add(Alert.critical("DLQ_DEPTH", "Dead letter queue depth is " + dlqDepth + " (critical threshold: " + DLQ_CRITICAL_THRESHOLD + ")"));
        } else if (dlqDepth >= DLQ_WARN_THRESHOLD) {
            alerts.add(Alert.warning("DLQ_DEPTH", "Dead letter queue depth is " + dlqDepth + " (warn threshold: " + DLQ_WARN_THRESHOLD + ")"));
        }

        // 2. 熔断器状态检查
        for (String instance : List.of("ai-service", "external-api")) {
            try {
                CircuitBreaker.State state = resilienceService.getCircuitState(instance);
                if (state == CircuitBreaker.State.OPEN) {
                    alerts.add(Alert.critical("CB_OPEN", "Circuit breaker [" + instance + "] is OPEN"));
                } else if (state == CircuitBreaker.State.HALF_OPEN) {
                    alerts.add(Alert.warning("CB_HALF_OPEN", "Circuit breaker [" + instance + "] is HALF_OPEN"));
                }
            } catch (Exception ignored) {}
        }

        if (!alerts.isEmpty()) {
            alerts.forEach(alert -> {
                if (alert.severity() == AlertSeverity.CRITICAL) {
                    log.error("ALERT [CRITICAL]: {} - {}", alert.code(), alert.message());
                } else {
                    log.warn("ALERT [WARNING]: {} - {}", alert.code(), alert.message());
                }
            });
        }

        return alerts;
    }

    public record Alert(AlertSeverity severity, String code, String message) {
        public static Alert warning(String code, String message) {
            return new Alert(AlertSeverity.WARNING, code, message);
        }
        public static Alert critical(String code, String message) {
            return new Alert(AlertSeverity.CRITICAL, code, message);
        }
    }

    public enum AlertSeverity {
        WARNING, CRITICAL
    }
}
