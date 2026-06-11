package com.docflow.ai.ticket.statemachine;

import org.springframework.stereotype.Component;

@Component
public class ResolvedStateHandler implements TicketStateHandler {
    @Override
    public int getStatus() { return 3; }
    @Override
    public boolean canTransitionTo(int target) { return target == 2 || target == 4; }
    @Override
    public void onBeforeTransition(TicketStateContext ctx) {
        // Validate: reopening requires reason
    }
    @Override
    public void onAfterTransition(TicketStateContext ctx) {
        // Side effects: send satisfaction survey on close, trigger knowledge draft
    }
}
