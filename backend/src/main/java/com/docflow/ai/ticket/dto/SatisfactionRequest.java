package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 满意度评价请求。
 */
@Data
public class SatisfactionRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为 1")
    @Max(value = 5, message = "评分最大为 5")
    private Integer score;

    @Size(max = 500, message = "评价内容最多 500 字")
    private String comment;
}