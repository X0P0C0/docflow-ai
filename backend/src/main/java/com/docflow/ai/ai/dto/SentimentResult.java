package com.docflow.ai.ai.dto;

import lombok.Data;

@Data
public class SentimentResult {
    private String sentiment;
    private double score;
    private String emotion;
    private String urgency;
    private String summary;
}
