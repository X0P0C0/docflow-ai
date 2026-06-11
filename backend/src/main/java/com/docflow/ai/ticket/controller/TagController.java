package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.ticket.dto.CreateTagRequest;
import com.docflow.ai.ticket.dto.TagResponse;
import com.docflow.ai.ticket.service.TagService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Ticket tag management")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Timed(value = "tags.read", histogram = true)
    @Operation(summary = "List all tags")
    public ApiResponse<List<TagResponse>> listTags(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(tagService.listTags(principal.getUserId()));
    }

    @PostMapping
    @Timed(value = "tags.write", histogram = true)
    @AuditLog(module = "TAG", action = "CREATE")
    @Operation(summary = "Create tag")
    public ApiResponse<TagResponse> createTag(@AuthenticationPrincipal AuthUserPrincipal principal,
                                              @Valid @RequestBody CreateTagRequest request) {
        return ApiResponse.success(tagService.createTag(principal.getUserId(), request));
    }

    @DeleteMapping("/{id}")
    @Timed(value = "tags.write", histogram = true)
    @AuditLog(module = "TAG", action = "DELETE")
    @Operation(summary = "Delete tag")
    public ApiResponse<Void> deleteTag(@PathVariable Long id,
                                       @AuthenticationPrincipal AuthUserPrincipal principal) {
        tagService.deleteTag(id, principal.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/ticket/{ticketId}/tag/{tagId}")
    @Timed(value = "tags.write", histogram = true)
    @Operation(summary = "Add tag to ticket")
    public ApiResponse<Void> addTag(@PathVariable Long ticketId, @PathVariable Long tagId) {
        tagService.addTagToTicket(ticketId, tagId);
        return ApiResponse.success();
    }

    @DeleteMapping("/ticket/{ticketId}/tag/{tagId}")
    @Timed(value = "tags.write", histogram = true)
    @Operation(summary = "Remove tag from ticket")
    public ApiResponse<Void> removeTag(@PathVariable Long ticketId, @PathVariable Long tagId) {
        tagService.removeTagFromTicket(ticketId, tagId);
        return ApiResponse.success();
    }

    @GetMapping("/ticket/{ticketId}")
    @Timed(value = "tags.read", histogram = true)
    @Operation(summary = "Get tags for a ticket")
    public ApiResponse<List<TagResponse>> getTicketTags(@PathVariable Long ticketId) {
        return ApiResponse.success(tagService.getTicketTags(ticketId));
    }
}
