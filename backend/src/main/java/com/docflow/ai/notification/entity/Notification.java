package com.docflow.ai.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知实体。
 * <p>
 * 典型场景：
 * <ul>
 *   <li>工单被指派给你</li>
 *   <li>你的工单状态变更</li>
 *   <li>有人评论了你的工单</li>
 *   <li>AI 生成了新的建议</li>
 * </ul>
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通知标题 */
    private String title;

    /** 通知内容 */
    private String content;

    /** 通知类型：ASSIGN / STATUS_CHANGE / COMMENT / AI_SUGGESTION / SYSTEM */
    private String notificationType;

    /** 接收人 ID */
    private Long receiverUserId;

    /** 关联资源类型：TICKET / ARTICLE */
    private String relatedBusinessType;

    /** 关联资源 ID */
    private Long relatedBusinessId;

    /** 是否已读（0=未读，1=已读） */
    private Integer isRead;

    /** 已读时间 */
    private LocalDateTime readTime;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除 */
    private Integer deleted;
}