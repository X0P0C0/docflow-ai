package com.docflow.ai.common.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 索引提示服务 —— 帮助开发者了解索引使用情况
 * <p>
 * 功能：
 * <ul>
 *   <li>提供常见查询的索引使用建议</li>
 *   <li>记录缺失索引的查询</li>
 *   <li>生成索引创建 SQL</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>索引类型：B+Tree vs Hash vs Full-text</li>
 *   <li>联合索引的最左前缀原则</li>
 *   <li>覆盖索引 vs 回表查询</li>
 *   <li>索引选择性（Cardinality）</li>
 * </ul>
 */
@Slf4j
@Component
public class IndexHintService {

    // 已知的索引配置
    private static final Map<String, List<String>> TABLE_INDEXES = Map.of(
        "ticket", List.of("idx_ticket_no", "idx_ticket_status", "idx_ticket_assignee", "idx_ticket_create_time"),
        "ticket_record", List.of("idx_ticket_record_ticket_id"),
        "ticket_comment", List.of("idx_ticket_comment_ticket_id"),
        "knowledge_article", List.of("idx_article_status", "idx_article_category")
    );

    /**
     * 检查查询是否使用了索引
     * @param table 表名
     * @param whereColumns WHERE 条件中的列
     * @return 建议的索引
     */
    public IndexSuggestion checkIndex(String table, List<String> whereColumns) {
        List<String> existingIndexes = TABLE_INDEXES.getOrDefault(table.toLowerCase(), Collections.emptyList());

        // 检查是否有匹配的索引
        boolean hasMatchingIndex = existingIndexes.stream()
                .anyMatch(idx -> idx.contains(whereColumns.get(0)));

        if (!hasMatchingIndex && !whereColumns.isEmpty()) {
            String suggestedIndex = String.format("idx_%s_%s", table.toLowerCase(),
                    String.join("_", whereColumns));
            return new IndexSuggestion(table, whereColumns, suggestedIndex, false);
        }

        return new IndexSuggestion(table, whereColumns, null, true);
    }

    /**
     * 生成创建索引的 SQL
     */
    public String generateCreateIndexSql(String table, String... columns) {
        String indexName = "idx_" + table.toLowerCase() + "_" + String.join("_", columns);
        return String.format("CREATE INDEX %s ON %s (%s);", indexName, table, String.join(", ", columns));
    }

    /**
     * 索引建议结果
     */
    public record IndexSuggestion(
        String table,
        List<String> columns,
        String suggestedIndex,
        boolean hasIndex
    ) {
        public String getMessage() {
            if (hasIndex) {
                return "索引已存在";
            }
            return "建议创建索引: " + suggestedIndex;
        }
    }
}
