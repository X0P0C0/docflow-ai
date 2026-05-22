package com.docflow.ai.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return Health.up()
                        .withDetail("数据库类型", "MySQL")
                        .withDetail("连接地址", conn.getMetaData().getURL())
                        .build();
            }
            return Health.down().withDetail("error", "数据库连接验证失败").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("错误详情", e.getMessage())
                    .build();
        }
    }
}