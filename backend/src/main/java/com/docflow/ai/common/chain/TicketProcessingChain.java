package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 责任链构建器。
 * 自动注册所有 TicketProcessingHandler 实现，构建处理链。
 * 技术点：责任链模式 + Spring 自动注入。
 */
@Slf4j
@Component
public class TicketProcessingChain {

    private final TicketProcessingHandler head;

    public TicketProcessingChain(List<TicketProcessingHandler> handlers) {
        if (handlers.isEmpty()) {
            // 空链，直接通过
            head = new NoOpHandler();
        } else {
            head = handlers.get(0);
            TicketProcessingHandler current = head;
            for (int i = 1; i < handlers.size(); i++) {
                current.setNext(handlers.get(i));
                current = handlers.get(i);
            }
            log.info("Processing chain built with {} handlers: {}",
                    handlers.size(), handlers.stream().map(TicketProcessingHandler::name).toList());
        }
    }

    /**
     * 执行处理链。
     */
    public ProcessingContext execute(ProcessingContext context) {
        head.handle(context);
        return context;
    }

    private static class NoOpHandler implements TicketProcessingHandler {
        @Override public String name() { return "NoOp"; }
        @Override public void setNext(TicketProcessingHandler next) {}
        @Override public boolean handle(ProcessingContext context) { return true; }
    }
}
