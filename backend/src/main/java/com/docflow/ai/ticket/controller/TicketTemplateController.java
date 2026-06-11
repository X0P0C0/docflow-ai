package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.ticket.dto.CreateTemplateRequest;
import com.docflow.ai.ticket.dto.TemplateResponse;
import com.docflow.ai.ticket.service.TicketTemplateService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-templates")
@RequiredArgsConstructor
@Tag(name = "Ticket Templates", description = "CRUD for ticket templates")
public class TicketTemplateController {

    private final TicketTemplateService templateService;

    @GetMapping
    @Timed(value = "templates.read", histogram = true)
    @Operation(summary = "List all templates")
    public ApiResponse<List<TemplateResponse>> listTemplates(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(templateService.listTemplates(principal.getUserId()));
    }

    @GetMapping("/{id}")
    @Timed(value = "templates.read", histogram = true)
    @Operation(summary = "Get template detail")
    public ApiResponse<TemplateResponse> getTemplate(@PathVariable Long id) {
        return ApiResponse.success(templateService.getTemplate(id));
    }

    @PostMapping
    @Timed(value = "templates.write", histogram = true)
    @AuditLog(module = "TEMPLATE", action = "CREATE")
    @Operation(summary = "Create template")
    public ApiResponse<TemplateResponse> createTemplate(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.success(templateService.createTemplate(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    @Timed(value = "templates.write", histogram = true)
    @AuditLog(module = "TEMPLATE", action = "UPDATE")
    @Operation(summary = "Update template")
    public ApiResponse<TemplateResponse> updateTemplate(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.success(templateService.updateTemplate(id, principal.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "templates.write", histogram = true)
    @AuditLog(module = "TEMPLATE", action = "DELETE")
    @Operation(summary = "Delete template")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthUserPrincipal principal) {
        templateService.deleteTemplate(id, principal.getUserId());
        return ApiResponse.success();
    }
}
