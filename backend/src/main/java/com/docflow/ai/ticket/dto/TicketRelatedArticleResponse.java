package com.docflow.ai.ticket.dto;

import lombok.Data;

@Data
public class TicketRelatedArticleResponse {

    private Long id;

    private String title;

    private String summary;

    private String reason;
}
