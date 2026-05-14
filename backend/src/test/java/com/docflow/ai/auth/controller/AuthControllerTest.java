package com.docflow.ai.auth.controller;

import com.docflow.ai.auth.dto.CurrentUserResponse;
import com.docflow.ai.auth.dto.LoginResponse;
import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.auth.service.AuthService;
import com.docflow.ai.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void loginShouldReturnUnifiedSuccessResponse() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setToken("test-token");
        response.setExpireSeconds(7200L);

        when(authService.login(eq("admin"), eq("123456"))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", "admin",
                                "password", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.error").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").value("test-token"));
    }

    @Test
    void currentUserShouldReturnCurrentUserProfile() throws Exception {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(1L);
        response.setUsername("admin");
        response.setRoles(List.of("ADMIN"));
        response.setPermissions(List.of("ticket:manage"));

        when(authService.getCurrentUser(1L)).thenReturn(response);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUserPrincipal(1L, "admin"), null, List.of())
        );

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }
}
