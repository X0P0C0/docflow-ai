package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("工单处理模板测试")
class TicketProcessingTemplateTest {

    @Test
    @DisplayName("故障工单应自动提升优先级")
    void incidentShouldElevatePriority() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNo("INC-001");
        ticket.setTitle("服务器宕机");
        ticket.setPriority(3);

        IncidentProcessingTemplate template = new IncidentProcessingTemplate();
        TicketProcessingResult result = template.process(ticket);

        assertThat(result.success()).isTrue();
        assertThat(ticket.getPriority()).isEqualTo(2); // Auto-elevated
    }

    @Test
    @DisplayName("咨询工单应使用默认低优先级")
    void questionShouldUseDefaultPriority() {
        Ticket ticket = new Ticket();
        ticket.setId(2L);
        ticket.setTicketNo("QST-001");
        ticket.setTitle("如何使用系统");

        QuestionProcessingTemplate template = new QuestionProcessingTemplate();
        TicketProcessingResult result = template.process(ticket);

        assertThat(result.success()).isTrue();
        assertThat(ticket.getPriority()).isEqualTo(3);
    }

    @Test
    @DisplayName("无效工单应验证失败")
    void shouldRejectInvalidTicket() {
        IncidentProcessingTemplate template = new IncidentProcessingTemplate();
        TicketProcessingResult result = template.process(null);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("验证失败");
    }

    @Test
    @DisplayName("无标题的故障工单应验证失败")
    void shouldRejectIncidentWithoutTitle() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTicketNo("INC-002");
        ticket.setTitle("");

        IncidentProcessingTemplate template = new IncidentProcessingTemplate();
        TicketProcessingResult result = template.process(ticket);

        assertThat(result.success()).isFalse();
    }
}
