package com.docflow.ai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRuleRequest {
    @NotBlank(message = "Rule name is required")
    private String name;
    private String description;
    @NotBlank(message = "Trigger type is required")
    private String triggerType;
    @NotBlank(message = "Conditions are required")
    private String conditions;
    @NotBlank(message = "Actions are required")
    private String actions;
    private Integer priority;
    private Integer status;
}
