package com.docflow.ai.common.pattern.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Saga 编排器测试")
class SagaOrchestratorTest {

    private final SagaOrchestrator orchestrator = new SagaOrchestrator();

    @Test
    @DisplayName("所有步骤成功应返回成功")
    void shouldSucceedWhenAllStepsPass() {
        AtomicInteger counter = new AtomicInteger(0);

        List<SagaOrchestrator.SagaStep> steps = List.of(
                createStep("Step1", true, counter),
                createStep("Step2", true, counter),
                createStep("Step3", true, counter)
        );

        var result = orchestrator.execute("test-saga", steps);

        assertThat(result.success()).isTrue();
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("步骤失败应执行补偿操作")
    void shouldCompensateOnFailure() {
        AtomicInteger executed = new AtomicInteger(0);
        AtomicInteger compensated = new AtomicInteger(0);

        List<SagaOrchestrator.SagaStep> steps = List.of(
                createStep("Step1", true, executed, compensated),
                createStep("Step2", true, executed, compensated),
                createStep("Step3", false, executed, compensated)
        );

        var result = orchestrator.execute("test-saga", steps);

        assertThat(result.success()).isFalse();
        assertThat(executed.get()).isEqualTo(3); // 3 步都尝试执行了
        assertThat(compensated.get()).isEqualTo(2); // 前 2 步补偿了
    }

    @Test
    @DisplayName("补偿应逆序执行")
    void shouldCompensateInReverseOrder() {
        var order = new java.util.concurrent.CopyOnWriteArrayList<String>();

        List<SagaOrchestrator.SagaStep> steps = List.of(
                new SagaOrchestrator.SagaStep() {
                    public String getName() { return "A"; }
                    public boolean execute() { order.add("exec-A"); return true; }
                    public void compensate() { order.add("comp-A"); }
                },
                new SagaOrchestrator.SagaStep() {
                    public String getName() { return "B"; }
                    public boolean execute() { order.add("exec-B"); return true; }
                    public void compensate() { order.add("comp-B"); }
                },
                new SagaOrchestrator.SagaStep() {
                    public String getName() { return "C"; }
                    public boolean execute() { order.add("exec-C"); return false; }
                    public void compensate() { order.add("comp-C"); }
                }
        );

        orchestrator.execute("test-saga", steps);

        assertThat(order).containsExactly("exec-A", "exec-B", "exec-C", "comp-B", "comp-A");
    }

    private SagaOrchestrator.SagaStep createStep(String name, boolean succeed, AtomicInteger counter) {
        return new SagaOrchestrator.SagaStep() {
            public String getName() { return name; }
            public boolean execute() { counter.incrementAndGet(); return succeed; }
            public void compensate() { counter.decrementAndGet(); }
        };
    }

    private SagaOrchestrator.SagaStep createStep(String name, boolean succeed,
                                                   AtomicInteger executed, AtomicInteger compensated) {
        return new SagaOrchestrator.SagaStep() {
            public String getName() { return name; }
            public boolean execute() { executed.incrementAndGet(); return succeed; }
            public void compensate() { compensated.incrementAndGet(); }
        };
    }
}
