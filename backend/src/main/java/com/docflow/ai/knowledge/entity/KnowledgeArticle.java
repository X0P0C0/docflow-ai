package com.docflow.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docflow.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_article")
public class KnowledgeArticle extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String content;

    private Long categoryId;

    private Long sourceTicketId;

    private Long authorUserId;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private LocalDateTime publishTime;
}
