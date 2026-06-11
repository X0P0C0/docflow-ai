package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomFieldResponse {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String options;
    private String defaultValue;
    private Integer required;
    private Integer sortOrder;
    private Long categoryId;
    private Integer status;
    private LocalDateTime createTime;
}
