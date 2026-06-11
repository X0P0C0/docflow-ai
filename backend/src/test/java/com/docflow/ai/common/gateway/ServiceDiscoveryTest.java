package com.docflow.ai.common.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("服务发现测试")
class ServiceDiscoveryTest {

    private final ServiceDiscovery discovery = new ServiceDiscovery();

    @Test
    @DisplayName("注册服务后应能发现")
    void shouldDiscoverRegisteredService() {
        discovery.register("user-service", "localhost", 8081);

        var instances = discovery.discover("user-service");

        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).serviceName()).isEqualTo("user-service");
    }

    @Test
    @DisplayName("注销服务后应无法发现")
    void shouldNotDiscoverDeregisteredService() {
        discovery.register("user-service", "localhost", 8081);
        discovery.deregister("user-service", "localhost", 8081);

        var instances = discovery.discover("user-service");

        assertThat(instances).isEmpty();
    }

    @Test
    @DisplayName("多实例注册应全部返回")
    void shouldReturnAllInstances() {
        discovery.register("user-service", "host1", 8081);
        discovery.register("user-service", "host2", 8082);

        var instances = discovery.discover("user-service");

        assertThat(instances).hasSize(2);
    }

    @Test
    @DisplayName("获取所有服务名称")
    void shouldGetAllServices() {
        discovery.register("service-a", "localhost", 8081);
        discovery.register("service-b", "localhost", 8082);

        var services = discovery.getAllServices();

        assertThat(services).containsExactlyInAnyOrder("service-a", "service-b");
    }
}
