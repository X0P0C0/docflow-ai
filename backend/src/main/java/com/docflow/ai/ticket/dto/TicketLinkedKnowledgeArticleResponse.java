package com.docflow.ai.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketLinkedKnowledgeArticleResponse {

    private Long id;

    private String title;

    private Integer status;

    private String statusLabel;

    private LocalDateTime publishTime;

    private LocalDateTime updateTime;
}
