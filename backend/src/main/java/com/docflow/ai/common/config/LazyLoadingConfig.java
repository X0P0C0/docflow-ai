package com.docflow.ai.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 延迟加载配置 —— 按需加载 Bean
 * <p>
 * 面试考点：
 * <ul>
 *   <li>饿加载 vs 懒加载的区别</li>
 *   <li>懒加载的优点：启动快、节省内存</li>
 *   <li>懒加载的缺点：首次请求慢</li>
 *   <li>适用场景：非核心功能、低频使用的服务</li>
 * </ul>
 * <p>
 * 配置方式：
 * <pre>
 *   spring:
 *     main:
 *       lazy-initialization: true  # 全局懒加载
 * </pre>
 * <p>
 * 或者单个 Bean：
 * <pre>
 *   @Lazy
 *   @Component
 *   public class HeavyService { ... }
 * </pre>
 */
@Slf4j
@Configuration
public class LazyLoadingConfig {

    // 全局懒加载可通过 application.yml 配置
    // spring.main.lazy-initialization: true

    // 排除核心 Bean 的懒加载
    // spring.main.lazy-initialization.exclude:
    //   - DataSource
    //   - RedisTemplate
}
