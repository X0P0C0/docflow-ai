package com.docflow.ai.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                CircuitBreaker cb = event.getAddedEntry();
                cb.getEventPublisher()
                        .onStateTransition(e -> log.warn("CircuitBreaker [{}] state: {} -> {}",
                                e.getCircuitBreakerName(),
                                e.getStateTransition().getFromState(),
                                e.getStateTransition().getToState()))
                        .onError(e -> log.warn("CircuitBreaker [{}] error: {}ms",
                                e.getCircuitBreakerName(), e.getElapsedDuration().toMillis()))
                        .onCallNotPermitted(e -> log.warn("CircuitBreaker [{}] call rejected (OPEN)",
                                e.getCircuitBreakerName()));
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {}
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {}
        };
    }
}
