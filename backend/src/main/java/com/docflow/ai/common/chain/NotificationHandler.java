package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 责任链 —— 通知处理器。
 * 最后一步：记录需要发送的通知（实际发送由异步事件完成）。
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
        // 标记需要通知的人员
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
