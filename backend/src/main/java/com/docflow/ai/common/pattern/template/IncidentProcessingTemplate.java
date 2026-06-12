package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 故障工单处理模板 - 模板方法模式的具体实现
 *
 * 【设计模式】模板方法模式 (Template Method Pattern) 的子类
 * 【面试考点】
 *   - 子类只覆盖需要定制的方法（钩子方法），不覆盖模板方法
 *   - 好莱坞原则：父类调用子类，子类不调用父类
 *   - 策略 vs 模板方法：策略用组合，模板方法用继承
 *
 * 【故障工单特点】
 *   - 需要立即响应（SLA 更严格）
 *   - 自动升级机制（优先级自动提升）
 *   - 额外通知管理层
 *
 * 【处理流程】
 *   1. validate: 故障工单必须有标题
 *   2. preprocess: 自动提升优先级（确保至少 P1）
 *   3. execute: 指派给运维主管
 *   4. shouldNotify: 总是通知（故障必须通知）
 *   5. notifyStakeholders: 通知管理层（升级机制）
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
        // 故障工单自动提升优先级（确保至少 P1，不会被低优先级处理）
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
        // 故障工单额外通知管理层（升级机制）
        log.info("Escalation notified for incident: {}", ticket.getTicketNo());
    }
}