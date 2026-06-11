package com.docflow.ai.ticket.strategy;

import com.docflow.ai.ticket.dto.CreateTicketRequest;

/**
 * 工单类型处理策略接口（策略模式）。
 * 不同类型的工单有不同的处理逻辑：
 * - INCIDENT：紧急事件，需要快速响应，自动升级
 * - TASK：任务型，需要明确截止日期和负责人
 * - QUESTION：咨询型，可以自动推荐知识库文章
 */
public interface TicketTypeStrategy {
    
    /** 工单类型标识 */
    String getType();
    
    /** 创建前的预处理（如自动填充默认值） */
    void beforeCreate(CreateTicketRequest request);
    
    /** 创建后的副作用（如自动分配、触发 SLA） */
    void afterCreate(Long ticketId, CreateTicketRequest request);
    
    /** 获取默认优先级 */
    int getDefaultPriority();
    
    /** 是否需要自动分配 */
    boolean requiresAutoAssign();
}
