package com.docflow.ai.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsResponse {

    private long total;
    private long newCount;
    private long inProgress;
    private long resolved;
    private long unassigned;
}