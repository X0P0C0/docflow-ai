package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SatisfactionStatsResponse {
    private long totalRated;
    private double averageScore;
    private Map<Integer, Long> distribution;
    private List<AssigneeSatisfaction> byAssignee;

    @Data
    public static class AssigneeSatisfaction {
        private Long assigneeId;
        private String assigneeName;
        private long ratedCount;
        private double avgScore;
    }
}
