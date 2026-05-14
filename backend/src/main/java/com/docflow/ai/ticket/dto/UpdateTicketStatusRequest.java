package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTicketStatusRequest {

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
