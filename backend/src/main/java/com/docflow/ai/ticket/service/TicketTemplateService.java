package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.CreateTemplateRequest;
import com.docflow.ai.ticket.dto.TemplateResponse;
import java.util.List;

public interface TicketTemplateService {
    List<TemplateResponse> listTemplates(Long userId);
    TemplateResponse getTemplate(Long id);
    TemplateResponse createTemplate(Long userId, CreateTemplateRequest request);
    TemplateResponse updateTemplate(Long id, Long userId, CreateTemplateRequest request);
    void deleteTemplate(Long id, Long userId);
}
