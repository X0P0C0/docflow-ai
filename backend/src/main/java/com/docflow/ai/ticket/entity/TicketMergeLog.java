package com.docflow.ai.ticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单合并记录。
 * <p>
 * 记录工单合并历史，便于追溯。
 */
@Data
@TableName("ticket_merge_log")
public class TicketMergeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被合并的工单 ID */
    private Long sourceTicketId;

    /** 合并到的目标工单 ID */
    private Long targetTicketId;

    /** 合并原因 */
    private String reason;

    /** 操作人 */
    private Long mergeBy;

    /** 合并时间 */
    private LocalDateTime mergeTime;
}