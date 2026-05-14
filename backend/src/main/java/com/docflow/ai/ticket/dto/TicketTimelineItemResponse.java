package com.docflow.ai.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketTimelineItemResponse {

    private Long id;

    private String operatorName;

    private String title;

    private String desc;

    private LocalDateTime createTime;
}
