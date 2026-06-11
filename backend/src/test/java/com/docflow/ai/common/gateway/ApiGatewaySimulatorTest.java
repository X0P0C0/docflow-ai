package com.docflow.ai.common.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("API 网关模拟器测试")
class ApiGatewaySimulatorTest {

    private final ApiGatewaySimulator gateway = new ApiGatewaySimulator();

    @Test
    @DisplayName("注册服务后应能路由到该服务")
    void shouldRouteToRegisteredService() {
        gateway.registerService("user-service", "localhost", 8081);
        gateway.addRoute("/api/users", "user-service");

        var instance = gateway.route("/api/users");

        assertThat(instance).isNotNull();
        assertThat(instance.serviceName()).isEqualTo("user-service");
        assertThat(instance.getUrl()).isEqualTo("http://localhost:8081");
    }

    @Test
    @DisplayName("未注册的路由应返回 null")
    void shouldReturnNullForUnknownRoute() {
        var instance = gateway.route("/api/unknown");
        assertThat(instance).isNull();
    }

    @Test
    @DisplayName("前缀匹配应生效")
    void shouldMatchByPrefix() {
        gateway.registerService("order-service", "localhost", 8082);
        gateway.addRoute("/api/orders", "order-service");

        var instance = gateway.route("/api/orders/123");

        assertThat(instance).isNotNull();
        assertThat(instance.serviceName()).isEqualTo("order-service");
    }
}
