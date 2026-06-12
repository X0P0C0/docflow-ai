package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;

/**
 * 责任链抽象基类 - 提供链式调用基础设施
 *
 * 【设计模式】责任链模式 (Chain of Responsibility) 的抽象基类
 * 【面试考点】
 *   - 模板方法模式在这里也有应用：handle() 是模板方法，doHandle() 是抽象方法
 *   - 链式调用的实现：每个节点持有 next 引用，形成单向链表
 *   - 短路特性：doHandle() 返回 false 时，后续节点不执行
 *
 * 【链式调用流程】
 *   ValidationHandler.handle(context)
 *     -> doHandle(context)  // 验证
 *     -> shouldContinue && next != null
 *     -> RoutingHandler.handle(context)
 *       -> doHandle(context)  // 路由
 *       -> shouldContinue && next != null
 *       -> NotificationHandler.handle(context)
 *         -> doHandle(context)  // 通知
 *         -> next == null, 链结束
 */
@Slf4j
public abstract class AbstractProcessingHandler implements TicketProcessingHandler {

    /** 下一个处理器（链表结构） */
    private TicketProcessingHandler next;

    @Override
    public void setNext(TicketProcessingHandler next) {
        this.next = next;
    }

    /**
     * 模板方法：执行当前节点 + 决定是否继续
     *
     * 【面试考点】这里同时使用了责任链和模板方法两种模式
     *   - 责任链：节点串联，逐个执行
     *   - 模板方法：handle() 定义算法骨架，doHandle() 由子类实现
     */
    @Override
    public boolean handle(ProcessingContext context) {
        boolean shouldContinue = doHandle(context);
        if (shouldContinue && next != null) {
            return next.handle(context); // 传递给下一个节点
        }
        return shouldContinue;
    }

    /** 子类实现具体处理逻辑（模板方法的抽象步骤） */
    protected abstract boolean doHandle(ProcessingContext context);
}