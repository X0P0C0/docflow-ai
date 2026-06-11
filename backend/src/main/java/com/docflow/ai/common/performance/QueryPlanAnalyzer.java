package com.docflow.ai.common.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询计划分析器 —— 使用 EXPLAIN 分析 SQL 性能
 * <p>
 * EXPLAIN 输出关键字段：
 * <ul>
 *   <li>type：访问类型（ALL < index < range < ref < eq_ref < const）</li>
 *   <li>key：使用的索引</li>
 *   <li>rows：扫描行数</li>
 *   <li>Extra：额外信息（Using filesort, Using temporary 等）</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>全表扫描 vs 索引扫描 vs 覆盖索引</li>
 *   <li>回表查询的原因和优化</li>
 *   <li>索引下推（ICP）</li>
 * </ul>
 */
@Slf4j
@Component
public class QueryPlanAnalyzer {

    private final JdbcTemplate jdbcTemplate;

    public QueryPlanAnalyzer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 分析 SQL 查询计划
     * @param sql 要分析的 SQL
     * @return 查询计划详情
     */
    public QueryPlan analyze(String sql) {
        String explainSql = "EXPLAIN " + sql;
        List<Map<String, Object>> planRows = jdbcTemplate.queryForList(explainSql);

        if (planRows.isEmpty()) {
            return QueryPlan.empty();
        }

        Map<String, Object> row = planRows.get(0);
        String type = (String) row.get("type");
        String key = (String) row.get("key");
        Object rows = row.get("rows");
        String extra = (String) row.get("Extra");

        boolean hasIssue = false;
        StringBuilder issues = new StringBuilder();

        // 检查全表扫描
        if ("ALL".equalsIgnoreCase(type)) {
            hasIssue = true;
            issues.append("全表扫描，建议添加索引；");
        }

        // 检查 filesort
        if (extra != null && extra.contains("Using filesort")) {
            hasIssue = true;
            issues.append("文件排序，建议优化 ORDER BY；");
        }

        // 检查临时表
        if (extra != null && extra.contains("Using temporary")) {
            hasIssue = true;
            issues.append("使用临时表，建议优化 GROUP BY；");
        }

        return new QueryPlan(type, key, rows, extra, hasIssue, issues.toString());
    }

    /**
     * 查询计划结果
     */
    public record QueryPlan(
            String accessType,
            String indexUsed,
            Object rowsExamined,
            String extra,
            boolean hasIssue,
            String issueDescription
    ) {
        public static QueryPlan empty() {
            return new QueryPlan(null, null, 0, null, false, "");
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("访问类型: ").append(accessType);
            if (indexUsed != null) {
                sb.append(", 使用索引: ").append(indexUsed);
            }
            sb.append(", 扫描行数: ").append(rowsExamined);
            if (extra != null) {
                sb.append(", 额外信息: ").append(extra);
            }
            if (hasIssue) {
                sb.append(" [警告: ").append(issueDescription).append("]");
            }
            return sb.toString();
        }
    }
}
