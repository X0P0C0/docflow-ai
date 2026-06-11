package com.docflow.ai.common.observer;

/**
 * 观察者接口 —— 工单生命周期事件观察者。
 * 技术点：经典观察者模式，解耦工单操作与副作用（审计、通知、SLA）。
 * 任何需要监听工单变更的服务只需实现此接口并注册到 TicketObserverRegistry。
 */
public interface TicketObserver {

    /**
     * 观察者名称，用于日志和调试。
     */
    String name();

    /**
     * 当工单事件发生时被调用。
     * @param event 工单事件
     */
    void onEvent(TicketEvent event);
}
