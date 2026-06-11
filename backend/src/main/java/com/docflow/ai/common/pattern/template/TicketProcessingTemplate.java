package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;

/**
 * 工单处理模板 —— 模板方法模式（Template Method Pattern）
 * <p>
 * 模板方法模式的核心思想：
 * <ul>
 *   <li>在抽象类中定义算法骨架（模板方法）</li>
 *   <li>将某些步骤延迟到子类实现</li>
 *   <li>算法结构不变，但具体步骤可定制</li>
 * </ul>
 * <p>
 * 工单处理流程：
 * <ol>
 *   <li>验证工单 → validate()</li>
 *   <li>预处理 → preprocess()</li>
 *   <li>执行核心逻辑 → execute()（抽象方法，子类必须实现）</li>
 *   <li>后处理 → postprocess()</li>
 *   <li>通知相关人员 → notify()</li>
 * </ol>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>模板方法 vs 策略模式的区别</li>
 *   <li>钩子方法（Hook Method）的作用</li>
 *   <li>好莱坞原则：Don't call us, we'll call you</li>
 * </ul>
 */
@Slf4j
public abstract class TicketProcessingTemplate {

    /**
     * 模板方法 —— 定义处理流程（final 防止子类覆盖）
     */
    public final TicketProcessingResult process(Ticket ticket) {
        log.info("Starting processing for ticket: {}", ticket != null ? ticket.getTicketNo() : "null");

        // Step 1: 验证
        if (!validate(ticket)) {
            return TicketProcessingResult.failure("验证失败");
        }

        // Step 2: 预处理
        preprocess(ticket);

        // Step 3: 核心逻辑（子类实现）
        TicketProcessingResult result;
        try {
            result = execute(ticket);
        } catch (Exception e) {
            log.error("Processing failed for ticket: {}", ticket.getTicketNo(), e);
            return TicketProcessingResult.failure("处理异常: " + e.getMessage());
        }

        // Step 4: 后处理
        postprocess(ticket, result);

        // Step 5: 通知
        if (shouldNotify(ticket, result)) {
            notifyStakeholders(ticket, result);
        }

        log.info("Completed processing for ticket: {}, result: {}",
                ticket.getTicketNo(), result.success());
        return result;
    }

    // ===== 可覆盖的方法（钩子方法） =====

    protected boolean validate(Ticket ticket) {
        return ticket != null && ticket.getId() != null;
    }

    protected void preprocess(Ticket ticket) {
        // 默认空实现，子类可覆盖
    }

    protected void postprocess(Ticket ticket, TicketProcessingResult result) {
        // 默认空实现
    }

    protected boolean shouldNotify(Ticket ticket, TicketProcessingResult result) {
        return true; // 默认总是通知
    }

    protected void notifyStakeholders(Ticket ticket, TicketProcessingResult result) {
        log.info("Notifying stakeholders for ticket: {}", ticket.getTicketNo());
    }

    // ===== 抽象方法（子类必须实现） =====

    protected abstract TicketProcessingResult execute(Ticket ticket);
}
