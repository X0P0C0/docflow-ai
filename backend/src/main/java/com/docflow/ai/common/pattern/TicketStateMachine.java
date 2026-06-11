package com.docflow.ai.common.pattern;

import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import java.util.Map;
import java.util.Set;

public class TicketStateMachine {
    public enum State {
        OPEN(1), IN_PROGRESS(2), RESOLVED(3), CLOSED(4);
        final int code;
        State(int code) { this.code = code; }
        public int getCode() { return code; }
        public static State fromCode(int code) {
            for (State s : values()) { if (s.code == code) return s; }
            throw new IllegalArgumentException("Invalid status: " + code);
        }
    }

    public enum Event { ASSIGN, START, RESOLVE, CLOSE, REOPEN }

    private static final Map<State, Map<Event, State>> TRANSITIONS = Map.of(
        State.OPEN, Map.of(Event.ASSIGN, State.OPEN, Event.START, State.IN_PROGRESS, Event.CLOSE, State.CLOSED),
        State.IN_PROGRESS, Map.of(Event.RESOLVE, State.RESOLVED, Event.CLOSE, State.CLOSED, Event.ASSIGN, State.IN_PROGRESS),
        State.RESOLVED, Map.of(Event.CLOSE, State.CLOSED, Event.REOPEN, State.OPEN),
        State.CLOSED, Map.of(Event.REOPEN, State.OPEN)
    );

    public static State transition(State current, Event event) {
        Map<Event, State> allowed = TRANSITIONS.get(current);
        if (allowed == null || !allowed.containsKey(event)) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "Invalid transition: " + current + " + " + event);
        }
        return allowed.get(event);
    }

    public static boolean canTransition(State current, Event event) {
        Map<Event, State> allowed = TRANSITIONS.get(current);
        return allowed != null && allowed.containsKey(event);
    }

    public static Set<Event> availableEvents(State current) {
        Map<Event, State> allowed = TRANSITIONS.get(current);
        return allowed != null ? allowed.keySet() : Set.of();
    }
}
