package com.docflow.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_article_version")
public class KnowledgeArticleVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Integer versionNo;

    private String title;

    private String summary;

    private String content;

    private Long operatorUserId;

    private String remark;

    private LocalDateTime createTime;
}
