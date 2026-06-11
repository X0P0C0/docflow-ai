package com.docflow.ai.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMetricsServiceTest {

    private MeterRegistry registry;
    private BusinessMetricsService metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new BusinessMetricsService(registry);
    }

    @Test
    @DisplayName("ticketCreated: increments counter with tags")
    void ticketCreatedIncrementsCounter() {
        metrics.ticketCreated("1", "INCIDENT");
        metrics.ticketCreated("1", "INCIDENT");
        metrics.ticketCreated("2", "QUESTION");

        Counter counter = registry.find("docflow_tickets_created_total")
                .tag("priority", "1")
                .tag("type", "INCIDENT")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);

        Counter counter2 = registry.find("docflow_tickets_created_total")
                .tag("priority", "2")
                .tag("type", "QUESTION")
                .counter();
        assertThat(counter2).isNotNull();
        assertThat(counter2.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("ticketResolved: increments resolved counter")
    void ticketResolvedIncrementsCounter() {
        metrics.ticketResolved();
        metrics.ticketResolved();

        Counter counter = registry.find("docflow_tickets_resolved_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("slaBreached: increments SLA breach counter")
    void slaBreachedIncrementsCounter() {
        metrics.slaBreached();

        Counter counter = registry.find("docflow_sla_breach_total").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("loginAttempt: tracks success and failure separately")
    void loginAttemptTracksSeparately() {
        metrics.loginAttempt("success");
        metrics.loginAttempt("success");
        metrics.loginAttempt("failure");

        Counter success = registry.find("docflow_login_total").tag("result", "success").counter();
        Counter failure = registry.find("docflow_login_total").tag("result", "failure").counter();

        assertThat(success).isNotNull();
        assertThat(success.count()).isEqualTo(2.0);
        assertThat(failure).isNotNull();
        assertThat(failure.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("aiAnalysisCompleted: records counter and timer")
    void aiAnalysisCompletedRecordsMetrics() {
        metrics.aiAnalysisCompleted("intent", 150);
        metrics.aiAnalysisCompleted("intent", 200);

        Counter counter = registry.find("docflow_ai_analysis_total")
                .tag("type", "intent").tag("result", "success").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);

        Timer timer = registry.find("docflow_ai_analysis_duration_seconds")
                .tag("type", "intent").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("aiAnalysisFailed: increments failure counter")
    void aiAnalysisFailedIncrementsCounter() {
        metrics.aiAnalysisFailed("sentiment");

        Counter counter = registry.find("docflow_ai_analysis_total")
                .tag("type", "sentiment").tag("result", "failure").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
