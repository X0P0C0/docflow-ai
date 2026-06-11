package com.docflow.ai.common.observer;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工单事件对象。
 */
@Data
@Builder
public class TicketEvent {
    private String eventType;      // CREATED, STATUS_CHANGED, ASSIGNED, COMMENTED, MERGED
    private Long ticketId;
    private Long operatorId;
    private Object oldValue;
    private Object newValue;
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;
}
