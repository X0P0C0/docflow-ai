package com.docflow.ai.logging;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作审计日志实体。
 * <p>
 * 存储在 MySQL 的 sys_audit_log 表中，记录所有关键操作。
 */
@Data
@TableName("sys_audit_log")
public class AuditLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作模块 */
    private String module;

    /** 操作描述 */
    private String action;

    /** 请求方法（GET/POST/PUT/DELETE） */
    private String method;

    /** 请求 URI */
    private String uri;

    /** HTTP 状态码 */
    private Integer statusCode;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 客户端 IP */
    private String clientIp;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 是否成功（1=成功，0=失败） */
    private Integer success;

    /** 错误信息（失败时记录） */
    private String errorMessage;

    /** 链路追踪 ID */
    private String traceId;
}