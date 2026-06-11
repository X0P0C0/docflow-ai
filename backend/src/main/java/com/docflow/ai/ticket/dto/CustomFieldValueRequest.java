package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomFieldValueRequest {
    @NotNull(message = "Field ID is required")
    private Long fieldId;
    private String fieldValue;
}
