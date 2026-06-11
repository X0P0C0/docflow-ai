package com.docflow.ai.common.pattern;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DomainEvent {
    private String eventId;
    private String eventType;
    private Long entityId;
    private String entityType;
    private Object payload;
    private LocalDateTime timestamp;
    private int retryCount;

    public static DomainEvent of(String type, Long entityId, String entityType, Object payload) {
        DomainEvent e = new DomainEvent();
        e.setEventId(UUID.randomUUID().toString());
        e.setEventType(type);
        e.setEntityId(entityId);
        e.setEntityType(entityType);
        e.setPayload(payload);
        e.setTimestamp(LocalDateTime.now());
        e.setRetryCount(0);
        return e;
    }
}
