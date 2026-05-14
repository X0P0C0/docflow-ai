package com.docflow.ai.knowledge.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.knowledge.dto.CreateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.dto.KnowledgeArticleQuery;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.knowledge.dto.UpdateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/articles")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    @GetMapping
    public ApiResponse<List<KnowledgeArticleResponse>> listArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long sourceTicketId,
            @RequestParam(required = false) String sourceTicketNo) {
        KnowledgeArticleQuery query = new KnowledgeArticleQuery();
        query.setKeyword(keyword);
        query.setCategoryId(categoryId);
        query.setStatus(status);
        query.setSourceTicketId(sourceTicketId);
        query.setSourceTicketNo(sourceTicketNo);
        return ApiResponse.success(knowledgeArticleService.listArticles(query));
    }

    @GetMapping("/source-ticket-counts")
    public ApiResponse<java.util.Map<Long, Long>> countBySourceTickets(@RequestParam List<Long> ticketIds) {
        return ApiResponse.success(knowledgeArticleService.countArticlesBySourceTickets(ticketIds));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeArticleResponse> getArticle(@PathVariable Long id) {
        return ApiResponse.success(knowledgeArticleService.getArticleById(id));
    }

    @PostMapping
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<KnowledgeArticleResponse> createArticle(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                               @Valid @RequestBody CreateKnowledgeArticleRequest request) {
        return ApiResponse.success(knowledgeArticleService.createArticle(principal.getUserId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<KnowledgeArticleResponse> updateArticle(@PathVariable Long id,
                                                               @AuthenticationPrincipal AuthUserPrincipal principal,
                                                               @Valid @RequestBody UpdateKnowledgeArticleRequest request) {
        return ApiResponse.success(knowledgeArticleService.updateArticle(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<KnowledgeArticleResponse> restoreArticleVersion(@PathVariable Long id,
                                                                       @PathVariable Long versionId,
                                                                       @AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(knowledgeArticleService.restoreArticleVersion(id, versionId, principal.getUserId()));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<KnowledgeArticleResponse> archiveArticle(@PathVariable Long id,
                                                                @AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(knowledgeArticleService.archiveArticle(id, principal.getUserId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthUserPrincipal principal) {
        knowledgeArticleService.deleteArticle(id, principal.getUserId());
        return ApiResponse.success();
    }
}
