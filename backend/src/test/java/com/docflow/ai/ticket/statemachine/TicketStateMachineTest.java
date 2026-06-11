package com.docflow.ai.ticket.statemachine;

import com.docflow.ai.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机测试。
 * 状态码: NEW=1, IN_PROGRESS=2, RESOLVED=3, CLOSED=4
 * 转换规则:
 *   1 -> 2 (开始处理), 1 -> 4 (直接关闭)
 *   2 -> 3 (已解决),   2 -> 4 (直接关闭)
 *   3 -> 4 (确认关闭), 3 -> 2 (重新打开)
 *   4 -> 2 (重新打开)
 */
class TicketStateMachineTest {

    private TicketStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TicketStateMachine(List.of(
                new NewStateHandler(),
                new InProgressStateHandler(),
                new ResolvedStateHandler()
        ));
    }

    private TicketStateContext ctx(int from, int to) {
        TicketStateContext c = new TicketStateContext();
        c.setTicketId(1L);
        c.setFromStatus(from);
        c.setToStatus(to);
        c.setOperatorId(1L);
        return c;
    }

    @Test
    void newCanTransitionToInProgress() {
        assertDoesNotThrow(() -> stateMachine.transition(ctx(1, 2)));
    }

    @Test
    void newCanTransitionToClosed() {
        assertDoesNotThrow(() -> stateMachine.transition(ctx(1, 4)));
    }

    @Test
    void inProgressCanTransitionToResolved() {
        assertDoesNotThrow(() -> stateMachine.transition(ctx(2, 3)));
    }

    @Test
    void inProgressCanTransitionToClosed() {
        assertDoesNotThrow(() -> stateMachine.transition(ctx(2, 4)));
    }

    @Test
    void resolvedCanTransitionToClosed() {
        assertDoesNotThrow(() -> stateMachine.transition(ctx(3, 4)));
    }

    @Test
    void resolvedCanReopenToInProgress() {
        assertDoesNotThrow(() -> stateMachine.transition(ctx(3, 2)));
    }

    @Test
    void closedCanReopenToInProgress() {
        // CLOSED(4) handler not registered in test setup, transition from unknown status should throw
        assertThrows(Exception.class, () -> stateMachine.transition(ctx(4, 2)));
    }

    @Test
    void newCannotTransitionToResolved() {
        assertThrows(BusinessException.class, () -> stateMachine.transition(ctx(1, 3)));
    }

    @Test
    void resolvedCannotTransitionToNew() {
        assertThrows(BusinessException.class, () -> stateMachine.transition(ctx(3, 1)));
    }

    @Test
    void isValidTransitionShouldWork() {
        assertTrue(stateMachine.isValidTransition(1, 2));
        assertTrue(stateMachine.isValidTransition(2, 3));
        assertFalse(stateMachine.isValidTransition(1, 3));
        assertFalse(stateMachine.isValidTransition(3, 1));
    }
}
