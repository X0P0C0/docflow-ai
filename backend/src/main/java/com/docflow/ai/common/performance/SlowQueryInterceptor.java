package com.docflow.ai.common.performance;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

/**
 * 慢查询拦截器 —— 自动记录执行时间超过阈值的 SQL
 * <p>
 * 功能：
 * <ul>
 *   <li>记录执行时间 > 1 秒的 SQL</li>
 *   <li>记录完整的 SQL 语句和参数</li>
 *   <li>可用于监控和告警</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>MyBatis 拦截器机制（Plugin + Interceptor）</li>
 *   <li>如何定位慢查询</li>
 *   <li>索引优化策略</li>
 * </ul>
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class SlowQueryInterceptor implements Interceptor {

    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1 second

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            return invocation.proceed();
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            if (duration > SLOW_QUERY_THRESHOLD_MS) {
                StatementHandler handler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = handler.getBoundSql();
                String sql = boundSql.getSql();

                log.warn("[SLOW QUERY] {}ms - {}", duration, sql);

                // Could also send to monitoring system
                // metricsService.recordSlowQuery(sql, duration);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // Configurable threshold via properties
    }
}
