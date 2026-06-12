package com.docflow.ai.common.observer;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工单事件对象 - 观察者模式的事件载体
 *
 * 【设计模式】观察者模式 (Observer Pattern) 的事件对象
 * 【面试考点】
 *   - 事件对象是观察者和被观察者之间的"通信契约"
 *   - 包含：事件类型、操作人、旧值、新值、元数据
 *   - 使用 @Builder 模式构建（避免构造函数参数过多）
 *
 * 【事件类型】
 *   - CREATED: 工单创建
 *   - STATUS_CHANGED: 状态变更
 *   - ASSIGNED: 工单指派
 *   - COMMENTED: 添加评论
 *   - MERGED: 工单合并
 *
 * 【真实业务场景】
 *   工单状态从 OPEN 变为 IN_PROGRESS：
 *   eventType = "STATUS_CHANGED", oldValue = 1, newValue = 2
 */
@Data
@Builder
public class TicketEvent {
    private String eventType;      // 事件类型
    private Long ticketId;         // 工单 ID
    private Long operatorId;       // 操作人 ID
    private Object oldValue;       // 旧值（如旧状态）
    private Object newValue;       // 新值（如新状态）
    private Map<String, Object> metadata;  // 扩展元数据
    private LocalDateTime timestamp;       // 事件时间
}