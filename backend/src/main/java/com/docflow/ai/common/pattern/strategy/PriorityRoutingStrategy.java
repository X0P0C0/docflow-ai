package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

/**
 * 优先级路由策略 - 策略模式的具体实现
 *
 * 【设计模式】策略模式 (Strategy Pattern) 的具体策略
 * 【面试考点】
 *   - 策略模式 vs if-else：策略模式符合开闭原则，新增策略不改现有代码
 *   - Spring 自动发现：@Component 使该策略自动注册到 TicketRouter
 *   - 策略选择逻辑：supports() 方法决定何时使用该策略
 *
 * 【路由规则】
 *   P0（紧急）-> 技术主管（ID=1）
 *   P1（高）  -> 高级工程师（ID=2）
 *   P2/P3（中/低）-> 不匹配此策略，交给下一个策略处理
 *
 * 【真实业务场景】
 *   VIP 客户报告系统宕机（P0）-> 自动分配给技术主管
 *   普通客户报告功能异常（P1）-> 分配给高级工程师
 */
@Component
public class PriorityRoutingStrategy implements TicketRoutingStrategy {

    /**
     * 判断该策略是否适用于当前工单
     * 只有高优先级（P0/P1）才走此策略
     */
    @Override
    public boolean supports(Ticket ticket) {
        return ticket.getPriority() != null && ticket.getPriority() <= 2;
    }

    /**
     * 根据优先级决定处理人
     * P0 紧急 -> 技术主管，P1 高 -> 高级工程师
     */
    @Override
    public Long resolveAssignee(Ticket ticket) {
        return ticket.getPriority() == 1 ? 1L : 2L;
    }

    @Override
    public String getName() {
        return "priority-routing";
    }
}