package com.docflow.ai.ai.dto;

import lombok.Data;

@Data
public class KnowledgeRecommendation {
    private Long articleId;
    private String title;
    private String summary;
    private double relevanceScore;
    private String matchReason;
}
