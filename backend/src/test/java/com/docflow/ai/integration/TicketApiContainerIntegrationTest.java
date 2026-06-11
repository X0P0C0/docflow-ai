package com.docflow.ai.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketApiContainerIntegrationTest extends AbstractApiContainerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetKnowledgeDraftArtifacts() {
        jdbcTemplate.update("DELETE FROM kb_article_version");
        jdbcTemplate.update("DELETE FROM kb_article");
    }

    @Test
    void createKnowledgeDraftEndpointShouldCreateDraftWithRealSecurityAndPersistence() throws Exception {
        mockMvc.perform(post("/api/tickets/100/knowledge-draft")
                        .header(authorizationHeaderName(), authHeader(2L, "support01", "SUPPORT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": "ai-center",
                                  "closeRemark": "Root cause fixed and customer informed."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sourceTicketId").value(100))
                .andExpect(jsonPath("$.data.authorUserId").value(2))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.title").value("Payment callback failed处理复盘"))
                .andExpect(jsonPath("$.data.content").value(org.hamcrest.Matchers.containsString("Root cause fixed and customer informed.")));
    }

    @Test
    void createKnowledgeDraftEndpointShouldRejectUserWithoutKnowledgeAccess() throws Exception {
        mockMvc.perform(post("/api/tickets/100/knowledge-draft")
                        .header(authorizationHeaderName(), authHeader(1L, "user01", "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "origin": "portal"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.error").value("AUTH_FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/tickets/100/knowledge-draft"));
    }
}
