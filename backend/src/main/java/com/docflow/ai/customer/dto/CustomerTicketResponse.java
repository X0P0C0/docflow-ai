package com.docflow.ai.customer.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerTicketResponse {
    private Long id;
    private String ticketNo;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private String priorityLabel;
    private Integer status;
    private String statusLabel;
    private String assigneeName;
    private Integer satisfactionScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<TimelineItem> timeline;

    @Data
    public static class TimelineItem {
        private String actionType;
        private String operatorName;
        private String remark;
        private String time;
    }
}
