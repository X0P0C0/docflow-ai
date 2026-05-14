package com.docflow.ai.ticket.dto;

import lombok.Data;

@Data
public class TicketQueryRequest {

    private String keyword;

    private Integer status;

    private Integer priority;

    private String type;

    private Long assigneeUserId;
}
