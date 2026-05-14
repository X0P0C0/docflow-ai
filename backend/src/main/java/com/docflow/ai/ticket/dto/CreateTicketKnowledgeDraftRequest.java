package com.docflow.ai.ticket.dto;

import lombok.Data;

@Data
public class CreateTicketKnowledgeDraftRequest {

    private String origin;

    private String closeRemark;
}
