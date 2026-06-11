package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 咨询工单处理模板 —— 普通咨询的轻量处理
 * <p>
 * 咨询工单特点：
 * <ul>
 *   <li>优先级较低</li>
 *   <li>可自动回复</li>
 *   <li>SLA 宽松</li>
 * </ul>
 */
@Slf4j
@Component
public class QuestionProcessingTemplate extends TicketProcessingTemplate {

    @Override
    protected void preprocess(Ticket ticket) {
        // 咨询工单默认低优先级
        if (ticket.getPriority() == null) {
            ticket.setPriority(3);
        }
    }

    @Override
    protected TicketProcessingResult execute(Ticket ticket) {
        // 咨询工单指派给客服
        Long assigneeId = 4L;
        log.info("Question {} assigned to support {}", ticket.getTicketNo(), assigneeId);
        return TicketProcessingResult.success(assigneeId);
    }

    @Override
    protected boolean shouldNotify(Ticket ticket, TicketProcessingResult result) {
        return false; // 咨询工单不需要立即通知
    }
}
