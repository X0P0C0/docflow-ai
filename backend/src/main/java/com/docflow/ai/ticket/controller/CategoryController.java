package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.ticket.dto.CategoryResponse;
import com.docflow.ai.ticket.dto.CreateCategoryRequest;
import com.docflow.ai.ticket.service.CategoryService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Ticket category management")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Timed(value = "categories.read", histogram = true)
    @Operation(summary = "List categories (tree)")
    public ApiResponse<List<CategoryResponse>> listCategories(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(categoryService.listCategories(principal.getUserId()));
    }

    @PostMapping
    @Timed(value = "categories.write", histogram = true)
    @AuditLog(module = "CATEGORY", action = "CREATE")
    @Operation(summary = "Create category")
    public ApiResponse<CategoryResponse> createCategory(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(categoryService.createCategory(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    @Timed(value = "categories.write", histogram = true)
    @AuditLog(module = "CATEGORY", action = "UPDATE")
    @Operation(summary = "Update category")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success(categoryService.updateCategory(id, principal.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "categories.write", histogram = true)
    @AuditLog(module = "CATEGORY", action = "DELETE")
    @Operation(summary = "Delete category")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id,
                                            @AuthenticationPrincipal AuthUserPrincipal principal) {
        categoryService.deleteCategory(id, principal.getUserId());
        return ApiResponse.success();
    }
}
