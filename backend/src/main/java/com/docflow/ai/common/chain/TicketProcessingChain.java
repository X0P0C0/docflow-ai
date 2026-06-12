package com.docflow.ai.common.chain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 责任链构建器 - 自动组装处理链
 *
 * 【设计模式】责任链模式 (Chain of Responsibility)
 * 【面试考点】
 *   - 责任链 vs 装饰器：责任链可以中断（某个节点不放行），装饰器一定会执行所有层
 *   - 责任链 vs 过滤器链：本质相同，Servlet Filter 就是责任链的实现
 *   - Spring 自动注入 List<Handler> 构建链，避免手动配置顺序
 *
 * 【当前处理链顺序】
 *   1. ValidationHandler - 校验工单参数合法性
 *   2. RoutingHandler    - 自动路由分配处理人
 *   3. NotificationHandler - 发送通知
 *
 * 【真实业务场景】
 *   工单提交 -> 参数校验(必填项/格式) -> 路由分配(按类型/VIP等级)
 *   -> 通知(邮件/短信/WebSocket)
 *   如果校验失败，直接返回错误，不执行后续步骤
 */
@Slf4j
@Component
public class TicketProcessingChain {

    /** 链头节点，从这里开始执行 */
    private final TicketProcessingHandler head;

    /**
     * Spring 构造注入所有 Handler，自动构建链表结构
     * 【实现要点】
     *   - handlers 列表的顺序决定执行顺序（可通过 @Order 控制）
     *   - 空链使用 NoOpHandler 兜底，避免空指针
     *   - 链表结构而非数组遍历，每个节点可以决定是否传递给下一个
     */
    public TicketProcessingChain(List<TicketProcessingHandler> handlers) {
        if (handlers.isEmpty()) {
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
     * 执行处理链
     * @param context 处理上下文，链上的每个节点都可以读写
     * @return 最终的处理上下文（包含处理结果和错误信息）
     */
    public ProcessingContext execute(ProcessingContext context) {
        head.handle(context);
        return context;
    }

    /** 空操作处理器 - 链为空时的兜底实现（空对象模式） */
    private static class NoOpHandler implements TicketProcessingHandler {
        @Override public String name() { return "NoOp"; }
        @Override public void setNext(TicketProcessingHandler next) {}
        @Override public boolean handle(ProcessingContext context) { return true; }
    }
}