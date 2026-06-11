package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单关联关系。
 * <p>
 * 支持三种关联类型：
 * <ul>
 *   <li>RELATED - 相关工单（两个工单有关系）</li>
 *   <li>DUPLICATE - 重复工单（一个工单是另一个的重复）</li>
 *   <li>BLOCKED - 阻塞关系（一个工单被另一个阻塞）</li>
 * </ul>
 */
@Data
@TableName("ticket_link")
public class TicketLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 ID */
    private Long ticketId;

    /** 关联工单 ID */
    private Long linkedTicketId;

    /** 关联类型：RELATED, DUPLICATE, BLOCKED */
    private String linkType;

    /** 创建人 */
    private Long createBy;

    /** 创建时间 */
    private LocalDateTime createTime;
}