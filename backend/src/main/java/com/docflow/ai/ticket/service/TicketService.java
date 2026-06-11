package com.docflow.ai.ticket.service;

import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.ticket.dto.AddTicketCommentRequest;
import com.docflow.ai.ticket.dto.AssignTicketRequest;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.dto.CreateTicketRequest;
import com.docflow.ai.ticket.dto.TicketDetailResponse;
import com.docflow.ai.ticket.dto.TicketAssigneeOptionResponse;
import com.docflow.ai.ticket.dto.TicketListItemResponse;
import com.docflow.ai.ticket.dto.TicketStatsResponse;
import com.docflow.ai.ticket.dto.TicketQueryRequest;
import com.docflow.ai.ticket.dto.UpdateTicketStatusRequest;

import com.docflow.ai.ticket.dto.TransferTicketRequest;
import com.docflow.ai.ticket.dto.EscalateTicketRequest;
import com.docflow.ai.ticket.dto.MergeTicketRequest;
import com.docflow.ai.ticket.dto.LinkTicketRequest;
import com.docflow.ai.ticket.dto.SatisfactionRequest;
import com.docflow.ai.ticket.dto.BatchOperationResponse;
import com.docflow.ai.ticket.dto.BatchStatusRequest;
import com.docflow.ai.ticket.dto.BatchAssignRequest;
import java.util.List;

public interface TicketService {

    List<TicketListItemResponse> listTickets(Long userId, TicketQueryRequest request);

    TicketDetailResponse getTicketById(Long id, Long userId);

    List<TicketAssigneeOptionResponse> listAssignableUsers(Long userId);

    TicketDetailResponse createTicket(Long userId, CreateTicketRequest request);

    TicketDetailResponse addComment(Long ticketId, Long userId, AddTicketCommentRequest request);

    TicketDetailResponse updateStatus(Long ticketId, Long userId, UpdateTicketStatusRequest request);

    TicketDetailResponse assignTicket(Long ticketId, Long userId, AssignTicketRequest request);

    TicketStatsResponse getTicketStats(Long userId);


    // ===== Ticket lifecycle operations =====
    TicketDetailResponse transferTicket(Long ticketId, Long userId, TransferTicketRequest request);
    TicketDetailResponse escalateTicket(Long ticketId, Long userId, EscalateTicketRequest request);
    TicketDetailResponse mergeTicket(Long ticketId, Long userId, MergeTicketRequest request);
    void linkTicket(Long ticketId, Long userId, LinkTicketRequest request);
    TicketDetailResponse rateSatisfaction(Long ticketId, Long userId, SatisfactionRequest request);

    // ===== Batch operations =====
    BatchOperationResponse batchUpdateStatus(Long userId, BatchStatusRequest request);
    BatchOperationResponse batchAssign(Long userId, BatchAssignRequest request);
    String exportTickets(Long userId, TicketQueryRequest request);

    KnowledgeArticleResponse createKnowledgeDraft(Long ticketId, Long userId, CreateTicketKnowledgeDraftRequest request);
}
