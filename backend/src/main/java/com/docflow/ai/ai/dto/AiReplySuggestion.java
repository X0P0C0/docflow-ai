package com.docflow.ai.ai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AiReplySuggestion {

    private Long ticketId;

    private String statusKey;

    private boolean adopted;

    private Long adoptedByUserId;

    private String adoptedByName;

    private LocalDateTime adoptedAt;

    private LocalDateTime lastActivityAt;

    private String claimFreshness;

    private String ticketNo;

    private String title;

    private String summary;

    private String scene;

    private String confidence;

    private List<String> checklist;
}
