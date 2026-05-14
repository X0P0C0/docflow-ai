package com.docflow.ai.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKnowledgeArticleRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过200个字符")
    private String title;

    @Size(max = 500, message = "文章摘要长度不能超过500个字符")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String content;

    private Long categoryId;

    private Long sourceTicketId;

    @NotNull(message = "文章状态不能为空")
    @Min(value = 0, message = "文章状态不合法")
    @Max(value = 2, message = "文章状态不合法")
    private Integer status;
}
