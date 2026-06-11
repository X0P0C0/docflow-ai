package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardResponse {
    private TicketStatsResponse ticketStats;
    private SatisfactionStatsResponse satisfactionStats;
    private List<StatusTrend> statusTrend;
    private List<TypeDistribution> typeDistribution;
    private List<PriorityDistribution> priorityDistribution;
    private List<RecentActivity> recentActivities;
    private long breachedCount;
    private long unassignedCount;

    @Data
    public static class StatusTrend {
        private String date;
        private long created;
        private long resolved;
        private long closed;
    }

    @Data
    public static class TypeDistribution {
        private String type;
        private long count;
    }

    @Data
    public static class PriorityDistribution {
        private Integer priority;
        private String label;
        private long count;
    }

    @Data
    public static class RecentActivity {
        private Long ticketId;
        private String ticketNo;
        private String title;
        private String actionType;
        private String operatorName;
        private String remark;
        private String time;
    }
}
