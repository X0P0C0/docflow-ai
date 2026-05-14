package com.docflow.ai.knowledge.dto;

import lombok.Data;

@Data
public class KnowledgeArticleQuery {

    private String keyword;

    private Long categoryId;

    private Integer status;

    private Long sourceTicketId;

    private String sourceTicketNo;
}
