package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTicketRequest {

    @NotBlank(message = "工单标题不能为空")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    private String content;

    @NotBlank(message = "工单类型不能为空")
    private String type;

    @NotNull(message = "工单分类不能为空")
    private Long categoryId;

    @NotNull(message = "优先级不能为空")
    @Min(value = 1, message = "优先级不合法")
    @Max(value = 4, message = "优先级不合法")
    private Integer priority;
}
