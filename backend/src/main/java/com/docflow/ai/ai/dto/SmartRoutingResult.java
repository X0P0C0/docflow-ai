package com.docflow.ai.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class SmartRoutingResult {
    private Long recommendedAssigneeId;
    private String recommendedAssigneeName;
    private String reason;
    private double confidence;
    private List<AlternativeRoute> alternatives;

    @Data
    public static class AlternativeRoute {
        private Long assigneeId;
        private String assigneeName;
        private double score;
        private String reason;
    }
}
