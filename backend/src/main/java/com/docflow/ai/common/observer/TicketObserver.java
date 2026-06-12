package com.docflow.ai.common.observer;

/**
 * 观察者接口 - 工单生命周期事件观察者
 *
 * 【设计模式】观察者模式 (Observer Pattern) 的观察者接口
 * 【面试考点】
 *   - 经典观察者模式：解耦工单操作与副作用（审计、通知、SLA）
 *   - 开闭原则：新增观察者只需实现此接口 + @Component，无需修改工单服务
 *   - Spring 事件机制对比：ApplicationEvent 更重量级，此方式更轻量
 *
 * 【当前实现类】
 *   - AuditObserver: 审计日志（记录谁、什么时间、做了什么）
 *   - SlaObserver: SLA 检查（工单创建时检查 SLA 策略）
 *
 * 【扩展方式】
 *   任何需要监听工单变更的服务只需：
 *   1. 实现此接口
 *   2. 加 @Component 注解
 *   3. Spring 自动注入到 TicketObserverRegistry
 */
public interface TicketObserver {

    /** 观察者名称，用于日志和调试 */
    String name();

    /** 当工单事件发生时被调用 */
    void onEvent(TicketEvent event);
}