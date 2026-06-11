package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTemplateRequest {
    @NotBlank(message = "Template name is required")
    private String name;
    private String titleTemplate;
    private String contentTemplate;
    private String type;
    private Integer priority;
    private Long categoryId;
    private Integer isPublic;
}
