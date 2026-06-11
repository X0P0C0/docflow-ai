package com.docflow.ai.common.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 网关模拟 —— 统一入口、路由、限流、鉴权
 * <p>
 * API 网关核心功能：
 * <ul>
 *   <li>路由转发：将请求转发到对应的微服务</li>
 *   <li>负载均衡：轮询、加权轮询、随机</li>
 *   <li>限流熔断：保护后端服务</li>
 *   <li>认证鉴权：统一身份验证</li>
 *   <li>日志监控：请求链路追踪</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>API 网关 vs 服务网格（Service Mesh）</li>
 *   <li>常见网关：Nginx、Kong、Spring Cloud Gateway、Zuul</li>
 *   <li>BFF（Backend For Frontend）模式</li>
 * </ul>
 */
@Slf4j
@Component
public class ApiGatewaySimulator {

    // 服务注册表
    private final Map<String, ServiceInstance> serviceRegistry = new ConcurrentHashMap<>();

    // 路由表
    private final Map<String, String> routeTable = new ConcurrentHashMap<>();

    /**
     * 注册服务实例
     */
    public void registerService(String serviceName, String host, int port) {
        String instanceId = serviceName + ":" + host + ":" + port;
        serviceRegistry.put(instanceId, new ServiceInstance(serviceName, host, port, true));
        log.info("Registered service: {}", instanceId);
    }

    /**
     * 配置路由规则
     */
    public void addRoute(String path, String serviceName) {
        routeTable.put(path, serviceName);
        log.info("Added route: {} -> {}", path, serviceName);
    }

    /**
     * 路由请求
     * @param requestPath 请求路径
     * @return 目标服务实例
     */
    public ServiceInstance route(String requestPath) {
        // 匹配路由规则
        String serviceName = matchRoute(requestPath);
        if (serviceName == null) {
            log.warn("No route found for path: {}", requestPath);
            return null;
        }

        // 负载均衡选择实例
        return selectInstance(serviceName);
    }

    private String matchRoute(String path) {
        // 精确匹配
        if (routeTable.containsKey(path)) {
            return routeTable.get(path);
        }

        // 前缀匹配
        for (Map.Entry<String, String> entry : routeTable.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    private ServiceInstance selectInstance(String serviceName) {
        // 简单轮询（实际应使用 Round Robin）
        return serviceRegistry.values().stream()
                .filter(i -> i.serviceName().equals(serviceName) && i.healthy())
                .findFirst()
                .orElse(null);
    }

    /**
     * 服务实例
     */
    public record ServiceInstance(
            String serviceName,
            String host,
            int port,
            boolean healthy
    ) {
        public String getUrl() {
            return "http://" + host + ":" + port;
        }
    }
}
