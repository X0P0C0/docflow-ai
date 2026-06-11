package com.docflow.ai.common.api;

import java.lang.annotation.*;

/**
 * API 版本注解 —— 标记接口的版本号
 * <p>
 * 使用方式：
 * <pre>
 *   @ApiVersion(1)
 *   @GetMapping("/tickets")
 *   public List<Ticket> getTicketsV1() { ... }
 *
 *   @ApiVersion(2)
 *   @GetMapping("/tickets")
 *   public List<TicketV2> getTicketsV2() { ... }
 * </pre>
 * <p>
 * 请求方式：
 * <ul>
 *   <li>Header: Accept-Version: 1</li>
 *   <li>URL: /api/v1/tickets</li>
 *   <li>Query: /api/tickets?version=1</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>URL 版本 vs Header 版本 vs Query 版本的优劣</li>
 *   <li>向后兼容性设计</li>
 *   <li>API 废弃策略（Deprecation）</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    int value() default 1;
}
