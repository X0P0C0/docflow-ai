package com.docflow.ai.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 读写分离配置（演示）
 * <p>
 * 读写分离原理：
 * <ul>
 *   <li>主库（Master）处理写操作</li>
 *   <li>从库（Slave）处理读操作</li>
 *   <li>主从复制保证数据一致性</li>
 * </ul>
 * <p>
 * 实现方式：
 * <ul>
 *   <li>中间件：MyCat、ShardingSphere</li>
 *   <li>应用层：AbstractRoutingDataSource</li>
 *   <li>注解切换：@ReadDataSource / @WriteDataSource</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>主从延迟如何处理？—— 强制走主库、延迟检测</li>
 *   <li>事务中的读操作走哪里？—— 必须走主库</li>
 *   <li>如何判断是读操作？—— 方法名规则、注解</li>
 * </ul>
 * <p>
 * 注意：本项目使用单库，此配置仅作为学习演示
 */
@Slf4j
@Configuration
public class ReadWriteSplittingConfig {

    /**
     * 数据源路由 Key
     */
    public static final String MASTER = "master";
    public static final String SLAVE = "slave";

    /**
     * 使用 ThreadLocal 存储当前数据源类型
     * <p>
     * 为什么用 ThreadLocal？
     * <ul>
     *   <li>每个请求在独立线程处理</li>
     *   <li>线程内共享数据源选择</li>
     *   <li>请求结束后自动清理</li>
     * </ul>
     */
    private static final ThreadLocal<String> DATASOURCE_HOLDER = new ThreadLocal<>();

    public static void useMaster() {
        DATASOURCE_HOLDER.set(MASTER);
    }

    public static void useSlave() {
        DATASOURCE_HOLDER.set(SLAVE);
    }

    public static String getCurrentDataSource() {
        return DATASOURCE_HOLDER.get();
    }

    public static void clear() {
        DATASOURCE_HOLDER.remove();
    }

    /**
     * 读数据源注解
     */
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface ReadDataSource {
    }

    /**
     * 写数据源注解
     */
    @java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface WriteDataSource {
    }
}
