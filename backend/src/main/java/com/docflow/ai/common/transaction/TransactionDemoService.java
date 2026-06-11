package com.docflow.ai.common.transaction;

import com.docflow.ai.notification.service.NotificationService;
import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 事务传播机制演示。
 * 
 * 不同传播级别的实际应用场景：
 * - REQUIRED（默认）：加入当前事务，适合主业务逻辑
 * - REQUIRES_NEW：独立事务，适合审计日志、通知等"即使主事务回滚也要保留"的操作
 * - NESTED：嵌套事务（savepoint），适合可独立回滚的子操作
 * - MANDATORY：必须在已有事务中调用，适合"不允许独立执行"的校验逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDemoService {

    private final TicketRecordMapper recordMapper;
    private final NotificationService notificationService;

    /**
     * REQUIRES_NEW: 独立事务写审计日志。
     * 即使主事务（如创建工单）回滚，这条审计记录也会保留。
     * 典型场景：安全审计、合规日志。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeAuditLog(Long ticketId, Long operatorId, String action, String detail) {
        TicketRecord record = new TicketRecord();
        record.setTicketId(ticketId);
        record.setOperatorUserId(operatorId);
        record.setActionType(action);
        record.setRemark(detail);
        recordMapper.insert(record);
        log.info("Audit log written (REQUIRES_NEW): ticket={}, action={}", ticketId, action);
    }

    /**
     * REQUIRES_NEW: 独立事务发通知。
     * 通知发送失败不应导致工单操作回滚。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void sendNotification(Long receiverId, String type, String title, String content, Long refId) {
        try {
            notificationService.create(receiverId, null, type, title, content, "TICKET", refId);
            log.info("Notification sent (REQUIRES_NEW): receiver={}, title={}", receiverId, title);
        } catch (Exception e) {
            // 通知失败不影响主业务
            log.warn("Notification failed (isolated): {}", e.getMessage());
        }
    }

    /**
     * MANDATORY: 必须在已有事务中调用。
     * 如果没有活跃事务，Spring 会抛出异常。
     * 典型场景：校验逻辑，确保不会被独立调用。
     */
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void validateInTransaction(Long ticketId) {
        // This method MUST be called within an existing transaction
        log.info("Validation passed (MANDATORY): ticketId={}", ticketId);
    }
}
