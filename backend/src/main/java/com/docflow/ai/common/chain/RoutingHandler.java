package com.docflow.ai.common.chain;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 责任链 —— 路由处理器。
 * 第二步：根据工单类型和优先级，自动设置分类。
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

        // 根据类型设置默认优先级
        if (ticket.getPriority() == null) {
            switch (ticket.getType()) {
                case "incident" -> ticket.setPriority(2);  // 高
                case "task" -> ticket.setPriority(1);      // 中
                case "question" -> ticket.setPriority(0);  // 低
            }
            context.addLog("[Routing] Auto-set priority: " + ticket.getPriority());
        }

        // 根据标题关键词智能分类
        String title = ticket.getTitle().toLowerCase();
        if (title.contains("urgent") || title.contains("紧急") || title.contains("生产")) {
            ticket.setPriority(3); // 紧急
            context.addLog("[Routing] Escalated to URGENT based on title keywords");
        }

        context.addLog("[Routing] PASSED");
        return true;
    }
}
