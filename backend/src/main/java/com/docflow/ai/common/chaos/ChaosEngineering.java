package com.docflow.ai.common.chaos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 混沌工程模拟器 —— 故障注入测试
 * <p>
 * 什么是混沌工程？
 * <ul>
 *   <li>通过主动注入故障，验证系统韧性</li>
 *   <li>Netflix Chaos Monkey 是最著名的实现</li>
 *   <li>目标：在生产环境发现潜在问题</li>
 * </ul>
 * <p>
 * 故障类型：
 * <ul>
 *   <li>延迟注入：模拟网络抖动</li>
 *   <li>异常注入：模拟服务不可用</li>
 *   <li>资源耗尽：模拟 CPU/内存压力</li>
 *   <li>数据损坏：模拟数据不一致</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>混沌工程 vs 压力测试 vs 故障演练</li>
 *   <li>爆炸半径控制：限制故障影响范围</li>
 *   <li>自动化混沌实验</li>
 * </ul>
 */
@Slf4j
@Component
public class ChaosEngineering {

    private boolean enabled = false;
    private double failureRate = 0.1; // 10% 失败率
    private long maxLatencyMs = 5000; // 最大延迟 5 秒

    /**
     * 启用/禁用混沌实验
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Chaos engineering {}", enabled ? "enabled" : "disabled");
    }

    /**
     * 注入随机延迟
     */
    public void injectLatency() {
        if (!enabled) return;

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            long latency = ThreadLocalRandom.current().nextLong(1000, maxLatencyMs);
            log.warn("Chaos: injecting {}ms latency", latency);
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 注入随机异常
     */
    public void injectException() {
        if (!enabled) return;

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            log.warn("Chaos: injecting exception");
            throw new ChaosException("混沌工程注入的异常");
        }
    }

    /**
     * 模拟服务不可用
     */
    public boolean isServiceUnavailable() {
        if (!enabled) return false;

        boolean unavailable = ThreadLocalRandom.current().nextDouble() < failureRate;
        if (unavailable) {
            log.warn("Chaos: simulating service unavailability");
        }
        return unavailable;
    }

    public void setFailureRate(double rate) {
        this.failureRate = rate;
    }

    public static class ChaosException extends RuntimeException {
        public ChaosException(String message) {
            super(message);
        }
    }
}
