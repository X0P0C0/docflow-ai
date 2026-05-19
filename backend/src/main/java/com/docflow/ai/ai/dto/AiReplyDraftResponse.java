package com.docflow.ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiReplyDraftResponse {

    private Long ticketId;

    private String statusKey;

    private boolean adopted;

    private Long adoptedByUserId;

    private String adoptedByName;

    private LocalDateTime adoptedAt;

    private LocalDateTime lastActivityAt;

    private String claimFreshness;

    private String ticketNo;

    private String ticketTitle;

    private String scene;

    private String confidence;

    private String opener;

    private String diagnosis;

    private String nextStep;

    private String customerReply;

    private List<String> operatorNotes;

    private List<AiKnowledgeRecommendation> relatedKnowledge;
}
