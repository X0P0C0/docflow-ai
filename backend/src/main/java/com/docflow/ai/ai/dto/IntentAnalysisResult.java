package com.docflow.ai.ai.dto;

import lombok.Data;

@Data
public class IntentAnalysisResult {
    private String ticketType;
    private Integer priority;
    private Long categoryId;
    private String categoryName;
    private double confidence;
    private String reason;
}
