package com.docflow.ai.ticket.strategy;

import com.docflow.ai.ticket.dto.CreateTicketRequest;
import org.springframework.stereotype.Component;

@Component
public class IncidentStrategy implements TicketTypeStrategy {
    @Override
    public String getType() { return "INCIDENT"; }
    @Override
    public void beforeCreate(CreateTicketRequest request) {
        if (request.getPriority() == null) request.setPriority(3); // Default: high
    }
    @Override
    public void afterCreate(Long ticketId, CreateTicketRequest request) {
        // INCIDENT: trigger SLA timer, notify on-call team
    }
    @Override
    public int getDefaultPriority() { return 3; }
    @Override
    public boolean requiresAutoAssign() { return true; }
}
