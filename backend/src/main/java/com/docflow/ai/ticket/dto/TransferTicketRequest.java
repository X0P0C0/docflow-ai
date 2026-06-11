package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单转派请求。
 */
@Data
public class TransferTicketRequest {

    @NotNull(message = "新处理人不能为空")
    private Long newAssigneeUserId;

    @Size(max = 500, message = "转派原因最多 500 字")
    private String reason;
}