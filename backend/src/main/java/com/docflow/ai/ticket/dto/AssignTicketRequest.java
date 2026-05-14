package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignTicketRequest {

    @NotNull(message = "处理人不能为空")
    private Long assigneeUserId;

    private String remark;
}
