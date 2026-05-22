package com.docflow.ai.ai.controller;

import com.docflow.ai.ai.dto.AiReplyDraftResponse;
import com.docflow.ai.ai.dto.AiWorkspaceAdoptionResponse;
import com.docflow.ai.ai.dto.AiWorkspaceResponse;
import com.docflow.ai.ai.service.AiWorkspaceService;
import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micrometer.core.annotation.Timed;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/workspace")
@RequiredArgsConstructor
@Tag(name = "AI 工作区", description = "AI 辅助工单回复、知识草稿生成和采纳")
public class AiWorkspaceController {

    private final AiWorkspaceService aiWorkspaceService;

    @GetMapping
    @PreAuthorize("@userAccessService.canAccessAiCenter(#principal.userId)")
    public ApiResponse<AiWorkspaceResponse> getWorkspace(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(aiWorkspaceService.getWorkspace(principal.getUserId()));
    }

    @GetMapping("/reply-drafts/{ticketId}")
    @PreAuthorize("@userAccessService.canAccessAiCenter(#principal.userId)")
    public ApiResponse<AiReplyDraftResponse> getReplyDraft(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                           @PathVariable Long ticketId) {
        return ApiResponse.success(aiWorkspaceService.getReplyDraft(principal.getUserId(), ticketId));
    }

    @PostMapping("/reply-drafts/{ticketId}/adopt")
    @PreAuthorize("@userAccessService.canAccessAiCenter(#principal.userId)")
    public ApiResponse<AiWorkspaceAdoptionResponse> markReplyDraftAdopted(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                                          @PathVariable Long ticketId) {
        return ApiResponse.success(aiWorkspaceService.markReplyDraftAdopted(principal.getUserId(), ticketId));
    }

    @DeleteMapping("/reply-drafts/{ticketId}/adopt")
    @PreAuthorize("@userAccessService.canAccessAiCenter(#principal.userId)")
    public ApiResponse<AiWorkspaceAdoptionResponse> unmarkReplyDraftAdopted(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                                            @PathVariable Long ticketId) {
        return ApiResponse.success(aiWorkspaceService.unmarkReplyDraftAdopted(principal.getUserId(), ticketId));
    }
}
