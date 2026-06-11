package com.docflow.ai.common.performance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("索引提示服务测试")
class IndexHintServiceTest {

    private final IndexHintService indexHintService = new IndexHintService();

    @Test
    @DisplayName("有索引时应返回匹配结果")
    void shouldDetectExistingIndex() {
        var suggestion = indexHintService.checkIndex("ticket", List.of("status"));
        assertThat(suggestion.hasIndex()).isTrue();
    }

    @Test
    @DisplayName("无索引时应建议创建")
    void shouldSuggestMissingIndex() {
        var suggestion = indexHintService.checkIndex("ticket", List.of("priority", "type"));
        assertThat(suggestion.hasIndex()).isFalse();
        assertThat(suggestion.suggestedIndex()).contains("priority");
    }

    @Test
    @DisplayName("生成创建索引 SQL")
    void shouldGenerateCreateIndexSql() {
        String sql = indexHintService.generateCreateIndexSql("ticket", "priority", "type");
        assertThat(sql).contains("CREATE INDEX");
        assertThat(sql).contains("ticket");
        assertThat(sql).contains("priority");
    }
}
