package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.docflow.ai.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ticket")
public class Ticket extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ticketNo;

    private String title;

    private String content;

    private String type;

    private Long categoryId;

    private Integer priority;

    private Integer status;

    private Long submitUserId;

    private Long assigneeUserId;

    private Long deptId;

    private String source;

    private LocalDateTime expectedFinishTime;

    private LocalDateTime actualFinishTime;

    private LocalDateTime closeTime;
}
