package com.docflow.ai.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志注解。
 * <p>
 * 使用方式：
 * <pre>
 *   @AuditLog(module = "工单", action = "创建工单")
 *   @PostMapping
 *   public ApiResponse&lt;TicketDetailResponse&gt; createTicket(...) { ... }
 * </pre>
 * <p>
 * 记录内容：谁（userId）在什么时间（timestamp）对什么模块（module）做了什么操作（action），结果如何（success/fail）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 操作模块，如 "工单"、"知识库"、"用户管理" */
    String module();

    /** 操作描述，如 "创建工单"、"删除文章" */
    String action();
}