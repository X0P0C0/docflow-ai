package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("工单路由器测试")
class TicketRouterTest {

    @Test
    @DisplayName("路由器应初始化所有策略")
    void shouldInitializeStrategies() {
        TicketRouter r = new TicketRouter(List.of(
            new PriorityRoutingStrategy(),
            new TypeRoutingStrategy(),
            new FallbackRoutingStrategy()
        ));
        assertThat(r).isNotNull();
    }

    @Test
    @DisplayName("高优先级工单应使用优先级策略")
    void shouldRouteHighPriorityTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketNo("INC-20260529-00001");
        ticket.setPriority(1);

        TicketRouter r = new TicketRouter(List.of(
            new PriorityRoutingStrategy(),
            new TypeRoutingStrategy(),
            new FallbackRoutingStrategy()
        ));

        Long assignee = r.route(ticket);
        assertThat(assignee).isEqualTo(1L);
    }

    @Test
    @DisplayName("按类型路由普通工单")
    void shouldRouteByType() {
        Ticket ticket = new Ticket();
        ticket.setTicketNo("TICKET-001");
        ticket.setPriority(3);
        ticket.setType("QUESTION");

        TicketRouter r = new TicketRouter(List.of(
            new PriorityRoutingStrategy(),
            new TypeRoutingStrategy(),
            new FallbackRoutingStrategy()
        ));

        Long assignee = r.route(ticket);
        assertThat(assignee).isEqualTo(4L);
    }
}
