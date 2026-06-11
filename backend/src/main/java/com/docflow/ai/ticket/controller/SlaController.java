package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.ticket.dto.SlaPolicyRequest;
import com.docflow.ai.ticket.dto.SlaStatusResponse;
import com.docflow.ai.ticket.entity.SlaPolicy;
import com.docflow.ai.ticket.service.SlaService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sla")
@RequiredArgsConstructor
@Tag(name = "SLA Management", description = "SLA policy CRUD and breach monitoring")
public class SlaController {

    private final SlaService slaService;

    @GetMapping("/policies")
    @Timed(value = "sla.read", histogram = true)
    @Operation(summary = "List all SLA policies")
    public ApiResponse<List<SlaPolicy>> listPolicies() {
        return ApiResponse.success(slaService.listPolicies());
    }

    @PostMapping("/policies")
    @Timed(value = "sla.write", histogram = true)
    @AuditLog(module = "SLA", action = "CREATE_POLICY")
    @Operation(summary = "Create SLA policy")
    public ApiResponse<SlaPolicy> createPolicy(@AuthenticationPrincipal AuthUserPrincipal principal,
                                               @Valid @RequestBody SlaPolicyRequest request) {
        return ApiResponse.success(slaService.createPolicy(principal.getUserId(), request));
    }

    @PutMapping("/policies/{id}")
    @Timed(value = "sla.write", histogram = true)
    @AuditLog(module = "SLA", action = "UPDATE_POLICY")
    @Operation(summary = "Update SLA policy")
    public ApiResponse<SlaPolicy> updatePolicy(@PathVariable Long id,
                                               @AuthenticationPrincipal AuthUserPrincipal principal,
                                               @Valid @RequestBody SlaPolicyRequest request) {
        return ApiResponse.success(slaService.updatePolicy(id, principal.getUserId(), request));
    }

    @DeleteMapping("/policies/{id}")
    @Timed(value = "sla.write", histogram = true)
    @AuditLog(module = "SLA", action = "DELETE_POLICY")
    @Operation(summary = "Delete SLA policy")
    public ApiResponse<Void> deletePolicy(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthUserPrincipal principal) {
        slaService.deletePolicy(id, principal.getUserId());
        return ApiResponse.success();
    }

    @GetMapping("/status/{ticketId}")
    @Timed(value = "sla.read", histogram = true)
    @Operation(summary = "Get SLA status for a ticket")
    public ApiResponse<SlaStatusResponse> getSlaStatus(@PathVariable Long ticketId) {
        return ApiResponse.success(slaService.getSlaStatus(ticketId));
    }

    @GetMapping("/breached")
    @Timed(value = "sla.read", histogram = true)
    @Operation(summary = "Get SLA breached tickets")
    public ApiResponse<List<SlaStatusResponse>> getBreachedTickets(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(slaService.getBreachedTickets(principal.getUserId()));
    }
}
