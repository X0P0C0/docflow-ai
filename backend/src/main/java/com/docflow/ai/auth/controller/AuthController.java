package com.docflow.ai.auth.controller;

import com.docflow.ai.auth.dto.CurrentUserResponse;
import com.docflow.ai.auth.dto.LoginRequest;
import com.docflow.ai.auth.dto.LoginResponse;
import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.auth.service.AuthService;
import com.docflow.ai.common.domain.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(authService.getCurrentUser(principal.getUserId()));
    }
}
