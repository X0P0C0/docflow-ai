package com.docflow.ai.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WebhookConfigResponse {
    private Long id;
    private String name;
    private String url;
    private String events;
    private Integer status;
    private LocalDateTime createTime;
}
