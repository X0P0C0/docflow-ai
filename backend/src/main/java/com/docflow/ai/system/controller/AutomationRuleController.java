package com.docflow.ai.system.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.system.dto.AutomationRuleResponse;
import com.docflow.ai.system.dto.CreateRuleRequest;
import com.docflow.ai.system.service.AutomationRuleService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/automation-rules")
@RequiredArgsConstructor
@Tag(name = "Automation Rules", description = "Ticket automation rule management")
public class AutomationRuleController {

    private final AutomationRuleService ruleService;

    @GetMapping
    @Timed(value = "rules.read", histogram = true)
    @Operation(summary = "List automation rules")
    public ApiResponse<List<AutomationRuleResponse>> listRules(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(ruleService.listRules(principal.getUserId()));
    }

    @PostMapping
    @Timed(value = "rules.write", histogram = true)
    @AuditLog(module = "AUTOMATION", action = "CREATE")
    @Operation(summary = "Create automation rule")
    public ApiResponse<AutomationRuleResponse> createRule(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody CreateRuleRequest request) {
        return ApiResponse.success(ruleService.createRule(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    @Timed(value = "rules.write", histogram = true)
    @AuditLog(module = "AUTOMATION", action = "UPDATE")
    @Operation(summary = "Update automation rule")
    public ApiResponse<AutomationRuleResponse> updateRule(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody CreateRuleRequest request) {
        return ApiResponse.success(ruleService.updateRule(id, principal.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "rules.write", histogram = true)
    @AuditLog(module = "AUTOMATION", action = "DELETE")
    @Operation(summary = "Delete automation rule")
    public ApiResponse<Void> deleteRule(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthUserPrincipal principal) {
        ruleService.deleteRule(id, principal.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/{id}/toggle")
    @Timed(value = "rules.write", histogram = true)
    @AuditLog(module = "AUTOMATION", action = "TOGGLE")
    @Operation(summary = "Toggle automation rule")
    public ApiResponse<Void> toggleRule(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                        @RequestParam Integer status) {
        ruleService.toggleRule(id, principal.getUserId(), status);
        return ApiResponse.success();
    }
}
