package com.docflow.ai.ticket.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TagResponse {
    private Long id;
    private String name;
    private String color;
    private Long usageCount;
    private LocalDateTime createTime;
}
