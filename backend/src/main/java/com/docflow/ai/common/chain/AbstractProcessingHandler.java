package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;

/**
 * 责任链抽象基类 —— 提供链式调用基础设施。
 */
@Slf4j
public abstract class AbstractProcessingHandler implements TicketProcessingHandler {

    private TicketProcessingHandler next;

    @Override
    public void setNext(TicketProcessingHandler next) {
        this.next = next;
    }

    @Override
    public boolean handle(ProcessingContext context) {
        boolean shouldContinue = doHandle(context);
        if (shouldContinue && next != null) {
            return next.handle(context);
        }
        return shouldContinue;
    }

    /**
     * 子类实现具体处理逻辑。
     */
    protected abstract boolean doHandle(ProcessingContext context);
}
