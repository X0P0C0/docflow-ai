package com.docflow.ai.common.chain;

/**
 * 责任链处理器接口。
 * 技术点：责任链模式，将工单处理流程拆分为多个可组合的步骤。
 * 每个处理器决定是否继续传递给下一个处理器。
 */
public interface TicketProcessingHandler {

    /**
     * 处理器名称。
     */
    String name();

    /**
     * 设置下一个处理器。
     */
    void setNext(TicketProcessingHandler next);

    /**
     * 处理工单请求。返回 true 表示继续传递，false 表示终止链。
     */
    boolean handle(ProcessingContext context);
}
