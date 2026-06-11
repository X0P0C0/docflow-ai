package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 责任链 —— 验证处理器。
 * 第一步：校验工单基本字段合法性。
 */
@Slf4j
@Component
@Order(1)
public class ValidationHandler extends AbstractProcessingHandler {

    @Override
    public String name() {
        return "Validation";
    }

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
