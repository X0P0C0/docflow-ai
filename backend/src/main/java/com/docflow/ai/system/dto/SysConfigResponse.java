package com.docflow.ai.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysConfigResponse {
    private Long id;
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
