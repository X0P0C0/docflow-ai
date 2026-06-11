package com.docflow.ai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateConfigRequest {
    @NotBlank(message = "Config key is required")
    private String configKey;
    private String configValue;
    private String description;
}
