package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 咨询工单处理模板 - 模板方法模式的轻量级实现
 *
 * 【设计模式】模板方法模式 (Template Method Pattern) 的子类
 * 【与 IncidentProcessingTemplate 对比】
 *   - 故障工单：高优先级、必须通知、升级管理层
 *   - 咨询工单：低优先级、不立即通知、自动回复
 *
 * 【面试考点】
 *   - 模板方法的"对称性"：不同子类覆盖不同钩子方法
 *   - IncidentProcessingTemplate 覆盖了 validate/preprocess/shouldNotify/notifyStakeholders
 *   - QuestionProcessingTemplate 只覆盖了 preprocess/execute/shouldNotify
 *   - 这就是模板方法的灵活性：算法骨架相同，细节各不相同
 *
 * 【咨询工单特点】
 *   - 优先级较低（不需要紧急响应）
 *   - 可以自动回复（查询知识库）
 *   - SLA 宽松（可能 24 小时内响应即可）
 */
@Slf4j
@Component
public class QuestionProcessingTemplate extends TicketProcessingTemplate {

    @Override
    protected void preprocess(Ticket ticket) {
        // 咨询工单默认低优先级（不需要紧急响应）
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
        return false; // 咨询工单不需要立即通知（可以异步处理）
    }
}