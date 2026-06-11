package com.docflow.ai.common.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 观察者注册中心。
 * 管理所有工单观察者，支持同步/异步通知。
 * 技术点：观察者模式的注册中心，Spring 自动注入所有 TicketObserver 实现。
 */
@Slf4j
@Component
public class TicketObserverRegistry {

    private final List<TicketObserver> observers = new ArrayList<>();

    /**
     * Spring 构造注入所有 TicketObserver Bean。
     */
    public TicketObserverRegistry(List<TicketObserver> observers) {
        this.observers.addAll(observers);
        this.observers.sort(Comparator.comparing(TicketObserver::name));
        log.info("Registered {} ticket observers: {}", observers.size(),
                observers.stream().map(TicketObserver::name).toList());
    }

    /**
     * 同步通知所有观察者（事务内执行）。
     */
    public void notifyAll(TicketEvent event) {
        for (TicketObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                log.error("Observer [{}] failed for event {}: {}",
                        observer.name(), event.getEventType(), e.getMessage(), e);
                // 观察者异常不影响主流程
            }
        }
    }

    /**
     * 异步通知所有观察者（事务外执行，适合非关键路径）。
     */
    public void notifyAllAsync(TicketEvent event) {
        CompletableFuture.runAsync(() -> notifyAll(event));
    }
}
