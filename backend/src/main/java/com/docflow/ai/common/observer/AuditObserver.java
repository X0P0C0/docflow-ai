package com.docflow.ai.common.observer;

import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 审计观察者 —— 自动记录工单操作日志。
 * 实现观察者模式：工单任何变更自动触发审计记录。
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

    @Override
    public void onEvent(TicketEvent event) {
        TicketRecord record = new TicketRecord();
        record.setTicketId(event.getTicketId());
        record.setActionType(event.getEventType());
        record.setOperatorUserId(event.getOperatorId());
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
