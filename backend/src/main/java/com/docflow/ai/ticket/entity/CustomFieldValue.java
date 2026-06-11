package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docflow.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("custom_field_value")
public class CustomFieldValue extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long fieldId;
    private String fieldValue;
}
