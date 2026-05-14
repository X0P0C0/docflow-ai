package com.docflow.ai.ticket.service;

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

import java.util.List;

public interface TicketService {

    List<TicketListItemResponse> listTickets(Long userId, TicketQueryRequest request);

    TicketDetailResponse getTicketById(Long id, Long userId);

    List<TicketAssigneeOptionResponse> listAssignableUsers(Long userId);

    TicketDetailResponse createTicket(Long userId, CreateTicketRequest request);

    TicketDetailResponse addComment(Long ticketId, Long userId, AddTicketCommentRequest request);

    TicketDetailResponse updateStatus(Long ticketId, Long userId, UpdateTicketStatusRequest request);

    TicketDetailResponse assignTicket(Long ticketId, Long userId, AssignTicketRequest request);

    KnowledgeArticleResponse createKnowledgeDraft(Long ticketId, Long userId, CreateTicketKnowledgeDraftRequest request);
}
