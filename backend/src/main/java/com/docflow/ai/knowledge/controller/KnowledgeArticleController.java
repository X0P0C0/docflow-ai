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


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micrometer.core.annotation.Timed;

import org.springframework.web.bind.annotation.RestController;





import java.util.List;





@RestController


@RequestMapping("/api/knowledge/articles")


@RequiredArgsConstructor


@Tag(name = "知识库管理", description = "知识文章的创建、查询、编辑、版本管理和删除")
public class KnowledgeArticleController {





    private final KnowledgeArticleService knowledgeArticleService;





    @GetMapping

    @Timed(value = "knowledge.read", histogram = true)

    public ApiResponse<List<KnowledgeArticleResponse>> listArticles(


            @RequestParam(required = false) String keyword,


            @RequestParam(required = false) Long categoryId,


            @RequestParam(required = false) Integer status,


            @RequestParam(required = false) Long sourceTicketId,


            @RequestParam(required = false) String sourceTicketNo) {


        // 这里把零散 query 参数整理成 Query DTO，便于 service 层持续扩展筛选条件。


        KnowledgeArticleQuery query = new KnowledgeArticleQuery();


        query.setKeyword(keyword);


        query.setCategoryId(categoryId);


        query.setStatus(status);


        query.setSourceTicketId(sourceTicketId);


        query.setSourceTicketNo(sourceTicketNo);


        return ApiResponse.success(knowledgeArticleService.listArticles(query));


    }





    @GetMapping("/source-ticket-counts")

    @Timed(value = "knowledge.read", histogram = true)

    public ApiResponse<java.util.Map<Long, Long>> countBySourceTickets(@RequestParam List<Long> ticketIds) {


        // 这个接口主要服务列表页“批量显示每张工单沉淀了多少篇文章”的场景。


        return ApiResponse.success(knowledgeArticleService.countArticlesBySourceTickets(ticketIds));


    }





    @GetMapping("/{id}")

    @Timed(value = "knowledge.read", histogram = true)

    public ApiResponse<KnowledgeArticleResponse> getArticle(@PathVariable Long id) {


        // 文章详情默认允许已登录用户查看，管理动作再单独加能力校验。


        return ApiResponse.success(knowledgeArticleService.getArticleById(id));


    }





    @PostMapping


    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")


    public ApiResponse<KnowledgeArticleResponse> createArticle(@AuthenticationPrincipal AuthUserPrincipal principal,


                                                               @Valid @RequestBody CreateKnowledgeArticleRequest request) {


        // 写操作统一挂在 canManageKnowledge 之下，避免 controller 各自维护分散规则。


        return ApiResponse.success(knowledgeArticleService.createArticle(principal.getUserId(), request));


    }





    @PutMapping("/{id}")


    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")


    @Operation(summary = "更新知识文章")
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


        // 恢复版本本质上仍然是一次写操作，所以沿用同一套知识管理权限。


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



    @PostMapping("/{id}/rate")
    @Timed(value = "knowledge.write", histogram = true)
    @Operation(summary = "Rate a knowledge article (1-5)")
    public ApiResponse<Void> rateArticle(@PathVariable Long id,
                                         @RequestParam int score) {
        knowledgeArticleService.rateArticle(id, score);
        return ApiResponse.success();
    }

    @GetMapping("/recommend")
    @Timed(value = "knowledge.read", histogram = true)
    @Operation(summary = "Recommend articles by keyword similarity")
    public ApiResponse<List<KnowledgeArticleResponse>> recommend(@RequestParam String keyword,
                                                                  @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(knowledgeArticleService.recommendByKeyword(keyword, limit));
    }
}
