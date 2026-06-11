package com.docflow.ai.common.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务发现模拟 —— 微服务注册与发现
 * <p>
 * 服务发现原理：
 * <pre>
 *   服务A → 注册中心 ← 服务B
 *              ↑
 *           服务C
 * </pre>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>Eureka vs Nacos vs Consul vs ZooKeeper</li>
 *   <li>AP（可用性）vs CP（一致性）选择</li>
 *   <li>心跳检测、健康检查</li>
 *   <li>服务上下线通知</li>
 * </ul>
 */
@Slf4j
@Component
public class ServiceDiscovery {

    // 服务注册表：serviceName -> instances
    private final Map<String, List<ServiceInstance>> registry = new ConcurrentHashMap<>();

    /**
     * 服务注册
     */
    public void register(String serviceName, String host, int port) {
        ServiceInstance instance = new ServiceInstance(serviceName, host, port);

        registry.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
                .add(instance);

        log.info("Service registered: {} at {}:{}", serviceName, host, port);
    }

    /**
     * 服务注销
     */
    public void deregister(String serviceName, String host, int port) {
        List<ServiceInstance> instances = registry.get(serviceName);
        if (instances != null) {
            instances.removeIf(i -> i.host().equals(host) && i.port() == port);
            if (instances.isEmpty()) {
                registry.remove(serviceName);
            }
            log.info("Service deregistered: {} at {}:{}", serviceName, host, port);
        }
    }

    /**
     * 发示服务实例
     */
    public List<ServiceInstance> discover(String serviceName) {
        return registry.getOrDefault(serviceName, List.of());
    }

    /**
     * 获取所有服务名称
     */
    public List<String> getAllServices() {
        return List.copyOf(registry.keySet());
    }

    /**
     * 服务实例
     */
    public record ServiceInstance(
            String serviceName,
            String host,
            int port
    ) {
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
    }
}
