package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BatchAssignRequest {
    @NotEmpty(message = "Ticket IDs cannot be empty")
    private List<Long> ticketIds;
    @NotNull(message = "Assignee is required")
    private Long assigneeUserId;
    private String remark;
}
