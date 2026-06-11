package com.docflow.ai.ticket.statemachine;

import org.springframework.stereotype.Component;

@Component
public class InProgressStateHandler implements TicketStateHandler {
    @Override
    public int getStatus() { return 2; }
    @Override
    public boolean canTransitionTo(int target) { return target == 3 || target == 4; }
    @Override
    public void onBeforeTransition(TicketStateContext ctx) {
        // Validate: must have assignee to resolve
    }
    @Override
    public void onAfterTransition(TicketStateContext ctx) {
        // Side effects: calculate resolution time, check SLA
    }
}
