package com.docflow.ai.common.observer;

import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计观察者 - 观察者模式的具体实现
 *
 * 【设计模式】观察者模式 (Observer Pattern) 的具体观察者
 * 【面试考点】
 *   - 观察者的职责单一：每个观察者只关注一件事（审计/SLA/通知等）
 *   - 观察者异常隔离：审计失败不应阻止工单创建（优雅降级）
 *   - 同步 vs 异步：审计需要同步写入（保证数据不丢），通知可以异步
 *
 * 【业务逻辑】
 *   工单任何变更（创建/状态变更/评论/指派）自动触发审计记录
 *   记录内容：谁、什么时间、做了什么操作、从什么状态变到什么状态
 *
 * 【真实业务场景】
 *   合规要求：金融/医疗行业必须记录所有操作日志
 *   问题排查：工单出问题时，通过审计日志追溯操作历史
 *   责任认定：明确每个操作的操作人和操作时间
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditObserver implements TicketObserver {

    private final TicketRecordMapper ticketRecordMapper;

    @Override
    public String name() {
        return "AuditObserver";
    }

    /**
     * 收到工单事件时自动执行
     *
     * 【事件驱动】这是一个典型的事件驱动架构：
     *   工单服务（事件发布者）-> 事件 -> 审计观察者（事件消费者）
     *   发布者不关心谁在监听，消费者不关心谁发布的（解耦）
     */
    @Override
    public void onEvent(TicketEvent event) {
        TicketRecord record = new TicketRecord();
        record.setTicketId(event.getTicketId());
        record.setActionType(event.getEventType());
        record.setOperatorUserId(event.getOperatorId());

        // 记录状态变更（从旧状态到新状态）
        if (event.getOldValue() instanceof Integer oldVal) {
            record.setOldStatus(oldVal);
        }
        if (event.getNewValue() instanceof Integer newVal) {
            record.setNewStatus(newVal);
        }

        record.setRemark("Observer audit: " + event.getEventType());
        ticketRecordMapper.insert(record);
        log.debug("Audit record created: ticketId={}, action={}", event.getTicketId(), event.getEventType());
    }
}