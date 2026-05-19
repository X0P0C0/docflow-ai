package com.docflow.ai.ai.service;

import com.docflow.ai.ai.dto.AiWorkspaceResponse;
import com.docflow.ai.ai.dto.AiReplyDraftResponse;
import com.docflow.ai.ai.dto.AiWorkspaceAdoptionResponse;

public interface AiWorkspaceService {

    AiWorkspaceResponse getWorkspace(Long userId);

    AiReplyDraftResponse getReplyDraft(Long userId, Long ticketId);

    AiWorkspaceAdoptionResponse markReplyDraftAdopted(Long userId, Long ticketId);

    AiWorkspaceAdoptionResponse unmarkReplyDraftAdopted(Long userId, Long ticketId);
}
