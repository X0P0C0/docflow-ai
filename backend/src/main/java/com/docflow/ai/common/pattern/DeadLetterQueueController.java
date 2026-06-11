package com.docflow.ai.common.pattern;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 死信队列监控接口。
 * GET /api/admin/dlq/status — 查看 DLQ 深度。
 */
@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
public class DeadLetterQueueController {

    private final DeadLetterQueueService deadLetterQueueService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDlqStatus() {
        return ResponseEntity.ok(Map.of(
                "queueDepth", deadLetterQueueService.getQueueDepth(),
                "status", "healthy"
        ));
    }
}
