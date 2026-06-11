package com.docflow.ai.ticket.statemachine;

import org.springframework.stereotype.Component;

@Component
public class NewStateHandler implements TicketStateHandler {
    @Override
    public int getStatus() { return 1; }
    @Override
    public boolean canTransitionTo(int target) { return target == 2 || target == 4; }
    @Override
    public void onBeforeTransition(TicketStateContext ctx) {
        // Validate: only operator or assignee can start processing
    }
    @Override
    public void onAfterTransition(TicketStateContext ctx) {
        // Side effects: notify assignee, start SLA timer
    }
}
