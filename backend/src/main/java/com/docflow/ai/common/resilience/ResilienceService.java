package com.docflow.ai.common.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilienceService {

    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    public <T> T execute(String instanceName, Supplier<T> action) {
        CircuitBreaker cb = cbRegistry.circuitBreaker(instanceName);
        Retry retry = retryRegistry.retry(instanceName);
        Supplier<T> decorated = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(cb, action));
        try {
            return decorated.get();
        } catch (CallNotPermittedException ex) {
            log.warn("CircuitBreaker [{}] OPEN - call rejected", instanceName);
            throw new CircuitBreakerOpenException(instanceName);
        } catch (Exception ex) {
            throw new ResilienceExecutionException(instanceName, ex);
        }
    }

    public <T> T executeWithFallback(String instanceName, Supplier<T> action, Supplier<T> fallback) {
        try {
            return execute(instanceName, action);
        } catch (CircuitBreakerOpenException | ResilienceExecutionException ex) {
            log.warn("CircuitBreaker [{}] fallback triggered: {}", instanceName, ex.getMessage());
            return fallback.get();
        }
    }

    public CircuitBreaker.State getCircuitState(String instanceName) {
        return cbRegistry.circuitBreaker(instanceName).getState();
    }

    public static class CircuitBreakerOpenException extends RuntimeException {
        private final String instanceName;
        public CircuitBreakerOpenException(String instanceName) {
            super("Circuit breaker [" + instanceName + "] is OPEN");
            this.instanceName = instanceName;
        }
        public String getInstanceName() { return instanceName; }
    }

    public static class ResilienceExecutionException extends RuntimeException {
        private final String instanceName;
        public ResilienceExecutionException(String instanceName, Throwable cause) {
            super("Resilience execution failed for [" + instanceName + "]", cause);
            this.instanceName = instanceName;
        }
        public String getInstanceName() { return instanceName; }
    }
}
