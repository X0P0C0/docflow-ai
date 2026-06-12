package com.docflow.ai.common.pattern;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机 - 状态模式的核心实现
 *
 * 【设计模式】状态模式 (State Pattern)
 * 【面试考点】
 *   - 状态模式 vs 策略模式的区别：状态模式的行为由当前状态决定，策略模式由客户端选择
 *   - 状态转换表驱动：用 Map<State, Map<Event, State>> 实现，避免大量 if-else
 *   - 开闭原则：新增状态只需修改 TRANSITIONS 表，不改业务代码
 *
 * 【状态流转图】
 *   OPEN --ASSIGN--> OPEN (分配不改变状态)
 *   OPEN --START--> IN_PROGRESS
 *   IN_PROGRESS --RESOLVE--> RESOLVED
 *   RESOLVED --CLOSE--> CLOSED
 *   CLOSED --REOPEN--> OPEN
 *   任意状态 --CLOSE--> CLOSED (强制关闭)
 *
 * 【真实业务场景】
 *   客服创建工单(OPEN) -> 分配给技术支持(ASSIGN) -> 开始处理(START/IN_PROGRESS)
 *   -> 解决问题(RESOLVED) -> 客户确认关闭(CLOSED)
 *   如果客户不满意，可以重新打开(REOPEN -> OPEN)
 */
public class TicketStateMachine {

    /**
     * 工单状态枚举
     * code 用于数据库存储（整型比字符串更高效）
     */
    public enum State {
        OPEN(1),           // 待处理：新建工单的初始状态
        IN_PROGRESS(2),    // 处理中：已被认领并正在处理
        RESOLVED(3),       // 已解决：处理人标记为已解决，等待客户确认
        CLOSED(4);         // 已关闭：客户确认或超时自动关闭

        final int code;
        State(int code) { this.code = code; }
        public int getCode() { return code; }

        /**
         * 从数据库整型还原为枚举（MyBatis 映射时使用）
         */
        public static State fromCode(int code) {
            for (State s : values()) { if (s.code == code) return s; }
            throw new IllegalArgumentException("Invalid status: " + code);
        }
    }

    /**
     * 触发状态转换的事件
     */
    public enum Event {
        ASSIGN,     // 分配工单
        START,      // 开始处理
        RESOLVE,    // 标记已解决
        CLOSE,      // 关闭工单
        REOPEN      // 重新打开
    }

    /**
     * 状态转换表 - 核心数据结构
     *
     * 【面试考点】表驱动法(Table-Driven)替代 if-else/switch
     *   - 优点：转换规则一目了然，易于扩展和测试
     *   - 缺点：状态多时表会变大（可用数据库/配置文件管理）
     *
     * 结构：当前状态 -> (事件 -> 目标状态)
     * 例如：OPEN 状态下收到 ASSIGN 事件 -> 保持 OPEN
     */
    private static final Map<State, Map<Event, State>> TRANSITIONS = Map.of(
        State.OPEN,         Map.of(Event.ASSIGN, State.OPEN, Event.START, State.IN_PROGRESS, Event.CLOSE, State.CLOSED),
        State.IN_PROGRESS,  Map.of(Event.RESOLVE, State.RESOLVED, Event.CLOSE, State.CLOSED, Event.ASSIGN, State.IN_PROGRESS),
        State.RESOLVED,     Map.of(Event.CLOSE, State.CLOSED, Event.REOPEN, State.OPEN),
        State.CLOSED,       Map.of(Event.REOPEN, State.OPEN)
    );

    /**
     * 执行状态转换
     *
     * @param current 当前状态
     * @param event   触发事件
     * @return 转换后的目标状态
     * @throws BusinessException 如果转换不合法（例如 CLOSED 状态下收到 START 事件）
     */
    public static State transition(State current, Event event) {
        Map<Event, State> allowed = TRANSITIONS.get(current);
        if (allowed == null || !allowed.containsKey(event)) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT,
                "Invalid transition: " + current + " + " + event);
        }
        return allowed.get(event);
    }

    /**
     * 检查转换是否合法（不抛异常，用于前端按钮灰显判断）
     */
    public static boolean canTransition(State current, Event event) {
        Map<Event, State> allowed = TRANSITIONS.get(current);
        return allowed != null && allowed.containsKey(event);
    }

    /**
     * 获取当前状态下所有可用事件（用于动态渲染操作按钮）
     */
    public static Set<Event> availableEvents(State current) {
        Map<Event, State> allowed = TRANSITIONS.get(current);
        return allowed != null ? allowed.keySet() : Set.of();
    }
}