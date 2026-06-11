package com.docflow.ai.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    private Integer readFlag;
    private LocalDateTime createTime;
    private LocalDateTime readTime;
    /** 发送人显示名称 */
    private String senderName;
}