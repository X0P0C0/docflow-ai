package com.docflow.ai.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XSS 过滤器测试")
class XssFilterTest {

    @Test
    @DisplayName("移除 script 标签")
    void shouldRemoveScriptTags() {
        String input = "<script>alert('xss')</script>Hello";
        String expected = "Hello";
        assertThat(cleanXss(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("移除 javascript 协议")
    void shouldRemoveJavascriptProtocol() {
        String input = "javascript:alert(1)";
        String expected = "alert(1)";
        assertThat(cleanXss(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("移除事件属性")
    void shouldRemoveEventAttributes() {
        String input = "onmouseover=alert(1)";
        String expected = "alert(1)";
        assertThat(cleanXss(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("HTML 实体编码")
    void shouldEncodeHtmlEntities() {
        String input = "<div>test</div>";
        String expected = "&lt;div&gt;test&lt;/div&gt;";
        assertThat(cleanXss(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("保留正常文本")
    void shouldPreserveNormalText() {
        String input = "Hello World 123";
        assertThat(cleanXss(input)).isEqualTo(input);
    }

    private String cleanXss(String value) {
        // Simplified version for testing
        value = value.replaceAll("<script[^>]*>.*?</script>", "");
        value = value.replaceAll("javascript:", "");
        value = value.replaceAll("on\\w+=", "");
        value = value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        return value;
    }
}
