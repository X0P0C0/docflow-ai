package com.docflow.ai.ticket.statemachine;

/**
 * 工单状态处理器接口（状态模式）。
 * 每个状态实现自己的转换逻辑和业务规则。
 * 
 * 状态流转图：
 *   NEW(1) --> IN_PROGRESS(2)  [开始处理]
 *   NEW(1) --> CLOSED(4)       [直接关闭]
 *   IN_PROGRESS(2) --> RESOLVED(3) [已解决]
 *   IN_PROGRESS(2) --> CLOSED(4)  [直接关闭]
 *   RESOLVED(3) --> CLOSED(4)     [确认关闭]
 *   RESOLVED(3) --> IN_PROGRESS(2) [重新打开]
 *   CLOSED(4) --> IN_PROGRESS(2)  [重新打开]
 */
public interface TicketStateHandler {
    
    /** 当前状态码 */
    int getStatus();
    
    /** 允许转换到哪些状态 */
    boolean canTransitionTo(int targetStatus);
    
    /** 转换前的业务校验 */
    void onBeforeTransition(TicketStateContext context);
    
    /** 转换后的副作用（如发通知、更新 SLA） */
    void onAfterTransition(TicketStateContext context);
}
