package com.docflow.ai.common.chain;

import com.docflow.ai.ticket.entity.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TicketProcessingChainTest {

    private TicketProcessingChain chain;

    @BeforeEach
    void setUp() {
        chain = new TicketProcessingChain(List.of(
                new ValidationHandler(),
                new RoutingHandler(),
                new NotificationHandler()
        ));
    }

    @Test
    void shouldPassValidTicket() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Test ticket");
        ticket.setType("incident");
        ProcessingContext ctx = ProcessingContext.builder()
                .ticket(ticket).action("CREATE").build();
        chain.execute(ctx);
        assertTrue(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Validation] PASSED")));
        assertTrue(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Routing] PASSED")));
        assertTrue(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Notification] PASSED")));
    }

    @Test
    void shouldFailWhenTitleIsEmpty() {
        Ticket ticket = new Ticket();
        ticket.setTitle("");
        ticket.setType("incident");
        ProcessingContext ctx = ProcessingContext.builder()
                .ticket(ticket).action("CREATE").build();
        chain.execute(ctx);
        assertTrue(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Validation] FAILED")));
        assertFalse(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Routing] PASSED")));
    }

    @Test
    void shouldFailWhenTicketIsNull() {
        ProcessingContext ctx = ProcessingContext.builder()
                .ticket(null).action("CREATE").build();
        chain.execute(ctx);
        assertTrue(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Validation] FAILED")));
    }

    @Test
    void routingShouldSetDefaultPriority() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Test");
        ticket.setType("incident");
        ticket.setPriority(null);
        ProcessingContext ctx = ProcessingContext.builder()
                .ticket(ticket).action("CREATE").build();
        chain.execute(ctx);
        assertEquals(2, ticket.getPriority());
    }

    @Test
    void routingShouldEscalateUrgentTickets() {
        Ticket ticket = new Ticket();
        ticket.setTitle("紧急：生产环境宕机");
        ticket.setType("incident");
        ProcessingContext ctx = ProcessingContext.builder()
                .ticket(ticket).action("CREATE").build();
        chain.execute(ctx);
        assertEquals(3, ticket.getPriority());
        assertTrue(ctx.getProcessingLog().stream().anyMatch(l -> l.contains("Escalated to URGENT")));
    }
}
