package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 工单关联请求。
 */
@Data
public class LinkTicketRequest {

    @NotNull(message = "关联工单不能为空")
    private Long linkedTicketId;

    @Pattern(regexp = "RELATED|DUPLICATE|BLOCKED", message = "关联类型必须是 RELATED、DUPLICATE 或 BLOCKED")
    private String linkType;
}