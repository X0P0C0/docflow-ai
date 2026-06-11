package com.docflow.ai.common.validation;

import com.docflow.ai.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("请求验证服务测试")
class RequestValidationServiceTest {

    private final RequestValidationService service = new RequestValidationService();

    @Test
    @DisplayName("检测 SQL 注入 - SELECT")
    void shouldDetectSqlInjectionSelect() {
        assertThatThrownBy(() -> service.checkSqlInjection("SELECT * FROM users"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("检测 SQL 注入 - UNION")
    void shouldDetectSqlInjectionUnion() {
        assertThatThrownBy(() -> service.checkSqlInjection("1 UNION SELECT password FROM users"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("正常文本应通过 SQL 注入检测")
    void shouldPassNormalText() {
        service.checkSqlInjection("这是一段正常的文本");
    }

    @Test
    @DisplayName("检测路径遍历")
    void shouldDetectPathTraversal() {
        assertThatThrownBy(() -> service.checkPathTraversal("../../etc/passwd"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("正常路径应通过检测")
    void shouldPassNormalPath() {
        service.checkPathTraversal("/uploads/2026/05/29/file.pdf");
    }

    @Test
    @DisplayName("验证邮箱格式")
    void shouldValidateEmail() {
        assertThat(service.isValidEmail("user@example.com")).isTrue();
        assertThat(service.isValidEmail("invalid-email")).isFalse();
        assertThat(service.isValidEmail(null)).isFalse();
    }

    @Test
    @DisplayName("验证手机号格式")
    void shouldValidatePhone() {
        assertThat(service.isValidPhone("13812345678")).isTrue();
        assertThat(service.isValidPhone("12345678901")).isFalse();
        assertThat(service.isValidPhone(null)).isFalse();
    }

    @Test
    @DisplayName("清理 HTML 标签")
    void shouldStripHtml() {
        assertThat(service.stripHtml("<script>alert(1)</script>")).isEqualTo("alert(1)");
        assertThat(service.stripHtml("<b>bold</b>")).isEqualTo("bold");
        assertThat(service.stripHtml("normal text")).isEqualTo("normal text");
        assertThat(service.stripHtml(null)).isNull();
    }

    @Test
    @DisplayName("长度验证")
    void shouldCheckLength() {
        assertThatThrownBy(() -> service.checkLength("a".repeat(101), 100, "标题"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("长度");
    }

    @Test
    @DisplayName("非空验证")
    void shouldCheckNotEmpty() {
        assertThatThrownBy(() -> service.checkNotEmpty("", "标题"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.checkNotEmpty(null, "标题"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("综合文本验证")
    void shouldSanitizeTextInput() {
        String result = service.sanitizeTextInput("  hello world  ", 100, "标题");
        assertThat(result).isEqualTo("hello world");
    }
}
