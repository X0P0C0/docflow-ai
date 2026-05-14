package com.docflow.ai.knowledge.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.auth.security.JwtTokenProvider;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeArticleController.class)
@AutoConfigureMockMvc
class KnowledgeArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KnowledgeArticleService knowledgeArticleService;

    @MockBean
    private UserAccessService userAccessService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createArticleShouldReturnUnifiedSuccessResponse() throws Exception {
        KnowledgeArticleResponse response = new KnowledgeArticleResponse();
        response.setId(200L);
        response.setTitle("Payment troubleshooting");
        response.setStatus(1);

        when(userAccessService.canManageKnowledge(2L)).thenReturn(true);
        when(knowledgeArticleService.createArticle(eq(2L), any())).thenReturn(response);

        mockMvc.perform(post("/api/knowledge/articles")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(2L, "support"),
                                null,
                                List.of()
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Payment troubleshooting",
                                "summary", "How to handle payment failures",
                                "content", "Step 1: check gateway logs",
                                "categoryId", 1,
                                "sourceTicketId", 100,
                                "status", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(200))
                .andExpect(jsonPath("$.data.title").value("Payment troubleshooting"));
    }

    @Test
    void createArticleShouldReturnValidationErrorsWhenPayloadInvalid() throws Exception {
        when(userAccessService.canManageKnowledge(2L)).thenReturn(true);

        mockMvc.perform(post("/api/knowledge/articles")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(2L, "support"),
                                null,
                                List.of()
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "",
                                "summary", "bad",
                                "content", "",
                                "status", 9
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/knowledge/articles"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
