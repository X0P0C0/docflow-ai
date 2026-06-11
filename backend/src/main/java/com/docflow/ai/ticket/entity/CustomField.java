package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docflow.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("custom_field")
public class CustomField extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private String options;
    private String defaultValue;
    private Integer required;
    private Integer sortOrder;
    private Long categoryId;
    private Integer status;
}
