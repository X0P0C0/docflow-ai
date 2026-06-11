package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.ticket.dto.DashboardResponse;
import com.docflow.ai.ticket.service.DashboardService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard analytics")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Timed(value = "dashboard.read", histogram = true)
    @Operation(summary = "Get full dashboard data")
    public ApiResponse<DashboardResponse> getDashboard(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(dashboardService.getDashboard(principal.getUserId()));
    }
}
