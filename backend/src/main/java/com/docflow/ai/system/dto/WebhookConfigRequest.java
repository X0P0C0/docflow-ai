package com.docflow.ai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebhookConfigRequest {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "URL is required")
    private String url;
    private String secret;
    @NotBlank(message = "Events are required")
    private String events;
    private Integer status;
}
