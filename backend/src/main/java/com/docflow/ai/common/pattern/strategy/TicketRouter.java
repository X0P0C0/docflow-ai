package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 工单路由器 —— 使用策略链自动选择最佳路由策略
 * <p>
 * 设计亮点：
 * <ul>
 *   <li>策略链自动遍历，找到第一个匹配的策略执行</li>
 *   <li>新增路由规则只需添加一个 @Component 类</li>
 *   <li>运行时可动态调整策略顺序</li>
 * </ul>
 */
@Slf4j
@Component
public class TicketRouter {

    private final List<TicketRoutingStrategy> strategies;

    public TicketRouter(List<TicketRoutingStrategy> strategies) {
        this.strategies = strategies;
        log.info("TicketRouter initialized with {} strategies: {}",
                strategies.size(),
                strategies.stream().map(TicketRoutingStrategy::getName).toList());
    }

    /**
     * 路由工单到合适的处理人
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
