package com.docflow.ai.ai.dto;

import lombok.Data;

@Data
public class AiWorkspaceOverview {

    private int pendingSuggestions;

    private int adoptedSuggestions;

    private int knowledgeRecommendations;
}
