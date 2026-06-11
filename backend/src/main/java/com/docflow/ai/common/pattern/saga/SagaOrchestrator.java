package com.docflow.ai.common.pattern.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Saga 模式 —— 分布式事务解决方案
 * <p>
 * 什么是 Saga？
 * <ul>
 *   <li>将长事务拆分为多个本地事务</li>
 *   <li>每个本地事务有对应的补偿操作</li>
 *   <li>如果某一步失败，执行前面所有步骤的补偿操作</li>
 * </ul>
 * <p>
 * 执行流程：
 * <pre>
 *   T1 → T2 → T3 → T4（成功）
 *   T1 → T2 → T3(失败) → C2 → C1（回滚）
 * </pre>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>Saga vs 2PC（两阶段提交）的区别</li>
 *   <li>编排式（Orchestration）vs 协同式（Choreography）</li>
 *   <li>补偿操作的幂等性</li>
 *   <li>Saga 的隔离性问题</li>
 * </ul>
 */
@Slf4j
@Component
public class SagaOrchestrator {

    /**
     * 执行 Saga 事务
     * @param sagaId 事务 ID
     * @param steps 事务步骤列表
     * @return 执行结果
     */
    public SagaResult execute(String sagaId, List<SagaStep> steps) {
        log.info("Starting saga: {}", sagaId);

        List<SagaStep> completedSteps = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            SagaStep step = steps.get(i);
            log.info("Saga {} - Step {}/{}: {}", sagaId, i + 1, steps.size(), step.getName());

            try {
                boolean success = step.execute();
                if (!success) {
                    log.warn("Saga {} - Step {} failed, starting compensation", sagaId, step.getName());
                    compensate(sagaId, completedSteps);
                    return SagaResult.ofFailure("步骤失败: " + step.getName());
                }
                completedSteps.add(step);
            } catch (Exception e) {
                log.error("Saga {} - Step {} threw exception", sagaId, step.getName(), e);
                compensate(sagaId, completedSteps);
                return SagaResult.ofFailure("步骤异常: " + step.getName());
            }
        }

        log.info("Saga {} completed successfully", sagaId);
        return SagaResult.ofSuccess();
    }

    private void compensate(String sagaId, List<SagaStep> completedSteps) {
        // 逆序执行补偿操作
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            try {
                log.info("Saga {} - Compensating step: {}", sagaId, step.getName());
                step.compensate();
            } catch (Exception e) {
                log.error("Saga {} - Compensation failed for step: {}", sagaId, step.getName(), e);
                // 补偿失败需要人工介入
            }
        }
    }

    /**
     * Saga 步骤接口
     */
    public interface SagaStep {
        String getName();
        boolean execute();
        void compensate();
    }

    /**
     * Saga 执行结果
     */
    public record SagaResult(boolean success, String message) {
        public static SagaResult ofSuccess() {
            return new SagaResult(true, "事务完成");
        }

        public static SagaResult ofFailure(String message) {
            return new SagaResult(false, message);
        }
    }
}
