package com.docflow.ai.common.pattern.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Saga 编排器 - 分布式事务解决方案
 *
 * 【设计模式】Saga 模式（编排式 Orchestration）
 * 【面试考点】（分布式事务是面试重灾区）
 *   - Saga vs 2PC（两阶段提交）：
 *     2PC 强一致但性能差、有协调者单点问题
 *     Saga 最终一致但性能好、适合微服务
 *   - 编排式(Orchestration) vs 协同式(Choreography)：
 *     编排式：由中央协调器统一调度（本项目使用）
 *     协同式：各服务通过事件自发协调
 *   - 补偿操作必须是幂等的（重复执行结果一致）
 *   - Saga 的隔离性问题：中间状态对外可见（脏读）
 *
 * 【执行流程】
 *   成功路径：T1 -> T2 -> T3 -> T4（全部成功）
 *   失败路径：T1 -> T2 -> T3(失败) -> C2 -> C1（逆序补偿回滚）
 *
 * 【真实业务场景】
 *   工单升级流程（涉及多个服务）：
 *   T1: 创建升级记录 -> T2: 通知主管 -> T3: 更新工单状态 -> T4: 记录审计日志
 *   如果 T3 失败：C2(撤回通知) -> C1(删除升级记录)
 *
 * 【与 Spring 事务的关系】
 *   Saga 跨越多个本地事务，每个步骤是一个独立事务
 *   Spring @Transactional 只管单个数据库操作
 *   Saga 管的是"业务事务"的最终一致性
 */
@Slf4j
@Component
public class SagaOrchestrator {

    /**
     * 执行 Saga 事务
     *
     * @param sagaId 事务唯一标识（用于日志追踪和排查）
     * @param steps  事务步骤列表（按顺序执行）
     * @return 执行结果
     */
    public SagaResult execute(String sagaId, List<SagaStep> steps) {
        log.info("Starting saga: {}", sagaId);

        /** 已完成的步骤（用于失败时逆序补偿） */
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

    /**
     * 逆序执行补偿操作
     *
     * 【面试考点】为什么要逆序？
     *   - 补偿的语义是"撤销"，应该按相反顺序执行
     *   - 例如：先创建了记录，后发了通知 -> 先撤通知，再删记录
     *   - 补偿失败需要人工介入（记录日志，报警）
     */
    private void compensate(String sagaId, List<SagaStep> completedSteps) {
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            try {
                log.info("Saga {} - Compensating step: {}", sagaId, step.getName());
                step.compensate();
            } catch (Exception e) {
                log.error("Saga {} - Compensation failed for step: {}", sagaId, step.getName(), e);
                // 补偿失败需要人工介入（报警 + 人工处理）
            }
        }
    }

    /**
     * Saga 步骤接口
     * 【设计要点】每个步骤必须实现 execute（正向操作）和 compensate（补偿操作）
     */
    public interface SagaStep {
        String getName();
        boolean execute();      // 正向操作
        void compensate();      // 补偿操作（必须幂等）
    }

    /**
     * Saga 执行结果（Java Record，不可变对象）
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