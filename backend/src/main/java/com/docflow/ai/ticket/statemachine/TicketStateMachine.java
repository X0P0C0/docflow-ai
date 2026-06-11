package com.docflow.ai.ticket.statemachine;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单状态机引擎。
 * 
 * 使用策略模式 + 状态模式组合：
 * - 每个状态有独立的 Handler（状态模式）
 * - 引擎根据当前状态选择 Handler（策略模式）
 * - 状态转换有明确的前置校验和后置副作用
 */
@Slf4j
@Component
public class TicketStateMachine {

    private final Map<Integer, TicketStateHandler> handlerMap = new HashMap<>();

    public TicketStateMachine(List<TicketStateHandler> handlers) {
        for (TicketStateHandler handler : handlers) {
            handlerMap.put(handler.getStatus(), handler);
        }
        log.info("TicketStateMachine initialized with {} state handlers: {}", handlers.size(), handlerMap.keySet());
    }

    /**
     * 执行状态转换。
     * 
     * @param context 转换上下文
     * @throws BusinessException 如果转换不合法
     */
    public void transition(TicketStateContext context) {
        TicketStateHandler handler = handlerMap.get(context.getFromStatus());
        if (handler == null) {
            throw new BusinessException(ResultCode.BUSINESS_RULE_VIOLATION,
                    "Unknown ticket status: " + context.getFromStatus());
        }
        if (!handler.canTransitionTo(context.getToStatus())) {
            throw new BusinessException(ResultCode.BUSINESS_RULE_VIOLATION,
                    String.format("Cannot transition from %d to %d", context.getFromStatus(), context.getToStatus()));
        }
        handler.onBeforeTransition(context);
        log.info("Ticket {} transitioning: {} -> {}", context.getTicketId(), context.getFromStatus(), context.getToStatus());
        handler.onAfterTransition(context);
    }

    public boolean isValidTransition(int fromStatus, int toStatus) {
        TicketStateHandler handler = handlerMap.get(fromStatus);
        return handler != null && handler.canTransitionTo(toStatus);
    }
}
