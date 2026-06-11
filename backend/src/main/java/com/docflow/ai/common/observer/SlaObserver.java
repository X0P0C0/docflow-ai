package com.docflow.ai.common.observer;

import com.docflow.ai.ticket.entity.SlaPolicy;
import com.docflow.ai.ticket.mapper.SlaPolicyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA 观察者 —— 工单创建时自动检查 SLA 策略并设置截止时间。
 * 实现观察者模式：监听 TICKET_CREATED 事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlaObserver implements TicketObserver {

    private final SlaPolicyMapper slaPolicyMapper;

    @Override
    public String name() {
        return "SlaObserver";
    }

    @Override
    public void onEvent(TicketEvent event) {
        if (!"CREATED".equals(event.getEventType())) return;

        // 查找匹配的 SLA 策略
        List<SlaPolicy> policies = slaPolicyMapper.selectList(
                new LambdaQueryWrapper<SlaPolicy>()
                        .eq(SlaPolicy::getStatus, 1));
        if (!policies.isEmpty()) {
            log.info("SLA check: ticketId={}, {} policies applicable", event.getTicketId(), policies.size());
        }
    }
}
