package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工单升级请求。
 */
@Data
public class EscalateTicketRequest {

    @NotNull(message = "升级层级不能为空")
    @Min(value = 1, message = "升级层级最小为 1")
    @Max(value = 3, message = "升级层级最大为 3")
    private Integer escalationLevel;

    @Size(max = 500, message = "升级原因最多 500 字")
    private String reason;
}