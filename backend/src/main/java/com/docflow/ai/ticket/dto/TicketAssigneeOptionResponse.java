package com.docflow.ai.ticket.dto;

import lombok.Data;

import java.util.List;

@Data
public class TicketAssigneeOptionResponse {

    private Long id;

    private String username;

    private String displayName;

    private List<String> roles;
}
