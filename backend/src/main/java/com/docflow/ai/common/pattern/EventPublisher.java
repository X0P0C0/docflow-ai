package com.docflow.ai.common.pattern;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {
    private final ApplicationEventPublisher publisher;
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    public void publish(DomainEvent event) {
        log.info("Event: type={}, entity={}({})", event.getEntityType(), event.getEntityId(), event.getEventType());
        publisher.publishEvent(event);
    }
}
