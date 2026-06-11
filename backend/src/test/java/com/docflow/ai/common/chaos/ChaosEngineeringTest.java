package com.docflow.ai.common.chaos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("混沌工程模拟器测试")
class ChaosEngineeringTest {

    private final ChaosEngineering chaos = new ChaosEngineering();

    @Test
    @DisplayName("禁用时不注入异常")
    void shouldNotInjectWhenDisabled() {
        chaos.setEnabled(false);
        chaos.injectException(); // 不应抛异常
    }

    @Test
    @DisplayName("禁用时不注入延迟")
    void shouldNotInjectLatencyWhenDisabled() {
        chaos.setEnabled(false);
        long start = System.currentTimeMillis();
        chaos.injectLatency();
        long duration = System.currentTimeMillis() - start;

        assertThat(duration).isLessThan(100);
    }

    @Test
    @DisplayName("禁用时服务应可用")
    void shouldNotBeUnavailableWhenDisabled() {
        chaos.setEnabled(false);
        assertThat(chaos.isServiceUnavailable()).isFalse();
    }

    @Test
    @DisplayName("100% 失败率应总是注入异常")
    void shouldAlwaysInjectAtFullRate() {
        chaos.setEnabled(true);
        chaos.setFailureRate(1.0);

        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> chaos.injectException())
                    .isInstanceOf(ChaosEngineering.ChaosException.class);
        }
    }

    @Test
    @DisplayName("0% 失败率应从不注入异常")
    void shouldNeverInjectAtZeroRate() {
        chaos.setEnabled(true);
        chaos.setFailureRate(0.0);

        for (int i = 0; i < 100; i++) {
            chaos.injectException(); // 不应抛异常
        }
    }
}
