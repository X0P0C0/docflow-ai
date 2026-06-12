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
 * SLA 观察者 - 观察者模式的具体实现
 *
 * 【设计模式】观察者模式 (Observer Pattern)
 * 【业务逻辑】
 *   工单创建时自动检查 SLA 策略并设置截止时间
 *   SLA (Service Level Agreement) = 服务等级协议
 *
 * 【SLA 概念】（面试常问）
 *   - 响应时间：从工单创建到首次回复的时间限制
 *   - 解决时间：从工单创建到最终解决的时间限制
 *   - 优先级越高，SLA 越严格（P0 可能要求 15 分钟内响应）
 *
 * 【真实业务场景】
 *   企业客户签了 SLA 协议：P0 故障 15 分钟响应，2 小时解决
 *   工单创建时自动计算截止时间，超时前自动告警
 *   超时后自动升级给管理层
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

    /**
     * 只处理工单创建事件
     * 【面试考点】观察者的过滤逻辑：不是所有事件都需要处理
     */
    @Override
    public void onEvent(TicketEvent event) {
        if (!"CREATED".equals(event.getEventType())) return;

        // 查询匹配的 SLA 策略
        List<SlaPolicy> policies = slaPolicyMapper.selectList(
                new LambdaQueryWrapper<SlaPolicy>()
                        .eq(SlaPolicy::getStatus, 1));
        if (!policies.isEmpty()) {
            log.info("SLA check: ticketId={}, {} policies applicable", event.getTicketId(), policies.size());
            // 实际应该根据工单优先级选择最匹配的 SLA 策略
            // 并计算截止时间写入工单
        }
    }
}