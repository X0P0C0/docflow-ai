package com.docflow.ai.ticket.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketListItemResponse {

    private Long id;

    private String ticketNo;

    private String title;

    private String content;

    private String type;

    private Long categoryId;

    private Integer priority;

    private String priorityLabel;

    private Integer status;

    private String statusLabel;

    private Long submitUserId;

    private Long assigneeUserId;

    private String submitterName;

    private String assigneeName;

    private Long linkedKnowledgeArticleCount;

    private TicketLinkedKnowledgeArticleResponse latestLinkedKnowledgeArticle;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
