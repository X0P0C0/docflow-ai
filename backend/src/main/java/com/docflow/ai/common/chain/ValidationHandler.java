package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 责任链 - 验证处理器（链的第一个节点）
 *
 * 【设计模式】责任链模式 (Chain of Responsibility)
 * 【执行顺序】@Order(1) 保证验证最先执行
 *
 * 【职责】校验工单基本字段合法性
 *   - 工单不能为 null
 *   - 标题不能为空
 *   - 类型不能为空
 *
 * 【面试考点】
 *   - 责任链的"短路"特性：验证失败时返回 false，后续节点不执行
 *   - @Order 注解控制执行顺序（数字越小越先执行）
 *   - Spring 自动注入 List<Handler> + @Order 排序
 */
@Slf4j
@Component
@Order(1)
public class ValidationHandler extends AbstractProcessingHandler {

    @Override
    public String name() {
        return "Validation";
    }

    /**
     * 验证工单字段
     * @return true=验证通过，继续下一个处理器；false=验证失败，中断链
     */
    @Override
    protected boolean doHandle(ProcessingContext context) {
        var ticket = context.getTicket();
        if (ticket == null) {
            context.addLog("[Validation] FAILED: ticket is null");
            return false;
        }
        if (ticket.getTitle() == null || ticket.getTitle().isBlank()) {
            context.addLog("[Validation] FAILED: title is empty");
            return false;
        }
        if (ticket.getType() == null || ticket.getType().isBlank()) {
            context.addLog("[Validation] FAILED: type is empty");
            return false;
        }
        context.addLog("[Validation] PASSED");
        return true;
    }
}