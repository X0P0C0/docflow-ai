package com.docflow.ai.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeArticleVersionResponse {

    private Long id;

    private Integer versionNo;

    private String title;

    private String summary;

    private String remark;

    private Long operatorUserId;

    private LocalDateTime createTime;
}
