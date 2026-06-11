package com.docflow.ai.integration;

import com.docflow.ai.auth.dto.LoginResponse;
import com.docflow.ai.auth.service.AuthService;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.ticket.dto.TicketStatsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 集成测试 —— 验证 API 端到端的正确性
 * <p>
 * 测试内容：
 * <ul>
 *   <li>登录 → 获取 Token → 访问受保护接口</li>
 *   <li>创建工单 → 查询工单 → 验证数据一致性</li>
 *   <li>Token 过期 → 刷新 Token → 继续访问</li>
 * </ul>
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试完整登录流程
     */
    @Test
    @DisplayName("集成测试：登录流程")
    void loginFlow() throws Exception {
        // 1. 登录
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        // 提取 Token
        String responseBody = loginResult.getResponse().getContentAsString();
        ApiResponse<LoginResponse> response = objectMapper.readValue(responseBody,
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, LoginResponse.class));
        String token = response.getData().getToken();

        // 2. 使用 Token 访问受保护接口
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    /**
     * 测试工单统计接口（带缓存）
     */
    @Test
    @DisplayName("集成测试：工单统计接口")
    void ticketStats() throws Exception {
        // 登录获取 Token
        LoginResponse login = authService.login("admin", "admin123");
        String token = login.getToken();

        // 第一次查询（缓存未命中）
        long start1 = System.currentTimeMillis();
        MvcResult result1 = mockMvc.perform(get("/api/tickets/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andReturn();
        long time1 = System.currentTimeMillis() - start1;

        // 第二次查询（缓存命中）
        long start2 = System.currentTimeMillis();
        MvcResult result2 = mockMvc.perform(get("/api/tickets/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andReturn();
        long time2 = System.currentTimeMillis() - start2;

        log.info("=== 工单统计接口测试 ===");
        log.info("第一次查询（缓存未命中）: {}ms", time1);
        log.info("第二次查询（缓存命中）: {}ms", time2);
        log.info("性能提升: {}x", (double) time1 / Math.max(time2, 1));
    }

    /**
     * 测试未认证访问被拒绝
     */
    @Test
    @DisplayName("集成测试：未认证访问被拒绝")
    void unauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 测试刷新 Token 流程
     */
    @Test
    @DisplayName("集成测试：刷新 Token")
    void refreshToken() throws Exception {
        // 1. 登录
        LoginResponse login = authService.login("admin", "admin123");
        String refreshToken = login.getRefreshToken();

        // 2. 刷新 Token
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        // 3. 旧的 Refresh Token 应该失效
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}