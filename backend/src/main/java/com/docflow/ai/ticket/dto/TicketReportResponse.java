package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.util.List;

@Data
public class TicketReportResponse {
    private String reportType; // DAILY or WEEKLY
    private String period; // e.g., "2026-05-29" or "2026-05-23 ~ 2026-05-29"
    private long createdCount;
    private long resolvedCount;
    private long closedCount;
    private long breachedCount;
    private double avgResolutionHours;
    private double satisfactionAvg;
    private List<TopAssignee> topAssignees;
    private List<CategoryBreakdown> categoryBreakdown;
    private String summary; // AI-generated summary text

    @Data
    public static class TopAssignee {
        private Long userId;
        private String name;
        private long resolvedCount;
        private double avgResolutionHours;
    }

    @Data
    public static class CategoryBreakdown {
        private String category;
        private long count;
    }
}
