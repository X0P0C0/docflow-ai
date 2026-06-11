package com.docflow.ai.ticket.strategy;

import com.docflow.ai.ticket.dto.CreateTicketRequest;
import org.springframework.stereotype.Component;

@Component
public class QuestionStrategy implements TicketTypeStrategy {
    @Override
    public String getType() { return "QUESTION"; }
    @Override
    public void beforeCreate(CreateTicketRequest request) {
        if (request.getPriority() == null) request.setPriority(1); // Default: low
    }
    @Override
    public void afterCreate(Long ticketId, CreateTicketRequest request) {
        // QUESTION: auto-search knowledge base, suggest articles
    }
    @Override
    public int getDefaultPriority() { return 1; }
    @Override
    public boolean requiresAutoAssign() { return false; }
}
