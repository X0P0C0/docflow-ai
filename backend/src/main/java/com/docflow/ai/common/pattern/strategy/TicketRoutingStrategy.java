package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;

/**
 * 工单路由策略接口 —— 策略模式（Strategy Pattern）
 * <p>
 * 策略模式的核心思想：
 * <ul>
 *   <li>定义一组算法，把它们一个个封装起来</li>
 *   <li>使它们可以互相替换，让算法独立于使用它的客户端</li>
 *   <li>遵循开闭原则：新增策略不需要修改已有代码</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>策略模式 vs 状态模式 vs 工厂模式</li>
 *   <li>如何避免 if-else / switch 地狱</li>
 *   <li>Spring 中策略模式的实现方式</li>
 * </ul>
 */
public interface TicketRoutingStrategy {

    /**
     * 判断当前策略是否适用于该工单
     */
    boolean supports(Ticket ticket);

    /**
     * 执行路由策略，返回指派的处理人 ID
     */
    Long resolveAssignee(Ticket ticket);

    /**
     * 策略名称
     */
    String getName();
}
