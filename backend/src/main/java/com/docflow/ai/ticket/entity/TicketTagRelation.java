package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ticket_tag_relation")
public class TicketTagRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long tagId;
    private LocalDateTime createTime;
}
