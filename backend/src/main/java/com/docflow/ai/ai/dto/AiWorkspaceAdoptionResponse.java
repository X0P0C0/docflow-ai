package com.docflow.ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiWorkspaceAdoptionResponse {

    private Long ticketId;

    private boolean adopted;

    private Long adoptedByUserId;

    private String adoptedByName;

    private LocalDateTime adoptedAt;

    private LocalDateTime lastActivityAt;

    private String claimFreshness;
}
