package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;

/**
 * 工单路由策略接口 - 策略模式 (Strategy Pattern) 的策略接口
 *
 * 【面试考点】
 *   - 策略模式 vs 状态模式 vs 工厂模式
 *   - 策略模式：客户端选择算法（路由规则由配置决定）
 *   - 状态模式：对象行为由状态决定（工单行为由当前状态决定）
 *   - 工厂模式：创建对象（不关心行为差异）
 *
 * 【如何避免 if-else 地狱】
 *   传统写法：if (type == "INCIDENT") { ... } else if (type == "TASK") { ... }
 *   策略模式：List<Strategy> 自动遍历，找到匹配的执行
 *   新增策略只需加一个 @Component 类，无需修改现有代码
 *
 * 【Spring 中的策略模式实现方式】
 *   1. 接口定义策略契约（本文件）
 *   2. 多个 @Component 实现类（Spring 自动收集）
 *   3. Context 类注入 List<Strategy>（TicketRouter）
 */
public interface TicketRoutingStrategy {

    /** 判断当前策略是否适用于该工单 */
    boolean supports(Ticket ticket);

    /** 执行路由策略，返回指派的处理人 ID */
    Long resolveAssignee(Ticket ticket);

    /** 策略名称（用于日志和调试） */
    String getName();
}