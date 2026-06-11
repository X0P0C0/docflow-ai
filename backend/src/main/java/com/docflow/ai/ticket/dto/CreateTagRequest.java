package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTagRequest {
    @NotBlank(message = "Tag name is required")
    private String name;
    private String color;
}
