package com.docflow.ai.common.datasource;

import java.lang.annotation.*;

/**
 * 强制走主库注解
 * <p>
 * 使用场景：
 * <ul>
 *   <li>写入后立即读取</li>
 *   <li>对数据一致性要求高的查询</li>
 *   <li>管理后台查询</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 *   @ForceMaster
 *   public Ticket getTicketById(Long id) {
 *       return ticketMapper.selectById(id);
 *   }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ForceMaster {
}
