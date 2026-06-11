package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SLA 策略配置。
 * <p>
 * 定义不同工单类型和优先级的响应时间、解决时间要求。
 */
@Data
@TableName("sla_policy")
public class SlaPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 策略名称 */
    private String name;

    /** 适用工单类型（NULL=所有类型） */
    private String ticketType;

    /** 适用优先级（NULL=所有优先级） */
    private Integer priority;

    /** 响应时间（小时） */
    private Integer responseHours;

    /** 解决时间（小时） */
    private Integer resolveHours;

    /** 是否只计算工作时间 */
    private Integer businessHoursOnly;

    /** 状态（1=启用，0=禁用） */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}