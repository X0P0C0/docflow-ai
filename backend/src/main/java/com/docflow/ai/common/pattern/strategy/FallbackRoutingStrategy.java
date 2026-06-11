package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认兜底策略 —— 当其他策略都不匹配时使用
 * <p>
 * 使用 @Order 注解控制优先级，数字越大优先级越低
 */
@Component
@Order(999)
public class FallbackRoutingStrategy implements TicketRoutingStrategy {

    @Override
    public boolean supports(Ticket ticket) {
        return true; // 兜底策略，总是匹配
    }

    @Override
    public Long resolveAssignee(Ticket ticket) {
        return 2L; // 默认指派给运维
    }

    @Override
    public String getName() {
        return "fallback-routing";
    }
}
