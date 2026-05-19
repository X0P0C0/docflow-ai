package com.docflow.ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class AiWorkspaceResponse {

    private LocalDateTime generatedAt;

    private boolean heuristicBased;

    private AiWorkspaceOverview overview;

    private AiReplySuggestion primarySuggestion;

    private List<AiKnowledgeRecommendation> recommendations;

    private List<AiFeedItem> feed;

    private List<AiFollowupItem> followups;

    private Set<Long> adoptedTicketIds;
}
