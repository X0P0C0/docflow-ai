package com.docflow.ai.system.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.system.dto.WebhookConfigRequest;
import com.docflow.ai.system.dto.WebhookConfigResponse;
import com.docflow.ai.system.service.WebhookService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Webhook configuration and management")
public class WebhookController {

    private final WebhookService webhookService;

    @GetMapping
    @Timed(value = "webhooks.read", histogram = true)
    @Operation(summary = "List webhooks")
    public ApiResponse<List<WebhookConfigResponse>> listWebhooks(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(webhookService.listWebhooks(principal.getUserId()));
    }

    @PostMapping
    @Timed(value = "webhooks.write", histogram = true)
    @AuditLog(module = "WEBHOOK", action = "CREATE")
    @Operation(summary = "Create webhook")
    public ApiResponse<WebhookConfigResponse> createWebhook(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                             @Valid @RequestBody WebhookConfigRequest request) {
        return ApiResponse.success(webhookService.createWebhook(principal.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "webhooks.write", histogram = true)
    @AuditLog(module = "WEBHOOK", action = "DELETE")
    @Operation(summary = "Delete webhook")
    public ApiResponse<Void> deleteWebhook(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthUserPrincipal principal) {
        webhookService.deleteWebhook(id, principal.getUserId());
        return ApiResponse.success();
    }
}
