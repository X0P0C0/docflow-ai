package com.docflow.ai.common.resilience;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 高可用服务 - 熔断器 + 重试（基于 Resilience4j）
 *
 * 【面试考点】（微服务高可用三板斧：熔断、降级、限流）
 *
 * 【熔断器原理】（类似电路保险丝）
 *   CLOSED -> 正常状态，请求正常通过
 *   OPEN   -> 熔断状态，请求直接拒绝（不打下游服务）
 *   HALF_OPEN -> 半开状态，放少量请求试探下游是否恢复
 *
 * 【状态转换】
 *   CLOSED -> OPEN: 失败率超过阈值（如 50%），或慢调用比例超标
 *   OPEN -> HALF_OPEN: 等待一段时间后自动进入半开
 *   HALF_OPEN -> CLOSED: 试探请求成功
 *   HALF_OPEN -> OPEN: 试探请求失败
 *
 * 【重试机制】
 *   指数退避重试：第 1 次等 1s，第 2 次等 2s，第 3 次等 4s...
 *   避免重试风暴（所有客户端同时重试会压垮下游）
 *
 * 【降级策略】
 *   executeWithFallback: 熔断或异常时执行兜底逻辑
 *   例如：AI 分析服务不可用时，返回"暂无分析结果"而非报错
 *
 * 【真实业务场景】
 *   调用外部 AI 服务 -> 如果超时/错误率高 -> 熔断 -> 返回兜底结果
 *   避免一个服务故障拖垮整个系统（防止雪崩）
 *
 * 【Resilience4j vs Hystrix】
 *   Hystrix 已停止维护，Resilience4j 是官方推荐的替代方案
 *   基于函数式编程，更轻量，支持 Java 17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilienceService {

    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * 执行带熔断和重试的操作
     *
     * 【装饰器链】
     *   原始操作 -> CircuitBreaker 包装 -> Retry 包装 -> 执行
     *   执行顺序：先重试，每次重试前检查熔断器状态
     */
    public <T> T execute(String instanceName, Supplier<T> action) {
        CircuitBreaker cb = cbRegistry.circuitBreaker(instanceName);
        Retry retry = retryRegistry.retry(instanceName);

        // 装饰器模式：给原始操作套上熔断 + 重试能力
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(cb, action));

        try {
            return decorated.get();
        } catch (CallNotPermittedException ex) {
            // 熔断器打开，直接拒绝调用
            log.warn("CircuitBreaker [{}] OPEN - call rejected", instanceName);
            throw new CircuitBreakerOpenException(instanceName);
        } catch (Exception ex) {
            throw new ResilienceExecutionException(instanceName, ex);
        }
    }

    /**
     * 带降级的执行（熔断或异常时执行兜底逻辑）
     *
     * 【面试考点】降级策略的选择：
     *   - 返回默认值（如"暂无数据"）
     *   - 返回缓存数据（过期但可用）
     *   - 调用备用服务（如备用 AI 服务）
     */
    public <T> T executeWithFallback(String instanceName, Supplier<T> action, Supplier<T> fallback) {
        try {
            return execute(instanceName, action);
        } catch (CircuitBreakerOpenException | ResilienceExecutionException ex) {
            log.warn("CircuitBreaker [{}] fallback triggered: {}", instanceName, ex.getMessage());
            return fallback.get();
        }
    }

    /** 查询熔断器当前状态（用于监控面板） */
    public CircuitBreaker.State getCircuitState(String instanceName) {
        return cbRegistry.circuitBreaker(instanceName).getState();
    }

    /** 熔断器打开异常 */
    public static class CircuitBreakerOpenException extends RuntimeException {
        private final String instanceName;
        public CircuitBreakerOpenException(String instanceName) {
            super("Circuit breaker [" + instanceName + "] is OPEN");
            this.instanceName = instanceName;
        }
        public String getInstanceName() { return instanceName; }
    }

    /** 弹性执行异常 */
    public static class ResilienceExecutionException extends RuntimeException {
        private final String instanceName;
        public ResilienceExecutionException(String instanceName, Throwable cause) {
            super("Resilience execution failed for [" + instanceName + "]", cause);
            this.instanceName = instanceName;
        }
        public String getInstanceName() { return instanceName; }
    }
}