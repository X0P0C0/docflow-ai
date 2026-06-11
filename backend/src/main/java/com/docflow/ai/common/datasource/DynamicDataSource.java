package com.docflow.ai.common.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由 —— 读写分离核心
 * <p>
 * 原理：
 * <ul>
 *   <li>写操作 → 主库（Master）</li>
 *   <li>读操作 → 从库（Slave）</li>
 *   <li>通过 ThreadLocal 标记当前操作类型</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>主从复制延迟问题：写入主库后立即读从库可能读到旧数据</li>
 *   <li>解决方案：写后读走主库、强制走主库注解</li>
 *   <li>分库分表中间件：ShardingSphere、MyCat</li>
 * </ul>
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<DataSourceType> CONTEXT = new ThreadLocal<>();

    public enum DataSourceType {
        MASTER, SLAVE
    }

    public static void useMaster() {
        CONTEXT.set(DataSourceType.MASTER);
    }

    public static void useSlave() {
        CONTEXT.set(DataSourceType.SLAVE);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static DataSourceType getCurrentType() {
        return CONTEXT.get();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType type = CONTEXT.get();
        if (type == null) {
            type = DataSourceType.MASTER; // 默认主库
        }
        log.debug("Routing to datasource: {}", type);
        return type;
    }
}
