package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BatchStatusRequest {
    @NotEmpty(message = "Ticket IDs cannot be empty")
    private List<Long> ticketIds;
    @NotNull(message = "Status is required")
    private Integer status;
    private String remark;
}
