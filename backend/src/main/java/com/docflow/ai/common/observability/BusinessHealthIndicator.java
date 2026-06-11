package com.docflow.ai.common.observability;

import com.docflow.ai.common.pattern.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 自定义健康检查指标。
 * 技术点：暴露业务健康状态到 /actuator/health。
 * 包含：DLQ 深度、消息队列状态。
 */
@Component
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class BusinessHealthIndicator implements HealthIndicator {

    private final DeadLetterQueueService deadLetterQueueService;

    private static final long DLQ_WARNING_THRESHOLD = 50;
    private static final long DLQ_CRITICAL_THRESHOLD = 200;

    @Override
    public Health health() {
        long dlqDepth = deadLetterQueueService.getQueueDepth();

        if (dlqDepth >= DLQ_CRITICAL_THRESHOLD) {
            return Health.down()
                    .withDetail("dlq.depth", dlqDepth)
                    .withDetail("dlq.status", "CRITICAL: too many failed messages")
                    .build();
        }
        if (dlqDepth >= DLQ_WARNING_THRESHOLD) {
            return Health.status("WARN")
                    .withDetail("dlq.depth", dlqDepth)
                    .withDetail("dlq.status", "WARNING: DLQ depth above threshold")
                    .build();
        }
        return Health.up()
                .withDetail("dlq.depth", dlqDepth)
                .withDetail("dlq.status", "healthy")
                .build();
    }
}
