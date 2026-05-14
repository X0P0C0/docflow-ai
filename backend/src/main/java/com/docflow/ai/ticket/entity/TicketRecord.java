package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket_record")
public class TicketRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ticketId;

    private Long operatorUserId;

    private String actionType;

    private Integer oldStatus;

    private Integer newStatus;

    private String remark;

    private LocalDateTime createTime;
}
