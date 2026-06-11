package com.docflow.ai.config.sharding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 【分库分表学习笔记】
 * <p>
 * 分库分表是应对海量数据（千万级+）的核心技术，但你的项目目前 9 张表、几百条数据，
 * 强行分库分表是画蛇添足。这里只做概念演示，帮你理解原理。
 *
 * <h3>一、什么时候需要分库分表？</h3>
 * <ul>
 *   <li>单表数据超过 1000 万行 → 查询变慢（B+ 树深度增加）</li>
 *   <li>单库写入 QPS 超过 5000 → 连接池耗尽</li>
 *   <li>磁盘空间不足 → 需要水平扩展</li>
 * </ul>
 *
 * <h3>二、垂直拆分 vs 水平拆分</h3>
 * <pre>
 * 垂直分库（按业务拆）：
 *   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
 *   │ 用户库       │  │ 工单库       │  │ 知识库       │
 *   │ sys_user     │  │ ticket      │  │ kb_article  │
 *   │ sys_role     │  │ ticket_comment│ │ kb_tag      │
 *   └─────────────┘  └─────────────┘  └─────────────┘
 *
 * 水平分表（按规则拆行）：
 *   ticket 表 → ticket_0, ticket_1, ticket_2, ticket_3
 *   路由规则：ticket_id % 4 = 表后缀
 * </pre>
 *
 * <h3>三、ShardingSphere 配置示例（application-sharding.yml）</h3>
 * <pre>
 * spring:
 *   shardingsphere:
 *     datasource:
 *       names: ds0, ds1
 *       ds0:
 *         url: jdbc:mysql://localhost:3306/docflow_ai_0
 *         username: root
 *         password: 123456
 *       ds1:
 *         url: jdbc:mysql://localhost:3306/docflow_ai_1
 *         username: root
 *         password: 123456
 *     rules:
 *       sharding:
 *         tables:
 *           ticket:
 *             actual-data-nodes: ds$->{0..1}.ticket_$->{0..3}
 *             table-strategy:
 *               standard:
 *                 sharding-column: id
 *                 sharding-algorithm-name: ticket-inline
 *         sharding-algorithms:
 *           ticket-inline:
 *             type: INLINE
 *             props:
 *               algorithm-expression: ticket_$->{id % 4}
 * </pre>
 *
 * <h3>四、分库分表带来的问题</h3>
 * <ul>
 *   <li><b>分布式事务</b>：跨库操作需要 Seata/TCC 等方案保证一致性</li>
 *   <li><b>跨库 JOIN</b>：无法直接 JOIN，需要应用层组装或冗余数据</li>
 *   <li><b>全局 ID</b>：不能用自增 ID，需要雪花算法/UUID</li>
 *   <li><b>扩容困难</b>：增加分片需要数据迁移</li>
 * </ul>
 *
 * <h3>五、你项目的建议</h3>
 * <p>
 * 当前数据量（< 1000 行）完全不需要分库分表。如果未来数据量增长：
 * <ol>
 *   <li>第一步：加索引 + 读写分离（主从复制）</li>
 *   <li>第二步：垂直分库（用户/工单/知识库拆开）</li>
 *   <li>第三步：水平分表（只有热点表才需要，如 ticket）</li>
 * </ol>
 * <p>
 * 这个配置文件是学习用的，不会实际生效。
 */
@Slf4j
@Configuration
public class ShardingConfigDemo {

    // 这个类只是一个学习笔记，不包含实际配置。
    // 实际分库分表需要在 application-sharding.yml 中配置，并添加 ShardingSphere 依赖。
    // 当前项目不需要分库分表，这里只是帮你理解概念。

    static {
        log.info("【学习笔记】分库分表配置演示类已加载。当前项目不需要分库分表，这只是学习用的。");
    }
}