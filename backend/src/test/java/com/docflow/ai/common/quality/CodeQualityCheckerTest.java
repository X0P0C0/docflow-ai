package com.docflow.ai.common.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("代码质量检查器测试")
class CodeQualityCheckerTest {

    private final CodeQualityChecker checker = new CodeQualityChecker();

    @Test
    @DisplayName("正常方法不应报告问题")
    void shouldNotReportNormalMethod() {
        var issue = checker.checkMethodLength("testMethod", 30);
        assertThat(issue).isNull();
    }

    @Test
    @DisplayName("过长方法应报告问题")
    void shouldReportLongMethod() {
        var issue = checker.checkMethodLength("longMethod", 100);
        assertThat(issue).isNotNull();
        assertThat(issue.type()).isEqualTo(CodeQualityChecker.IssueType.METHOD_TOO_LONG);
    }

    @Test
    @DisplayName("正常类不应报告问题")
    void shouldNotReportNormalClass() {
        var issue = checker.checkClassLength("TestClass", 200);
        assertThat(issue).isNull();
    }

    @Test
    @DisplayName("过长类应报告问题")
    void shouldReportLongClass() {
        var issue = checker.checkClassLength("LongClass", 600);
        assertThat(issue).isNotNull();
        assertThat(issue.type()).isEqualTo(CodeQualityChecker.IssueType.CLASS_TOO_LONG);
    }

    @Test
    @DisplayName("正常参数个数不应报告问题")
    void shouldNotReportNormalParams() {
        var issue = checker.checkParameterCount("testMethod", 3);
        assertThat(issue).isNull();
    }

    @Test
    @DisplayName("过多参数应报告问题")
    void shouldReportTooManyParams() {
        var issue = checker.checkParameterCount("methodWithManyParams", 8);
        assertThat(issue).isNotNull();
        assertThat(issue.type()).isEqualTo(CodeQualityChecker.IssueType.TOO_MANY_PARAMETERS);
    }

    @Test
    @DisplayName("正常嵌套不应报告问题")
    void shouldNotReportNormalNesting() {
        var issue = checker.checkNestingDepth("testMethod", 2);
        assertThat(issue).isNull();
    }

    @Test
    @DisplayName("过深嵌套应报告问题")
    void shouldReportDeepNesting() {
        var issue = checker.checkNestingDepth("deeplyNestedMethod", 5);
        assertThat(issue).isNotNull();
        assertThat(issue.type()).isEqualTo(CodeQualityChecker.IssueType.DEEP_NESTING);
    }

    @Test
    @DisplayName("批量检查应返回所有问题")
    void shouldReturnAllIssues() {
        List<CodeQualityChecker.QualityIssue> issues = checker.checkAll(
                "LongClass", "longMethod", 100, 600, 8, 5);

        assertThat(issues).hasSize(4);
    }

    @Test
    @DisplayName("正常代码批量检查应返回空列表")
    void shouldReturnEmptyForNormalCode() {
        List<CodeQualityChecker.QualityIssue> issues = checker.checkAll(
                "GoodClass", "goodMethod", 20, 100, 3, 2);

        assertThat(issues).isEmpty();
    }
}
