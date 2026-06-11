package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SlaStatusResponse {
    private Long ticketId;
    private String ticketNo;
    private String policyName;
    private LocalDateTime responseDeadline;
    private LocalDateTime resolveDeadline;
    private LocalDateTime firstResponseTime;
    private LocalDateTime resolvedTime;
    private boolean responseBreached;
    private boolean resolveBreached;
    private long responseRemainingMinutes;
    private long resolveRemainingMinutes;
}
