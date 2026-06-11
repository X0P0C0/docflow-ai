package com.docflow.ai.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AutomationRuleResponse {
    private Long id;
    private String name;
    private String description;
    private String triggerType;
    private String conditions;
    private String actions;
    private Integer priority;
    private Integer status;
    private LocalDateTime createTime;
}
