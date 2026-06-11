package com.docflow.ai.ticket.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单策略注册中心。
 * Spring 自动注入所有 TicketTypeStrategy 实现，
 * 按 type 建索引，运行时 O(1) 查找。
 */
@Slf4j
@Component
public class TicketStrategyContext {

    private final Map<String, TicketTypeStrategy> strategies = new HashMap<>();

    public TicketStrategyContext(List<TicketTypeStrategy> strategyList) {
        for (TicketTypeStrategy strategy : strategyList) {
            strategies.put(strategy.getType(), strategy);
        }
        log.info("Ticket strategies registered: {}", strategies.keySet());
    }

    public TicketTypeStrategy getStrategy(String type) {
        TicketTypeStrategy strategy = strategies.get(type);
        if (strategy == null) {
            strategy = strategies.get("INCIDENT"); // Default fallback
        }
        return strategy;
    }
}
