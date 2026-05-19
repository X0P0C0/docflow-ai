package com.docflow.ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiFollowupItem {

    private Long ticketId;

    private String queueKey;

    private String statusKey;

    private boolean adopted;

    private Long adoptedByUserId;

    private String adoptedByName;

    private LocalDateTime adoptedAt;

    private LocalDateTime lastActivityAt;

    private String claimFreshness;

    private String title;

    private String desc;

    private String chip;

    private String chipClass;
}
