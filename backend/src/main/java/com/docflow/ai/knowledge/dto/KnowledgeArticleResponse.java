package com.docflow.ai.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeArticleResponse {

    private Long id;

    private String title;

    private String summary;

    private String content;

    private Long categoryId;

    private Long sourceTicketId;

    private KnowledgeArticleSourceTicketResponse sourceTicket;

    private Long authorUserId;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<KnowledgeArticleVersionResponse> versions;
}
