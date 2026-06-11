package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private List<CategoryResponse> children;
    private LocalDateTime createTime;
}
