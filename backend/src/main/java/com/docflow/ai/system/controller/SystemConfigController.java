package com.docflow.ai.system.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.system.dto.SysConfigResponse;
import com.docflow.ai.system.dto.UpdateConfigRequest;
import com.docflow.ai.system.service.SystemConfigService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/configs")
@RequiredArgsConstructor
@Tag(name = "System Config", description = "System configuration management")
public class SystemConfigController {

    private final SystemConfigService configService;

    @GetMapping
    @Timed(value = "config.read", histogram = true)
    @Operation(summary = "List configs")
    public ApiResponse<List<SysConfigResponse>> listConfigs(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                            @RequestParam(required = false) String type) {
        return ApiResponse.success(configService.listConfigs(principal.getUserId(), type));
    }

    @PutMapping
    @Timed(value = "config.write", histogram = true)
    @AuditLog(module = "CONFIG", action = "UPDATE")
    @Operation(summary = "Update config")
    public ApiResponse<SysConfigResponse> updateConfig(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                       @Valid @RequestBody UpdateConfigRequest request) {
        return ApiResponse.success(configService.updateConfig(principal.getUserId(), request));
    }
}
