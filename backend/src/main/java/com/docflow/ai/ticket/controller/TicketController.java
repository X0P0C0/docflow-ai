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
import com.docflow.ai.ticket.dto.TicketStatsResponse;
import com.docflow.ai.ticket.dto.BatchAssignRequest;
import com.docflow.ai.ticket.dto.BatchStatusRequest;
import com.docflow.ai.ticket.dto.BatchOperationResponse;
import com.docflow.ai.ticket.dto.SatisfactionRequest;
import com.docflow.ai.ticket.dto.LinkTicketRequest;
import com.docflow.ai.ticket.dto.MergeTicketRequest;
import com.docflow.ai.ticket.dto.EscalateTicketRequest;
import com.docflow.ai.ticket.dto.TransferTicketRequest;


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


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.micrometer.core.annotation.Timed;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;





import java.util.List;





@RestController


@RequestMapping("/api/tickets")


@RequiredArgsConstructor


@Tag(name = "工单管理", description = "工单的创建、查询、状态流转、评论和指派")
public class TicketController {





    private final TicketService ticketService;





    @GetMapping

    @Timed(value = "tickets.read", histogram = true)

    public ApiResponse<List<TicketListItemResponse>> listTickets(@AuthenticationPrincipal AuthUserPrincipal principal,


                                                                 TicketQueryRequest request) {


        // controller 只透传当前登录用户和筛选条件，列表权限收口在 service 内部。


        return ApiResponse.success(ticketService.listTickets(principal.getUserId(), request));


    }





    @PostMapping

    @Timed(value = "tickets.write", histogram = true)

    public ApiResponse<TicketDetailResponse> createTicket(@AuthenticationPrincipal AuthUserPrincipal principal,


                                                          @Valid @RequestBody CreateTicketRequest request) {


        // 创建后直接返回详情态，前端可以少一次“新建后再查详情”的往返。


        return ApiResponse.success(ticketService.createTicket(principal.getUserId(), request));


    }





    @GetMapping("/assignees")

    @Timed(value = "tickets.read", histogram = true)

    @Operation(summary = "获取可指派人员列表")
    public ApiResponse<List<TicketAssigneeOptionResponse>> listAssignableUsers(@AuthenticationPrincipal AuthUserPrincipal principal) {


        // 指派人列表单独成接口，避免把整套用户管理数据暴露给工单页。


        return ApiResponse.success(ticketService.listAssignableUsers(principal.getUserId()));


    }





    @GetMapping("/{id}")

    @Timed(value = "tickets.read", histogram = true)

    public ApiResponse<TicketDetailResponse> getTicket(@PathVariable Long id,


                                                       @AuthenticationPrincipal AuthUserPrincipal principal) {


        // 详情是工单工作台主入口，service 会返回聚合后的完整上下文。


        return ApiResponse.success(ticketService.getTicketById(id, principal.getUserId()));


    }





    @PostMapping("/{id}/comments")

    @Timed(value = "tickets.write", histogram = true)

    public ApiResponse<TicketDetailResponse> addComment(@PathVariable Long id,


                                                        @AuthenticationPrincipal AuthUserPrincipal principal,


                                                        @Valid @RequestBody AddTicketCommentRequest request) {


        // 评论成功后返回最新详情，保证前端时间线和评论区立即同步。


        return ApiResponse.success(ticketService.addComment(id, principal.getUserId(), request));


    }





    @PostMapping("/{id}/status")

    @Timed(value = "tickets.write", histogram = true)

    public ApiResponse<TicketDetailResponse> updateStatus(@PathVariable Long id,


                                                          @AuthenticationPrincipal AuthUserPrincipal principal,


                                                          @Valid @RequestBody UpdateTicketStatusRequest request) {


        // 状态流转的合法性在 service 中统一校验，controller 不重复写规则。


        return ApiResponse.success(ticketService.updateStatus(id, principal.getUserId(), request));


    }





    @PostMapping("/{id}/assignee")

    @Timed(value = "tickets.write", histogram = true)

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




    @PostMapping("/{id}/transfer")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Transfer ticket to another agent")
    public ApiResponse<TicketDetailResponse> transferTicket(@PathVariable Long id,
                                                            @AuthenticationPrincipal AuthUserPrincipal principal,
                                                            @Valid @RequestBody TransferTicketRequest request) {
        return ApiResponse.success(ticketService.transferTicket(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/escalate")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Escalate ticket priority")
    public ApiResponse<TicketDetailResponse> escalateTicket(@PathVariable Long id,
                                                            @AuthenticationPrincipal AuthUserPrincipal principal,
                                                            @Valid @RequestBody EscalateTicketRequest request) {
        return ApiResponse.success(ticketService.escalateTicket(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/merge")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Merge ticket into another")
    public ApiResponse<TicketDetailResponse> mergeTicket(@PathVariable Long id,
                                                         @AuthenticationPrincipal AuthUserPrincipal principal,
                                                         @Valid @RequestBody MergeTicketRequest request) {
        return ApiResponse.success(ticketService.mergeTicket(id, principal.getUserId(), request));
    }

    @PostMapping("/{id}/link")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Link ticket to another")
    public ApiResponse<Void> linkTicket(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthUserPrincipal principal,
                                        @Valid @RequestBody LinkTicketRequest request) {
        ticketService.linkTicket(id, principal.getUserId(), request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/satisfaction")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Rate ticket satisfaction")
    public ApiResponse<TicketDetailResponse> rateSatisfaction(@PathVariable Long id,
                                                              @AuthenticationPrincipal AuthUserPrincipal principal,
                                                              @Valid @RequestBody SatisfactionRequest request) {
        return ApiResponse.success(ticketService.rateSatisfaction(id, principal.getUserId(), request));
    }

    @PostMapping("/batch/status")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Batch update ticket status")
    public ApiResponse<BatchOperationResponse> batchUpdateStatus(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                                 @Valid @RequestBody BatchStatusRequest request) {
        return ApiResponse.success(ticketService.batchUpdateStatus(principal.getUserId(), request));
    }

    @PostMapping("/batch/assign")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Batch assign tickets")
    public ApiResponse<BatchOperationResponse> batchAssign(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                           @Valid @RequestBody BatchAssignRequest request) {
        return ApiResponse.success(ticketService.batchAssign(principal.getUserId(), request));
    }

    @GetMapping("/export")
    @Timed(value = "tickets.read", histogram = true)
    @Operation(summary = "Export tickets as CSV")
    public ResponseEntity<String> exportTickets(@AuthenticationPrincipal AuthUserPrincipal principal,
                                                TicketQueryRequest request) {
        String csv = ticketService.exportTickets(principal.getUserId(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/stats")
    @Timed(value = "tickets.read", histogram = true)
    @Operation(summary = "Ticket statistics")
    public ApiResponse<TicketStatsResponse> getStats(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(ticketService.getTicketStats(principal.getUserId()));
    }

}