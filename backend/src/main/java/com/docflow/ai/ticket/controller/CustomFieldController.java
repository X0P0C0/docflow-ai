package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.ticket.dto.CreateCustomFieldRequest;
import com.docflow.ai.ticket.dto.CustomFieldResponse;
import com.docflow.ai.ticket.dto.CustomFieldValueRequest;
import com.docflow.ai.ticket.service.CustomFieldService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/custom-fields")
@RequiredArgsConstructor
@Tag(name = "Custom Fields", description = "Custom field definitions and values")
public class CustomFieldController {

    private final CustomFieldService fieldService;

    @GetMapping
    @Timed(value = "custom-fields.read", histogram = true)
    @Operation(summary = "List custom fields")
    public ApiResponse<List<CustomFieldResponse>> listFields(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                             @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(fieldService.listFields(principal.getUserId(), categoryId));
    }

    @PostMapping
    @Timed(value = "custom-fields.write", histogram = true)
    @AuditLog(module = "CUSTOM_FIELD", action = "CREATE")
    @Operation(summary = "Create custom field")
    public ApiResponse<CustomFieldResponse> createField(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody CreateCustomFieldRequest request) {
        return ApiResponse.success(fieldService.createField(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    @Timed(value = "custom-fields.write", histogram = true)
    @AuditLog(module = "CUSTOM_FIELD", action = "UPDATE")
    @Operation(summary = "Update custom field")
    public ApiResponse<CustomFieldResponse> updateField(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody CreateCustomFieldRequest request) {
        return ApiResponse.success(fieldService.updateField(id, principal.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "custom-fields.write", histogram = true)
    @AuditLog(module = "CUSTOM_FIELD", action = "DELETE")
    @Operation(summary = "Delete custom field")
    public ApiResponse<Void> deleteField(@PathVariable Long id,
                                         @AuthenticationPrincipal AuthUserPrincipal principal) {
        fieldService.deleteField(id, principal.getUserId());
        return ApiResponse.success();
    }

    @GetMapping("/values/{ticketId}")
    @Timed(value = "custom-fields.read", histogram = true)
    @Operation(summary = "Get custom field values for a ticket")
    public ApiResponse<Map<Long, String>> getFieldValues(@PathVariable Long ticketId) {
        return ApiResponse.success(fieldService.getFieldValues(ticketId));
    }

    @PostMapping("/values/{ticketId}")
    @Timed(value = "custom-fields.write", histogram = true)
    @AuditLog(module = "CUSTOM_FIELD", action = "SAVE_VALUES")
    @Operation(summary = "Save custom field values for a ticket")
    public ApiResponse<Void> saveFieldValues(@PathVariable Long ticketId,
                                             @AuthenticationPrincipal AuthUserPrincipal principal,
                                             @RequestBody List<CustomFieldValueRequest> values) {
        fieldService.saveFieldValues(ticketId, principal.getUserId(), values);
        return ApiResponse.success();
    }
}
