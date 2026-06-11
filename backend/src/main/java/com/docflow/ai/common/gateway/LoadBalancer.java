package com.docflow.ai.common.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡策略实现
 * <p>
 * 常见负载均衡算法：
 * <ol>
 *   <li>轮询（Round Robin）：依次分配请求</li>
 *   <li>加权轮询（Weighted Round Robin）：按权重分配</li>
 *   <li>随机（Random）：随机选择</li>
 *   <li>最少连接（Least Connections）：选择连接数最少的</li>
 *   <li>一致性哈希（Consistent Hashing）：相同请求总是到同一节点</li>
 * </ol>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>四层负载均衡 vs 七层负载均衡</li>
 *   <li>Nginx vs LVS vs HAProxy</li>
 *   <li>客户端负载均衡 vs 服务端负载均衡</li>
 * </ul>
 */
@Slf4j
@Component
public class LoadBalancer {

    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    /**
     * 轮询策略
     */
    public <T> T roundRobin(List<T> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int index = roundRobinIndex.getAndIncrement() % instances.size();
        return instances.get(Math.abs(index));
    }

    /**
     * 随机策略
     */
    public <T> T random(List<T> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return instances.get(index);
    }

    /**
     * 加权轮询策略
     * @param instances 实例列表
     * @param weights 对应权重
     */
    public <T> T weightedRoundRobin(List<T> instances, List<Integer> weights) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
        int index = roundRobinIndex.getAndIncrement() % totalWeight;

        int cumulative = 0;
        for (int i = 0; i < instances.size(); i++) {
            cumulative += weights.get(i);
            if (index < cumulative) {
                return instances.get(i);
            }
        }

        return instances.get(0);
    }

    /**
     * 一致性哈希策略
     */
    public <T> T consistentHash(List<T> instances, String key) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        int hash = key.hashCode();
        int index = Math.abs(hash) % instances.size();
        return instances.get(index);
    }
}
