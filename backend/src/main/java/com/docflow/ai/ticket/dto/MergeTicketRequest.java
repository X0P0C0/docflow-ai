package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单合并请求。
 */
@Data
public class MergeTicketRequest {

    @NotNull(message = "目标工单不能为空")
    private Long targetTicketId;

    @Size(max = 500, message = "合并原因最多 500 字")
    private String reason;
}