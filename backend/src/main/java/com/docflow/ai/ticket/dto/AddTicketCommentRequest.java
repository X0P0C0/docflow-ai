package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTicketCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    private Integer commentType;

    private Boolean internal;
}
