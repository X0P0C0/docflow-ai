package com.docflow.ai.common.pattern.strategy;

import com.docflow.ai.ticket.entity.Ticket;
import org.springframework.stereotype.Component;

/**
 * 按工单类型路由 - 策略模式的具体实现
 *
 * 【设计模式】策略模式 (Strategy Pattern)
 * 【路由规则】
 *   INCIDENT（故障）-> 运维团队
 *   TASK（任务）    -> 开发团队
 *   QUESTION（咨询）-> 客服团队
 *
 * 【真实业务场景】
 *   客户报告"系统无法登录"（INCIDENT）-> 自动分配给运维
 *   客户请求"新增报表功能"（TASK）    -> 自动分配给开发
 *   客户咨询"如何导出数据"（QUESTION）-> 自动分配给客服
 */
@Component
public class TypeRoutingStrategy implements TicketRoutingStrategy {

    @Override
    public boolean supports(Ticket ticket) {
        return ticket.getType() != null && !ticket.getType().isEmpty();
    }

    @Override
    public Long resolveAssignee(Ticket ticket) {
        return switch (ticket.getType()) {
            case "INCIDENT" -> 2L;  // 运维工程师
            case "TASK" -> 3L;      // 开发工程师
            case "QUESTION" -> 4L;  // 客服
            default -> 2L;          // 默认运维
        };
    }

    @Override
    public String getName() {
        return "type-routing";
    }
}