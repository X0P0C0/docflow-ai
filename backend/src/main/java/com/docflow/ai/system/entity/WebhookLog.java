package com.docflow.ai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("webhook_log")
public class WebhookLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long webhookId;
    private String event;
    private String payload;
    private Integer responseCode;
    private String responseBody;
    private Integer status;
    private LocalDateTime createTime;
}
