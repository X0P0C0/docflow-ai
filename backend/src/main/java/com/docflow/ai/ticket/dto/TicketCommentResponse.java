package com.docflow.ai.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketCommentResponse {

    private Long id;

    private String authorName;

    private String content;

    private String commentTypeLabel;

    private Boolean internal;

    private LocalDateTime createTime;
}
