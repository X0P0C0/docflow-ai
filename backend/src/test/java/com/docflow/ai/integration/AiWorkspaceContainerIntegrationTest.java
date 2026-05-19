package com.docflow.ai.integration;

import com.docflow.ai.ai.dto.AiReplyDraftResponse;
import com.docflow.ai.ai.dto.AiWorkspaceAdoptionResponse;
import com.docflow.ai.ai.service.AiWorkspaceService;
import com.docflow.ai.support.AbstractContainerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AiWorkspaceContainerIntegrationTest extends AbstractContainerIntegrationTest {

    @Autowired
    private AiWorkspaceService aiWorkspaceService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void clearWorkspaceRedisState() {
        stringRedisTemplate.delete("docflow:ai:workspace:adoptedTickets");
        stringRedisTemplate.delete("docflow:ai:workspace:adoption:100");
        stringRedisTemplate.delete("docflow:ai:workspace:adoption:101");
    }

    @Test
    void shouldPersistAndReadBackAdoptionStateThroughRedisAndMysql() {
        AiWorkspaceAdoptionResponse adoption = aiWorkspaceService.markReplyDraftAdopted(2L, 101L);

        assertThat(adoption.getTicketId()).isEqualTo(101L);
        assertThat(adoption.isAdopted()).isTrue();
        assertThat(adoption.getAdoptedByUserId()).isEqualTo(2L);
        assertThat(adoption.getAdoptedByName()).isEqualTo("Support Wang");
        assertThat(adoption.getAdoptedAt()).isNotNull();
        assertThat(adoption.getLastActivityAt()).isEqualTo(adoption.getAdoptedAt());
        assertThat(adoption.getClaimFreshness()).isEqualTo("fresh");

        assertThat(stringRedisTemplate.opsForSet().members("docflow:ai:workspace:adoptedTickets"))
                .contains("101");
        assertThat(stringRedisTemplate.opsForHash().get("docflow:ai:workspace:adoption:101", "userId"))
                .isEqualTo("2");

        AiReplyDraftResponse draft = aiWorkspaceService.getReplyDraft(2L, 101L);

        assertThat(draft.isAdopted()).isTrue();
        assertThat(draft.getAdoptedByUserId()).isEqualTo(2L);
        assertThat(draft.getAdoptedByName()).isEqualTo("Support Wang");
        assertThat(draft.getClaimFreshness()).isEqualTo("fresh");
        assertThat(draft.getTicketNo()).isEqualTo("INC-20260519-0101");
    }

    @Test
    void shouldRemoveAdoptionStateFromRedisWhenUnmarked() {
        aiWorkspaceService.markReplyDraftAdopted(2L, 100L);

        AiWorkspaceAdoptionResponse response = aiWorkspaceService.unmarkReplyDraftAdopted(2L, 100L);

        assertThat(response.getTicketId()).isEqualTo(100L);
        assertThat(response.isAdopted()).isFalse();
        assertThat(stringRedisTemplate.opsForSet().members("docflow:ai:workspace:adoptedTickets"))
                .doesNotContain("100");
        assertThat(stringRedisTemplate.hasKey("docflow:ai:workspace:adoption:100")).isFalse();
    }
}
