package com.docflow.ai.common.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("动态数据源路由测试")
class DynamicDataSourceTest {

    @AfterEach
    void cleanup() {
        DynamicDataSource.clear();
    }

    @Test
    @DisplayName("默认应使用主库")
    void shouldDefaultToMaster() {
        DynamicDataSource ds = new DynamicDataSource();
        assertThat(ds.determineCurrentLookupKey()).isEqualTo(DynamicDataSource.DataSourceType.MASTER);
    }

    @Test
    @DisplayName("useMaster 应设置主库")
    void shouldUseMaster() {
        DynamicDataSource.useMaster();
        assertThat(DynamicDataSource.getCurrentType()).isEqualTo(DynamicDataSource.DataSourceType.MASTER);
    }

    @Test
    @DisplayName("useSlave 应设置从库")
    void shouldUseSlave() {
        DynamicDataSource.useSlave();
        assertThat(DynamicDataSource.getCurrentType()).isEqualTo(DynamicDataSource.DataSourceType.SLAVE);
    }

    @Test
    @DisplayName("clear 应清除上下文")
    void shouldClearContext() {
        DynamicDataSource.useSlave();
        DynamicDataSource.clear();
        assertThat(DynamicDataSource.getCurrentType()).isNull();
    }

    @Test
    @DisplayName("determineCurrentLookupKey 应返回正确类型")
    void shouldReturnCorrectLookupKey() {
        DynamicDataSource ds = new DynamicDataSource();

        DynamicDataSource.useSlave();
        assertThat(ds.determineCurrentLookupKey()).isEqualTo(DynamicDataSource.DataSourceType.SLAVE);

        DynamicDataSource.useMaster();
        assertThat(ds.determineCurrentLookupKey()).isEqualTo(DynamicDataSource.DataSourceType.MASTER);
    }
}
