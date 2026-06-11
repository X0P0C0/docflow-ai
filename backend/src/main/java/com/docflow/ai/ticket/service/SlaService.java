package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.SlaPolicyRequest;
import com.docflow.ai.ticket.dto.SlaStatusResponse;
import com.docflow.ai.ticket.entity.SlaPolicy;
import java.util.List;

public interface SlaService {
    List<SlaPolicy> listPolicies();
    SlaPolicy createPolicy(Long userId, SlaPolicyRequest request);
    SlaPolicy updatePolicy(Long id, Long userId, SlaPolicyRequest request);
    void deletePolicy(Long id, Long userId);
    void calculateDeadlines(Long ticketId, String ticketType, Integer priority);
    SlaStatusResponse getSlaStatus(Long ticketId);
    List<SlaStatusResponse> getBreachedTickets(Long userId);
}
