package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.ticket.dto.AddTicketCommentRequest;
import com.docflow.ai.ticket.dto.AssignTicketRequest;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.dto.CreateTicketRequest;
import com.docflow.ai.ticket.dto.TicketDetailResponse;
import com.docflow.ai.ticket.dto.TicketAssigneeOptionResponse;
import com.docflow.ai.ticket.dto.TicketListItemResponse;
import com.docflow.ai.ticket.dto.TicketQueryRequest;
import com.docflow.ai.ticket.dto.UpdateTicketStatusRequest;
import com.docflow.ai.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ApiResponse<List<TicketListItemResponse>> listTickets(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                                 TicketQueryRequest request) {
        // controller 只透传当前登录用户和筛选条件，列表权限收口在 service 内部。
        return ApiResponse.success(ticketService.listTickets(principal.getUserId(), request));
    }

    @PostMapping
    public ApiResponse<TicketDetailResponse> createTicket(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody CreateTicketRequest request) {
        // 创建后直接返回详情态，前端可以少一次“新建后再查详情”的往返。
        return ApiResponse.success(ticketService.createTicket(principal.getUserId(), request));
    }

    @GetMapping("/assignees")
    public ApiResponse<List<TicketAssigneeOptionResponse>> listAssignableUsers(@AuthenticationPrincipal AuthUserPrincipal principal) {
        // 指派人列表单独成接口，避免把整套用户管理数据暴露给工单页。
        return ApiResponse.success(ticketService.listAssignableUsers(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketDetailResponse> getTicket(@PathVariable Long id,
                                                       @AuthenticationPrincipal AuthUserPrincipal principal) {
        // 详情是工单工作台主入口，service 会返回聚合后的完整上下文。
        return ApiResponse.success(ticketService.getTicketById(id, principal.getUserId()));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<TicketDetailResponse> addComment(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody AddTicketCommentRequest request) {
        // 评论成功后返回最新详情，保证前端时间线和评论区立即同步。
        return ApiResponse.success(ticketService.addComment(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<TicketDetailResponse> updateStatus(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody UpdateTicketStatusRequest request) {
        // 状态流转的合法性在 service 中统一校验，controller 不重复写规则。
        return ApiResponse.success(ticketService.updateStatus(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/assignee")
    public ApiResponse<TicketDetailResponse> assignTicket(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody AssignTicketRequest request) {
        // 指派成功后同样返回最新详情，方便前端直接刷新工作台状态。
        return ApiResponse.success(ticketService.assignTicket(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/knowledge-draft")
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<KnowledgeArticleResponse> createKnowledgeDraft(@PathVariable Long id,
                                                                      @AuthenticationPrincipal AuthUserPrincipal principal,
                                                                      @RequestBody(required = false) CreateTicketKnowledgeDraftRequest request) {
        // 这里显式用方法级鉴权，是为了把“谁能把工单沉淀成知识”单独收口出来。
        return ApiResponse.success(ticketService.createKnowledgeDraft(id, principal.getUserId(), request));
    }
}
