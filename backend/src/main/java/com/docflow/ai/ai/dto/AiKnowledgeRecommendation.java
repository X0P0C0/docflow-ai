package com.docflow.ai.ai.dto;

import lombok.Data;

@Data
public class AiKnowledgeRecommendation {

    private Long articleId;

    private String title;

    private String reason;

    private String matchRate;
}
