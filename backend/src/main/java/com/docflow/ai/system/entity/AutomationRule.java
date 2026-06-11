package com.docflow.ai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("automation_rule")
public class AutomationRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String triggerType;
    private String conditions;
    private String actions;
    private Integer priority;
    private Integer status;
    private Long createBy;
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
