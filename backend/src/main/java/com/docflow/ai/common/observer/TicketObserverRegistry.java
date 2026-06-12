package com.docflow.ai.common.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 观察者注册中心 - 观察者模式的核心调度器
 *
 * 【设计模式】观察者模式 (Observer Pattern)
 * 【面试考点】
 *   - 观察者模式 vs 发布-订阅模式：观察者直接通知，发布-订阅通过中间件
 *   - Spring 事件机制（ApplicationEvent）就是观察者模式的框架级应用
 *   - 同步 vs 异步通知的选择：同步保证事务内一致性，异步解耦非关键路径
 *   - 观察者异常不应影响主流程（隔离性）
 *
 * 【当前注册的观察者】
 *   - AuditObserver: 记录操作审计日志
 *   - SlaObserver: 检查 SLA 超时并触发告警
 *
 * 【真实业务场景】
 *   工单状态变更 -> 通知审计日志记录变更 -> 通知 SLA 检查是否超时
 *   -> 通知 WebSocket 推送给前端 -> 通知邮件/短信通知客户
 *   每个观察者独立，新增通知渠道只需添加一个 Observer 实现
 */
@Slf4j
@Component
public class TicketObserverRegistry {

    private final List<TicketObserver> observers = new ArrayList<>();

    /**
     * Spring 构造注入所有 TicketObserver Bean（自动发现机制）
     * 【面试考点】Spring 的 List 注入：自动收集所有实现类，按 @Order 排序
     */
    public TicketObserverRegistry(List<TicketObserver> observers) {
        this.observers.addAll(observers);
        this.observers.sort(Comparator.comparing(TicketObserver::name));
        log.info("Registered {} ticket observers: {}", observers.size(),
                observers.stream().map(TicketObserver::name).toList());
    }

    /**
     * 同步通知所有观察者（事务内执行，保证数据一致性）
     *
     * 【注意】观察者异常不会向上传播，避免影响主业务流程
     * 这是"优雅降级"思想：审计日志写失败不应阻止工单创建
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
     * 异步通知所有观察者（事务外执行，适合非关键路径）
     *
     * 【适用场景】发送邮件、推送通知等不需要强一致性的操作
     * 【面试考点】异步执行需要注意：线程池配置、异常处理、上下文传递
     */
    public void notifyAllAsync(TicketEvent event) {
        CompletableFuture.runAsync(() -> notifyAll(event));
    }
}