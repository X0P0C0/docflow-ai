package com.docflow.ai.common.pattern.template;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;

/**
 * 工单处理模板 - 模板方法模式 (Template Method Pattern)
 *
 * 【设计模式】模板方法模式
 * 【面试考点】
 *   - 模板方法 vs 策略模式：模板方法用继承，策略用组合
 *   - 好莱坞原则：Don't call us, we'll call you（父类调用子类，而非子类调用父类）
 *   - 钩子方法(Hook Method)：子类可选覆盖的方法，提供默认实现
 *   - 模板方法必须是 final，防止子类破坏算法结构
 *
 * 【处理流程】（5 步，子类只能覆盖钩子方法，不能改变流程顺序）
 *   1. validate()    - 校验工单（钩子方法，可覆盖）
 *   2. preprocess()  - 预处理（钩子方法，默认空实现）
 *   3. execute()     - 核心逻辑（抽象方法，子类必须实现）
 *   4. postprocess() - 后处理（钩子方法，默认空实现）
 *   5. notify()      - 通知（钩子方法，可控制是否通知）
 *
 * 【子类实现】
 *   - IncidentProcessingTemplate: 事件类工单（紧急，需要立即处理）
 *   - QuestionProcessingTemplate: 问题类工单（需要查询知识库）
 *
 * 【真实业务场景】
 *   不同类型的工单有不同的处理流程，但整体框架一致：
 *   事件类：校验 -> 标记紧急 -> 执行(升级通知) -> 后处理(记录SLA) -> 通知主管
 *   问题类：校验 -> 查询知识库 -> 执行(自动回复) -> 后处理(沉淀知识) -> 通知客户
 */
@Slf4j
public abstract class TicketProcessingTemplate {

    /**
     * 模板方法 - 定义处理流程骨架（final 防止子类覆盖）
     *
     * 【面试考点】为什么用 final？
     *   - 保证算法结构不被子类破坏
     *   - 子类只能通过覆盖钩子方法来定制行为
     *   - 这就是"控制反转"的体现
     */
    public final TicketProcessingResult process(Ticket ticket) {
        log.info("Starting processing for ticket: {}", ticket != null ? ticket.getTicketNo() : "null");

        // Step 1: 校验
        if (!validate(ticket)) {
            return TicketProcessingResult.failure("校验失败");
        }

        // Step 2: 预处理（钩子方法）
        preprocess(ticket);

        // Step 3: 核心逻辑（抽象方法，子类实现）
        TicketProcessingResult result;
        try {
            result = execute(ticket);
        } catch (Exception e) {
            log.error("Processing failed for ticket: {}", ticket.getTicketNo(), e);
            return TicketProcessingResult.failure("处理异常: " + e.getMessage());
        }

        // Step 4: 后处理（钩子方法）
        postprocess(ticket, result);

        // Step 5: 通知（钩子方法，可控制）
        if (shouldNotify(ticket, result)) {
            notifyStakeholders(ticket, result);
        }

        log.info("Completed processing for ticket: {}, result: {}",
                ticket.getTicketNo(), result.success());
        return result;
    }

    // ===== 钩子方法（Hook Methods）- 子类可选覆盖 =====

    /** 校验工单（默认检查非空和 ID 存在） */
    protected boolean validate(Ticket ticket) {
        return ticket != null && ticket.getId() != null;
    }

    /** 预处理（默认空实现，子类可覆盖添加逻辑） */
    protected void preprocess(Ticket ticket) {
        // 默认空实现
    }

    /** 后处理（默认空实现） */
    protected void postprocess(Ticket ticket, TicketProcessingResult result) {
        // 默认空实现
    }

    /** 是否需要通知（默认总是通知，子类可覆盖为条件通知） */
    protected boolean shouldNotify(Ticket ticket, TicketProcessingResult result) {
        return true;
    }

    /** 通知相关人员（默认日志记录，子类可覆盖为邮件/短信） */
    protected void notifyStakeholders(Ticket ticket, TicketProcessingResult result) {
        log.info("Notifying stakeholders for ticket: {}", ticket.getTicketNo());
    }

    // ===== 抽象方法（Abstract Methods）- 子类必须实现 =====

    /**
     * 核心处理逻辑（由子类定义具体行为）
     * 这是模板方法模式的关键：算法骨架固定，核心步骤可定制
     */
    protected abstract TicketProcessingResult execute(Ticket ticket);
}