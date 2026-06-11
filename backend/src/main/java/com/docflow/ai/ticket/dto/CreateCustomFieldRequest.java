package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCustomFieldRequest {
    @NotBlank(message = "Field name is required")
    private String fieldName;
    @NotBlank(message = "Field label is required")
    private String fieldLabel;
    @NotBlank(message = "Field type is required")
    private String fieldType;
    private String options;
    private String defaultValue;
    private Integer required;
    private Integer sortOrder;
    private Long categoryId;
}
