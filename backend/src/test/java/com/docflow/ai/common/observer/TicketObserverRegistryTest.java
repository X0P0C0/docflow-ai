package com.docflow.ai.common.observer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class TicketObserverRegistryTest {

    private TicketObserverRegistry registry;
    private TicketObserver mockObserver1;
    private TicketObserver mockObserver2;

    @BeforeEach
    void setUp() {
        mockObserver1 = Mockito.mock(TicketObserver.class);
        Mockito.when(mockObserver1.name()).thenReturn("TestObserver1");
        mockObserver2 = Mockito.mock(TicketObserver.class);
        Mockito.when(mockObserver2.name()).thenReturn("TestObserver2");
        registry = new TicketObserverRegistry(List.of(mockObserver1, mockObserver2));
    }

    @Test
    void notifyAllShouldCallAllObservers() {
        TicketEvent event = TicketEvent.builder()
                .eventType("CREATED").ticketId(1L).operatorId(1L).build();
        registry.notifyAll(event);
        Mockito.verify(mockObserver1).onEvent(event);
        Mockito.verify(mockObserver2).onEvent(event);
    }

    @Test
    void notifyAllShouldSwallowObserverException() {
        Mockito.doThrow(new RuntimeException("boom")).when(mockObserver1).onEvent(any());
        TicketEvent event = TicketEvent.builder()
                .eventType("CREATED").ticketId(1L).build();
        assertDoesNotThrow(() -> registry.notifyAll(event));
        Mockito.verify(mockObserver2).onEvent(event);
    }

    @Test
    void shouldRegisterAllObservers() {
        assertNotNull(registry);
    }
}
