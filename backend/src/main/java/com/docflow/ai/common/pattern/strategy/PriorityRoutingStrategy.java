package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

/**
 * 按优先级路由 —— 高优先级工单指派给高级工程师
 * <p>
 * 路由规则：
 * <ul>
 *   <li>P0（紧急）→ 技术主管</li>
 *   <li>P1（高）→ 高级工程师</li>
 *   <li>P2/P3（中/低）→ 普通工程师</li>
 * </ul>
 */
@Component
public class PriorityRoutingStrategy implements TicketRoutingStrategy {

    @Override
    public boolean supports(Ticket ticket) {
        return ticket.getPriority() != null && ticket.getPriority() <= 2;
    }

    @Override
    public Long resolveAssignee(Ticket ticket) {
        // P0 紧急 → 技术主管（ID=1）
        // P1 高   → 高级工程师（ID=2）
        return ticket.getPriority() == 1 ? 1L : 2L;
    }

    @Override
    public String getName() {
        return "priority-routing";
    }
}
