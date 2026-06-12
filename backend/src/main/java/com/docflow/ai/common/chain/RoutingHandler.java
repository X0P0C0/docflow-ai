package com.docflow.ai.common.chain;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 责任链 - 路由处理器（链的第二个节点）
 *
 * 【设计模式】责任链模式 (Chain of Responsibility)
 * 【执行顺序】@Order(2) 在验证之后执行
 *
 * 【职责】
 *   1. 根据工单类型设置默认优先级（如果未指定）
 *   2. 根据标题关键词智能升级优先级
 *
 * 【面试考点】
 *   - 责任链的"增强"特性：每个节点可以修改上下文中的数据
 *   - 与过滤器链的区别：过滤器通常不修改请求内容，责任链可以
 */
@Slf4j
@Component
@Order(2)
public class RoutingHandler extends AbstractProcessingHandler {

    @Override
    public String name() {
        return "Routing";
    }

    @Override
    protected boolean doHandle(ProcessingContext context) {
        Ticket ticket = context.getTicket();

        // 根据类型设置默认优先级（如果未指定）
        if (ticket.getPriority() == null) {
            switch (ticket.getType()) {
                case "incident" -> ticket.setPriority(2);  // 高
                case "task" -> ticket.setPriority(1);      // 中
                case "question" -> ticket.setPriority(0);  // 低
            }
            context.addLog("[Routing] Auto-set priority: " + ticket.getPriority());
        }

        // 根据标题关键词智能升级优先级（NLP 的简化版）
        String title = ticket.getTitle().toLowerCase();
        if (title.contains("urgent") || title.contains("紧急") || title.contains("生产")) {
            ticket.setPriority(3); // 紧急
            context.addLog("[Routing] Escalated to URGENT based on title keywords");
        }

        context.addLog("[Routing] PASSED");
        return true;
    }
}