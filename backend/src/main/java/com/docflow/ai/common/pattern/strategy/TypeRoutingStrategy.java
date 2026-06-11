package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

/**
 * 按工单类型路由 —— 不同类型的工单指派给对应团队
 * <p>
 * 路由规则：
 * <ul>
 *   <li>INCIDENT（故障）→ 运维团队</li>
 *   <li>TASK（任务）→ 开发团队</li>
 *   <li>QUESTION（咨询）→ 客服团队</li>
 * </ul>
 */
@Component
public class TypeRoutingStrategy implements TicketRoutingStrategy {

    @Override
    public boolean supports(Ticket ticket) {
        return ticket.getType() != null && !ticket.getType().isEmpty();
    }

    @Override
    public Long resolveAssignee(Ticket ticket) {
        return switch (ticket.getType()) {
            case "INCIDENT" -> 2L;  // 运维工程师
            case "TASK" -> 3L;      // 开发工程师
            case "QUESTION" -> 4L;  // 客服
            default -> 2L;          // 默认运维
        };
    }

    @Override
    public String getName() {
        return "type-routing";
    }
}
