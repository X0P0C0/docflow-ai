package com.docflow.ai.common.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 告警监控接口。
 * GET /api/admin/alerts — 获取当前活跃告警。
 */
@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getActiveAlerts() {
        List<AlertService.Alert> alerts = alertService.checkAlerts();
        return ResponseEntity.ok(Map.of(
                "count", alerts.size(),
                "alerts", alerts
        ));
    }
}
