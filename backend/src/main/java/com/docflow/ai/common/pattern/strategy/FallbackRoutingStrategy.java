package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 兜底路由策略 - 策略模式的默认实现
 *
 * 【设计模式】策略模式 (Strategy Pattern) 的兜底策略
 * 【面试考点】
 *   - @Order(999) 数字越大优先级越低，保证最后执行
 *   - supports() 总是返回 true，作为"保底"策略
 *   - 类似于 switch 的 default 分支
 *
 * 【真实业务场景】
 *   当所有路由规则都不匹配时（例如新类型的工单），默认分配给运维团队
 *   避免工单无人处理（防止"工单黑洞"）
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