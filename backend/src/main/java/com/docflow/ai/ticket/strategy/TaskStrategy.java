package com.docflow.ai.ticket.strategy;

import com.docflow.ai.ticket.dto.CreateTicketRequest;
import org.springframework.stereotype.Component;

@Component
public class TaskStrategy implements TicketTypeStrategy {
    @Override
    public String getType() { return "TASK"; }
    @Override
    public void beforeCreate(CreateTicketRequest request) {
        if (request.getPriority() == null) request.setPriority(2); // Default: medium
    }
    @Override
    public void afterCreate(Long ticketId, CreateTicketRequest request) {
        // TASK: set expected finish time, notify assignee
    }
    @Override
    public int getDefaultPriority() { return 2; }
    @Override
    public boolean requiresAutoAssign() { return false; }
}
