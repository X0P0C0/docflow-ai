package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 故障工单处理模板 —— 高优先级工单的特殊处理流程
 * <p>
 * 故障工单特点：
 * <ul>
 *   <li>需要立即响应</li>
 *   <li>自动升级机制</li>
 *   <li>SLA 更严格</li>
 * </ul>
 */
@Slf4j
@Component
public class IncidentProcessingTemplate extends TicketProcessingTemplate {

    @Override
    protected boolean validate(Ticket ticket) {
        if (!super.validate(ticket)) return false;
        // 故障工单必须有标题
        return ticket.getTitle() != null && !ticket.getTitle().trim().isEmpty();
    }

    @Override
    protected void preprocess(Ticket ticket) {
        // 故障工单自动设置高优先级
        if (ticket.getPriority() == null || ticket.getPriority() > 2) {
            ticket.setPriority(2);
            log.info("Auto-elevated priority for incident: {}", ticket.getTicketNo());
        }
    }

    @Override
    protected TicketProcessingResult execute(Ticket ticket) {
        // 故障工单指派给运维主管
        Long assigneeId = 1L;
        log.info("Incident {} assigned to senior engineer {}", ticket.getTicketNo(), assigneeId);
        return TicketProcessingResult.success(assigneeId);
    }

    @Override
    protected boolean shouldNotify(Ticket ticket, TicketProcessingResult result) {
        return true; // 故障工单总是通知
    }

    @Override
    protected void notifyStakeholders(Ticket ticket, TicketProcessingResult result) {
        super.notifyStakeholders(ticket, result);
        // 故障工单额外通知管理层
        log.info("Escalation notified for incident: {}", ticket.getTicketNo());
    }
}
