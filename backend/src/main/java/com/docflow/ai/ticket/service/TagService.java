package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.CreateTagRequest;
import com.docflow.ai.ticket.dto.TagResponse;
import java.util.List;

public interface TagService {
    List<TagResponse> listTags(Long userId);
    TagResponse createTag(Long userId, CreateTagRequest request);
    void deleteTag(Long id, Long userId);
    void addTagToTicket(Long ticketId, Long tagId);
    void removeTagFromTicket(Long ticketId, Long tagId);
    List<TagResponse> getTicketTags(Long ticketId);
}
