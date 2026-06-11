package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docflow.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单模板。
 * <p>
 * 预设工单模板，支持快速创建工单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket_template")
public class TicketTemplate extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    private String name;

    /** 标题模板（支持占位符：{问题描述}） */
    private String titleTemplate;

    /** 内容模板 */
    private String contentTemplate;

    /** 工单类型 */
    private String type;

    /** 优先级 */
    private Integer priority;

    /** 分类 ID */
    private Long categoryId;

    /** 是否公开（1=公开，0=私有） */
    private Integer isPublic;

    /** 创建人 */
    private Long createBy;
}