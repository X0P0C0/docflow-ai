package com.docflow.ai.auth.controller;



import java.util.Map;

import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.auth.dto.CurrentUserResponse;

import java.util.Map;

import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.auth.dto.LoginRequest;

import java.util.Map;

import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.auth.dto.LoginResponse;

import java.util.Map;

import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.auth.security.AuthUserPrincipal;

import java.util.Map;

import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.auth.service.AuthService;

import java.util.Map;

import com.docflow.ai.common.domain.ApiResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/auth")

@RequiredArgsConstructor

@Tag(name = "认证管理", description = "用户登录、登出和令牌管理")
public class AuthController {



    private final AuthService authService;
    private final BusinessMetricsService metricsService;



    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ApiResponse.success(authService.refreshToken(refreshToken));
    }

    @PostMapping("/login")

    @Operation(summary = "用户登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // 登录接口只负责签发会话，不在 controller 层掺杂额外权限判断。

        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));

    }



    @GetMapping("/me")

    public ApiResponse<CurrentUserResponse> currentUser(@AuthenticationPrincipal AuthUserPrincipal principal) {

        // /me 是前端恢复会话和刷新当前用户画像的统一入口。

        return ApiResponse.success(authService.getCurrentUser(principal.getUserId()));

    }

}

