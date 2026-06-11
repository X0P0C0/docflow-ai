package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemplateResponse {
    private Long id;
    private String name;
    private String titleTemplate;
    private String contentTemplate;
    private String type;
    private Integer priority;
    private Long categoryId;
    private Integer isPublic;
    private Long createBy;
    private String creatorName;
    private LocalDateTime createTime;
}
