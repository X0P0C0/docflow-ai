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
        return ApiResponse.success(ticketService.listTickets(principal.getUserId(), request));
    }

    @PostMapping
    public ApiResponse<TicketDetailResponse> createTicket(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody CreateTicketRequest request) {
        return ApiResponse.success(ticketService.createTicket(principal.getUserId(), request));
    }

    @GetMapping("/assignees")
    public ApiResponse<List<TicketAssigneeOptionResponse>> listAssignableUsers(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(ticketService.listAssignableUsers(principal.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<TicketDetailResponse> getTicket(@PathVariable Long id,
                                                       @AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(ticketService.getTicketById(id, principal.getUserId()));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<TicketDetailResponse> addComment(@PathVariable Long id,
                                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                                        @Valid @RequestBody AddTicketCommentRequest request) {
        return ApiResponse.success(ticketService.addComment(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<TicketDetailResponse> updateStatus(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ApiResponse.success(ticketService.updateStatus(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/assignee")
    public ApiResponse<TicketDetailResponse> assignTicket(@PathVariable Long id,
                                                          @AuthenticationPrincipal AuthUserPrincipal principal,
                                                          @Valid @RequestBody AssignTicketRequest request) {
        return ApiResponse.success(ticketService.assignTicket(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/knowledge-draft")
    @PreAuthorize("@userAccessService.canManageKnowledge(#principal.userId)")
    public ApiResponse<KnowledgeArticleResponse> createKnowledgeDraft(@PathVariable Long id,
                                                                      @AuthenticationPrincipal AuthUserPrincipal principal,
                                                                      @RequestBody(required = false) CreateTicketKnowledgeDraftRequest request) {
        return ApiResponse.success(ticketService.createKnowledgeDraft(id, principal.getUserId(), request));
    }
}
