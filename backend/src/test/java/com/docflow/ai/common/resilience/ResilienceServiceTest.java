package com.docflow.ai.common.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ResilienceServiceTest {

    private CircuitBreakerRegistry cbRegistry;
    private RetryRegistry retryRegistry;
    private ResilienceService resilienceService;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .build();
        cbRegistry = CircuitBreakerRegistry.of(cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofMillis(10))
                .build();
        retryRegistry = RetryRegistry.of(retryConfig);

        resilienceService = new ResilienceService(cbRegistry, retryRegistry);
    }

    @Test
    @DisplayName("execute: successful call returns result")
    void executeSuccess() {
        String result = resilienceService.execute("test", () -> "hello");
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("execute: retries on failure then throws")
    void executeRetriesAndFails() {
        assertThatThrownBy(() -> resilienceService.execute("test", () -> {
            throw new RuntimeException("fail");
        })).isInstanceOf(ResilienceService.ResilienceExecutionException.class);
    }

    @Test
    @DisplayName("executeWithFallback: returns fallback on failure")
    void executeWithFallbackReturnsFallback() {
        String result = resilienceService.executeWithFallback("test",
                () -> { throw new RuntimeException("fail"); },
                () -> "fallback");
        assertThat(result).isEqualTo("fallback");
    }

    @Test
    @DisplayName("executeWithFallback: returns result on success")
    void executeWithFallbackReturnsResult() {
        String result = resilienceService.executeWithFallback("test",
                () -> "success",
                () -> "fallback");
        assertThat(result).isEqualTo("success");
    }

    @Test
    @DisplayName("circuit breaker opens after threshold failures")
    void circuitBreakerOpensAfterFailures() {
        // Trigger enough failures to open the circuit
        for (int i = 0; i < 5; i++) {
            try {
                resilienceService.executeWithFallback("test",
                        () -> { throw new RuntimeException("fail"); },
                        () -> "fallback");
            } catch (Exception ignored) {}
        }

        CircuitBreaker cb = cbRegistry.circuitBreaker("test");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("getCircuitState returns current state")
    void getCircuitStateReturnsState() {
        CircuitBreaker.State state = resilienceService.getCircuitState("test");
        assertThat(state).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
