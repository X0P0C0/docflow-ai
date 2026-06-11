package com.docflow.ai.monitoring;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry registry;

    public void ticketCreated(String priority, String type) {
        Counter.builder("docflow_tickets_created_total")
                .tag("priority", priority).tag("type", type)
                .register(registry).increment();
    }

    public void ticketResolved() {
        Counter.builder("docflow_tickets_resolved_total")
                .register(registry).increment();
    }

    public void slaBreached() {
        Counter.builder("docflow_sla_breach_total")
                .register(registry).increment();
    }

    public void aiAnalysisCompleted(String type, long durationMs) {
        Counter.builder("docflow_ai_analysis_total")
                .tag("type", type).tag("result", "success")
                .register(registry).increment();
        Timer.builder("docflow_ai_analysis_duration_seconds")
                .tag("type", type)
                .register(registry).record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void aiAnalysisFailed(String type) {
        Counter.builder("docflow_ai_analysis_total")
                .tag("type", type).tag("result", "failure")
                .register(registry).increment();
    }

    public void loginAttempt(String result) {
        Counter.builder("docflow_login_total")
                .tag("result", result)
                .register(registry).increment();
    }
}
