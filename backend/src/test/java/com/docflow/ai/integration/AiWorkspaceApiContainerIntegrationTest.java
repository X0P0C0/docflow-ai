package com.docflow.ai.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiWorkspaceApiContainerIntegrationTest extends AbstractApiContainerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearWorkspaceRedisState() {
        stringRedisTemplate.delete("docflow:ai:workspace:adoptedTickets");
        stringRedisTemplate.delete("docflow:ai:workspace:adoption:100");
        stringRedisTemplate.delete("docflow:ai:workspace:adoption:101");
        jdbcTemplate.update("DELETE FROM kb_article_version");
        jdbcTemplate.update("DELETE FROM kb_article");

        jdbcTemplate.update("""
                INSERT INTO kb_article (
                    id, title, summary, content, category_id, source_ticket_id, author_user_id,
                    status, view_count, like_count, collect_count, publish_time,
                    create_by, update_by, create_time, update_time, deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                300L,
                "Payment callback recovery checklist",
                "Checklist for recovering payment callback incidents after a release.",
                "Use the callback verification checklist and confirm merchant signatures before closing the incident.",
                3L,
                100L,
                2L,
                1,
                0,
                0,
                0,
                java.sql.Timestamp.valueOf("2026-05-19 12:35:00"),
                2L,
                2L,
                java.sql.Timestamp.valueOf("2026-05-19 12:31:00"),
                java.sql.Timestamp.valueOf("2026-05-19 12:35:00"),
                0
        );
    }

    @Test
    void adoptReplyDraftEndpointShouldPersistAdoptionState() throws Exception {
        mockMvc.perform(post("/api/ai/workspace/reply-drafts/101/adopt")
                        .header(authorizationHeaderName(), authHeader(2L, "support01", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticketId").value(101))
                .andExpect(jsonPath("$.data.adopted").value(true))
                .andExpect(jsonPath("$.data.adoptedByUserId").value(2))
                .andExpect(jsonPath("$.data.adoptedByName").value("Support Wang"))
                .andExpect(jsonPath("$.data.claimFreshness").value("fresh"));
    }

    @Test
    void getWorkspaceEndpointShouldReturnReadModelBackedByMysqlAndRedis() throws Exception {
        stringRedisTemplate.opsForSet().add("docflow:ai:workspace:adoptedTickets", "101");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:101", "userId", "2");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:101", "userName", "Support Wang");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:101", "adoptedAt", "2026-05-19T12:10:00");

        mockMvc.perform(get("/api/ai/workspace")
                        .header(authorizationHeaderName(), authHeader(2L, "support01", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.heuristicBased").value(true))
                .andExpect(jsonPath("$.data.overview.pendingSuggestions").value(1))
                .andExpect(jsonPath("$.data.overview.adoptedSuggestions").value(2))
                .andExpect(jsonPath("$.data.overview.knowledgeRecommendations").value(1))
                .andExpect(jsonPath("$.data.primarySuggestion.ticketId").value(101))
                .andExpect(jsonPath("$.data.primarySuggestion.statusKey").value("needs-reply"))
                .andExpect(jsonPath("$.data.primarySuggestion.adopted").value(true))
                .andExpect(jsonPath("$.data.primarySuggestion.adoptedByName").value("Support Wang"))
                .andExpect(jsonPath("$.data.primarySuggestion.ticketNo").value("INC-20260519-0101"))
                .andExpect(jsonPath("$.data.primarySuggestion.title").value("Refund callback pending"))
                .andExpect(jsonPath("$.data.primarySuggestion.scene").value("INCIDENT / In Progress"))
                .andExpect(jsonPath("$.data.primarySuggestion.confidence").value("Medium"))
                .andExpect(jsonPath("$.data.recommendations[0].articleId").value(300))
                .andExpect(jsonPath("$.data.recommendations[0].title").value("Payment callback recovery checklist"))
                .andExpect(jsonPath("$.data.feed[0].title").value("Pending reply suggestions"))
                .andExpect(jsonPath("$.data.feed[0].value").value("1"))
                .andExpect(jsonPath("$.data.followups[0].ticketId").value(101))
                .andExpect(jsonPath("$.data.followups[0].statusKey").value("needs-reply"))
                .andExpect(jsonPath("$.data.followups[0].adopted").value(true))
                .andExpect(jsonPath("$.data.adoptedTicketIds[0]").value(101));
    }

    @Test
    void getReplyDraftEndpointShouldReturnReadModelBackedByMysqlAndRedis() throws Exception {
        stringRedisTemplate.opsForSet().add("docflow:ai:workspace:adoptedTickets", "101");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:101", "userId", "2");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:101", "userName", "Support Wang");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:101", "adoptedAt", "2026-05-19T12:10:00");

        mockMvc.perform(get("/api/ai/workspace/reply-drafts/101")
                        .header(authorizationHeaderName(), authHeader(2L, "support01", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticketId").value(101))
                .andExpect(jsonPath("$.data.statusKey").value("needs-reply"))
                .andExpect(jsonPath("$.data.adopted").value(true))
                .andExpect(jsonPath("$.data.adoptedByUserId").value(2))
                .andExpect(jsonPath("$.data.adoptedByName").value("Support Wang"))
                .andExpect(jsonPath("$.data.claimFreshness").value("fresh"))
                .andExpect(jsonPath("$.data.ticketNo").value("INC-20260519-0101"))
                .andExpect(jsonPath("$.data.ticketTitle").value("Refund callback pending"))
                .andExpect(jsonPath("$.data.scene").value("INCIDENT / In Progress"))
                .andExpect(jsonPath("$.data.confidence").value("Medium"))
                .andExpect(jsonPath("$.data.opener").value(org.hamcrest.Matchers.containsString("INC-20260519-0101")))
                .andExpect(jsonPath("$.data.diagnosis").value(org.hamcrest.Matchers.containsString("No linked knowledge article has been attached yet")))
                .andExpect(jsonPath("$.data.customerReply").value(org.hamcrest.Matchers.containsString("Hello, we have received your report for INC-20260519-0101.")))
                .andExpect(jsonPath("$.data.operatorNotes[0]").value("Ticket priority: 2"))
                .andExpect(jsonPath("$.data.relatedKnowledge").isEmpty());
    }

    @Test
    void unadoptReplyDraftEndpointShouldClearAdoptionState() throws Exception {
        stringRedisTemplate.opsForSet().add("docflow:ai:workspace:adoptedTickets", "100");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:100", "userId", "2");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:100", "userName", "Support Wang");
        stringRedisTemplate.opsForHash().put("docflow:ai:workspace:adoption:100", "adoptedAt", "2026-05-19T12:00:00");

        mockMvc.perform(delete("/api/ai/workspace/reply-drafts/100/adopt")
                        .header(authorizationHeaderName(), authHeader(2L, "support01", "SUPPORT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticketId").value(100))
                .andExpect(jsonPath("$.data.adopted").value(false));
    }

    @Test
    void adoptReplyDraftEndpointShouldRejectUserWithoutAiCenterAccess() throws Exception {
        mockMvc.perform(post("/api/ai/workspace/reply-drafts/101/adopt")
                        .header(authorizationHeaderName(), authHeader(1L, "user01", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.error").value("AUTH_FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/ai/workspace/reply-drafts/101/adopt"));
    }
}
