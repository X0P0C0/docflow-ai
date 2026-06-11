package com.docflow.ai.common.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 连接池优化配置 —— HikariCP 调优
 * <p>
 * HikariCP 是目前最快的 Java 连接池
 * <p>
 * 关键参数：
 * <ul>
 *   <li>maximumPoolSize: 最大连接数（默认 10，推荐 CPU核心数 * 2 + 磁盘数）</li>
 *   <li>minimumIdle: 最小空闲连接数</li>
 *   <li>connectionTimeout: 获取连接超时时间（默认 30s）</li>
 *   <li>idleTimeout: 空闲连接存活时间（默认 10min）</li>
 *   <li>maxLifetime: 连接最大存活时间（默认 30min）</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>为什么 HikariCP 比 C3P0 快？—— 字节码优化 + 并发容器</li>
 *   <li>连接池大小如何设置？—— 不是越大越好</li>
 *   <li>连接泄漏检测：leakDetectionThreshold</li>
 * </ul>
 * <p>
 * 配置示例（application.yml）：
 * <pre>
 *   spring:
 *     datasource:
 *       hikari:
 *         maximum-pool-size: 20
 *         minimum-idle: 5
 *         connection-timeout: 30000
 *         idle-timeout: 600000
 *         max-lifetime: 1800000
 *         leak-detection-threshold: 60000
 * </pre>
 */
@Slf4j
@Configuration
public class ConnectionPoolConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        // 优化参数
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);

        // 连接泄漏检测（开发环境建议开启）
        dataSource.setLeakDetectionThreshold(60000);

        // 连接测试
        dataSource.setConnectionTestQuery("SELECT 1");

        log.info("HikariCP configured: maxPoolSize={}, minIdle={}",
                dataSource.getMaximumPoolSize(), dataSource.getMinimumIdle());

        return dataSource;
    }
}
