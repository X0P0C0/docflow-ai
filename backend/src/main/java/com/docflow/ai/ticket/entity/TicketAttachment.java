package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ticket_attachment")
public class TicketAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String fileType;
    private Long uploadUserId;
    private LocalDateTime createTime;
    private Integer deleted;

    public void setContentType(String contentType) { this.fileType = contentType; }
}
