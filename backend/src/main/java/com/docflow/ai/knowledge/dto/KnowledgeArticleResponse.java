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

    // sourceTicketId / sourceTicket 用来保留“这篇知识从哪张工单沉淀而来”的业务来路。
    private Long sourceTicketId;

    private KnowledgeArticleSourceTicketResponse sourceTicket;

    private Long authorUserId;

    // status 代表当前文章所处状态；主表保存当前态，版本表保存历史态。
    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // versions 只在详情场景下返回，方便前端直接做版本回看与恢复。
    private List<KnowledgeArticleVersionResponse> versions;
}
