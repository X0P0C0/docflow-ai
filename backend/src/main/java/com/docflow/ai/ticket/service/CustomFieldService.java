package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.CreateCustomFieldRequest;
import com.docflow.ai.ticket.dto.CustomFieldResponse;
import com.docflow.ai.ticket.dto.CustomFieldValueRequest;
import java.util.List;
import java.util.Map;

public interface CustomFieldService {
    List<CustomFieldResponse> listFields(Long userId, Long categoryId);
    CustomFieldResponse createField(Long userId, CreateCustomFieldRequest request);
    CustomFieldResponse updateField(Long id, Long userId, CreateCustomFieldRequest request);
    void deleteField(Long id, Long userId);
    void saveFieldValues(Long ticketId, Long userId, List<CustomFieldValueRequest> values);
    Map<Long, String> getFieldValues(Long ticketId);
}
