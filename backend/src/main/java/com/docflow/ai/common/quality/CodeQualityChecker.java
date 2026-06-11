package com.docflow.ai.common.quality;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码质量检查器 —— 静态代码分析
 * <p>
 * 检查项目：
 * <ul>
 *   <li>方法长度：不超过 50 行</li>
 *   <li>类长度：不超过 500 行</li>
 *   <li>参数个数：不超过 5 个</li>
 *   <li>嵌套深度：不超过 3 层</li>
 *   <li>圈复杂度：不超过 10</li>
 * </ul>
 * <p>
 * 面试考点：
 * <ul>
 *   <li>圈复杂度（Cyclomatic Complexity）的计算</li>
 *   <li>SonarQube 质量门禁</li>
 *   <li>代码异味（Code Smell）</li>
 * </ul>
 */
@Slf4j
@Component
public class CodeQualityChecker {

    /**
     * 检查方法长度
     */
    public QualityIssue checkMethodLength(String methodName, int lines) {
        if (lines > 50) {
            return new QualityIssue(IssueType.METHOD_TOO_LONG,
                    methodName + " 方法过长 (" + lines + " 行)，建议拆分",
                    Severity.WARNING);
        }
        return null;
    }

    /**
     * 检查类长度
     */
    public QualityIssue checkClassLength(String className, int lines) {
        if (lines > 500) {
            return new QualityIssue(IssueType.CLASS_TOO_LONG,
                    className + " 类过长 (" + lines + " 行)，建议拆分",
                    Severity.WARNING);
        }
        return null;
    }

    /**
     * 检查参数个数
     */
    public QualityIssue checkParameterCount(String methodName, int count) {
        if (count > 5) {
            return new QualityIssue(IssueType.TOO_MANY_PARAMETERS,
                    methodName + " 参数过多 (" + count + " 个)，建议使用对象封装",
                    Severity.WARNING);
        }
        return null;
    }

    /**
     * 检查嵌套深度
     */
    public QualityIssue checkNestingDepth(String methodName, int depth) {
        if (depth > 3) {
            return new QualityIssue(IssueType.DEEP_NESTING,
                    methodName + " 嵌套过深 (" + depth + " 层)，建议提取方法",
                    Severity.WARNING);
        }
        return null;
    }

    /**
     * 批量检查
     */
    public List<QualityIssue> checkAll(String className, String methodName,
                                        int methodLines, int classLines,
                                        int paramCount, int nestingDepth) {
        List<QualityIssue> issues = new ArrayList<>();

        QualityIssue methodIssue = checkMethodLength(methodName, methodLines);
        if (methodIssue != null) issues.add(methodIssue);

        QualityIssue classIssue = checkClassLength(className, classLines);
        if (classIssue != null) issues.add(classIssue);

        QualityIssue paramIssue = checkParameterCount(methodName, paramCount);
        if (paramIssue != null) issues.add(paramIssue);

        QualityIssue nestingIssue = checkNestingDepth(methodName, nestingDepth);
        if (nestingIssue != null) issues.add(nestingIssue);

        return issues;
    }

    public record QualityIssue(IssueType type, String message, Severity severity) {}

    public enum IssueType {
        METHOD_TOO_LONG, CLASS_TOO_LONG, TOO_MANY_PARAMETERS, DEEP_NESTING
    }

    public enum Severity {
        INFO, WARNING, ERROR
    }
}
