package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工单路由器 - 策略模式的上下文 (Context)
 *
 * 【设计模式】策略模式 (Strategy Pattern) + 责任链思想
 * 【面试考点】
 *   - 策略模式的核心：定义算法族，使它们可以互相替换
 *   - Spring 自动注入所有 TicketRoutingStrategy 实现，自动组装策略链
 *   - 新增路由规则只需加一个 @Component 类，无需修改路由器代码（开闭原则）
 *
 * 【路由策略优先级】（由 @Order 或类名排序决定）
 *   1. PriorityRoutingStrategy - VIP/紧急工单优先分配给高级处理人
 *   2. TypeRoutingStrategy     - 按工单类型（技术/账单/一般）分配对应团队
 *   3. FallbackRoutingStrategy - 兜底策略，轮询分配
 *
 * 【真实业务场景】
 *   客户提交工单 -> 系统自动判断：如果是"支付失败"类型 -> 分配给财务团队
 *   如果是 VIP 客户的紧急工单 -> 直接分配给高级技术支持
 *   如果没有匹配规则 -> 轮询分配给在线客服
 */
@Slf4j
@Component
public class TicketRouter {

    /**
     * Spring 自动注入所有 TicketRoutingStrategy 实现，按顺序排列
     * 这是策略模式 + Spring 依赖注入的典型结合
     */
    private final List<TicketRoutingStrategy> strategies;

    public TicketRouter(List<TicketRoutingStrategy> strategies) {
        this.strategies = strategies;
        log.info("TicketRouter initialized with {} strategies: {}",
                strategies.size(),
                strategies.stream().map(TicketRoutingStrategy::getName).toList());
    }

    /**
     * 路由工单到合适的处理人
     *
     * 【实现原理】遍历策略链，找到第一个 supports() 返回 true 的策略执行
     * 类似于 Servlet FilterChain 的工作方式
     *
     * @param ticket 待路由的工单
     * @return 指派的处理人 ID，null 表示需要手动指派
     */
    public Long route(Ticket ticket) {
        for (TicketRoutingStrategy strategy : strategies) {
            if (strategy.supports(ticket)) {
                Long assigneeId = strategy.resolveAssignee(ticket);
                log.info("Ticket {} routed via strategy [{}] -> assignee {}",
                        ticket.getTicketNo(), strategy.getName(), assigneeId);
                return assigneeId;
            }
        }
        log.warn("No routing strategy matched for ticket {}", ticket.getTicketNo());
        return null;
    }
}