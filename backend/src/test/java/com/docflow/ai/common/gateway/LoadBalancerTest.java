package com.docflow.ai.common.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("负载均衡器测试")
class LoadBalancerTest {

    private final LoadBalancer lb = new LoadBalancer();

    @Test
    @DisplayName("轮询应依次分配")
    void shouldRoundRobin() {
        List<String> instances = List.of("A", "B", "C");

        String first = lb.roundRobin(instances);
        String second = lb.roundRobin(instances);
        String third = lb.roundRobin(instances);
        String fourth = lb.roundRobin(instances);

        assertThat(first).isEqualTo("A");
        assertThat(second).isEqualTo("B");
        assertThat(third).isEqualTo("C");
        assertThat(fourth).isEqualTo("A"); // 循环
    }

    @Test
    @DisplayName("随机应返回有效实例")
    void shouldReturnValidRandomInstance() {
        List<String> instances = List.of("A", "B", "C");

        for (int i = 0; i < 100; i++) {
            String result = lb.random(instances);
            assertThat(instances).contains(result);
        }
    }

    @Test
    @DisplayName("空列表应返回 null")
    void shouldReturnNullForEmptyList() {
        String nullResult1 = lb.roundRobin(List.of());
        assertThat(nullResult1).isNull();
        String nullResult2 = lb.random(List.of());
        assertThat(nullResult2).isNull();
    }

    @Test
    @DisplayName("一致性哈希应返回确定性结果")
    void shouldReturnConsistentResult() {
        List<String> instances = List.of("A", "B", "C");

        String result1 = lb.consistentHash(instances, "user-123");
        String result2 = lb.consistentHash(instances, "user-123");

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("加权轮询应按权重分配")
    void shouldWeightedRoundRobin() {
        List<String> instances = List.of("A", "B");
        List<Integer> weights = List.of(3, 1);

        int countA = 0, countB = 0;
        for (int i = 0; i < 4; i++) {
            String result = lb.weightedRoundRobin(instances, weights);
            if ("A".equals(result)) countA++;
            else countB++;
        }

        assertThat(countA).isEqualTo(3);
        assertThat(countB).isEqualTo(1);
    }
}
