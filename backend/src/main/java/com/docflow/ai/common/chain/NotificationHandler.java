package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 责任链 - 通知处理器（链的最后一个节点）
 *
 * 【设计模式】责任链模式 (Chain of Responsibility)
 * 【执行顺序】@Order(3) 最后执行
 *
 * 【职责】记录需要发送的通知（实际发送由异步事件完成）
 *
 * 【面试考点】
 *   - 责任链的"终止"特性：最后一个节点执行完，链结束
 *   - 通知的异步化：这里只记录，实际发送由 WebSocket/邮件服务异步完成
 *   - 解耦：业务逻辑不关心通知的具体实现（可以是邮件、短信、WebSocket）
 */
@Slf4j
@Component
@Order(3)
public class NotificationHandler extends AbstractProcessingHandler {

    @Override
    public String name() {
        return "Notification";
    }

    @Override
    protected boolean doHandle(ProcessingContext context) {
        // 根据操作类型决定通知谁
        if ("CREATE".equals(context.getAction())) {
            context.addLog("[Notification] Will notify: assignee, watchers");
        } else if ("ASSIGN".equals(context.getAction())) {
            context.addLog("[Notification] Will notify: new assignee");
        } else if ("CLOSE".equals(context.getAction())) {
            context.addLog("[Notification] Will notify: creator (ticket closed)");
        }
        context.addLog("[Notification] PASSED");
        return true;
    }
}