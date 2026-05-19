package com.docflow.ai.integration;

import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.support.AbstractContainerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InfrastructureContainerIntegrationTest extends AbstractContainerIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldConnectToMysqlAndRedisContainers() {
        List<String> supportRoles = sysUserMapper.selectRoleCodesByUserId(2L);

        assertThat(supportRoles).contains("SUPPORT");
        assertThat(sysUserMapper.selectById(1L)).isNotNull();

        stringRedisTemplate.opsForValue().set("it:infra:ping", "pong");
        assertThat(stringRedisTemplate.opsForValue().get("it:infra:ping")).isEqualTo("pong");
    }
}
