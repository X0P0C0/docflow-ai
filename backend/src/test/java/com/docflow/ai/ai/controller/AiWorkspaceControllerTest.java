package com.docflow.ai.ai.controller;

import com.docflow.ai.ai.dto.AiWorkspaceAdoptionResponse;
import com.docflow.ai.ai.dto.AiFeedItem;
import com.docflow.ai.ai.dto.AiReplyDraftResponse;
import com.docflow.ai.ai.dto.AiReplySuggestion;
import com.docflow.ai.ai.dto.AiWorkspaceOverview;
import com.docflow.ai.ai.dto.AiWorkspaceResponse;
import com.docflow.ai.ai.service.AiWorkspaceService;
import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.auth.security.JwtTokenProvider;
import com.docflow.ai.auth.service.UserAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiWorkspaceController.class)
@AutoConfigureMockMvc
@Import(AiWorkspaceControllerTest.MethodSecurityTestConfig.class)
class AiWorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiWorkspaceService aiWorkspaceService;

    @MockBean(name = "userAccessService")
    private UserAccessService userAccessService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getWorkspaceShouldReturnUnifiedSuccessResponse() throws Exception {
        AiWorkspaceOverview overview = new AiWorkspaceOverview();
        overview.setPendingSuggestions(3);
        overview.setAdoptedSuggestions(2);
        overview.setKnowledgeRecommendations(4);

        AiReplySuggestion primarySuggestion = new AiReplySuggestion();
        primarySuggestion.setTicketId(12L);
        primarySuggestion.setStatusKey("needs-reply");
        primarySuggestion.setAdopted(true);
        primarySuggestion.setAdoptedByUserId(2L);
        primarySuggestion.setAdoptedByName("Support Wang");
        primarySuggestion.setAdoptedAt(LocalDateTime.of(2026, 5, 19, 11, 35));
        primarySuggestion.setLastActivityAt(LocalDateTime.of(2026, 5, 19, 11, 48));
        primarySuggestion.setClaimFreshness("fresh");
        primarySuggestion.setTicketNo("INC-20260519-0012");
        primarySuggestion.setTitle("Payment callback failed");
        primarySuggestion.setSummary("Confirm impact scope and share the next troubleshooting checkpoint.");
        primarySuggestion.setScene("incident / In Progress");
        primarySuggestion.setConfidence("High");
        primarySuggestion.setChecklist(List.of("Confirm impact", "Set next update time"));

        AiFeedItem feedItem = new AiFeedItem();
        feedItem.setTitle("Pending reply suggestions");
        feedItem.setValue("3");

        AiWorkspaceResponse response = new AiWorkspaceResponse();
        response.setGeneratedAt(LocalDateTime.of(2026, 5, 19, 11, 30));
        response.setHeuristicBased(true);
        response.setOverview(overview);
        response.setPrimarySuggestion(primarySuggestion);
        response.setFeed(List.of(feedItem));
        response.setRecommendations(List.of());
        response.setFollowups(List.of());
        response.setAdoptedTicketIds(java.util.Set.of(12L));

        when(userAccessService.canAccessAiCenter(2L)).thenReturn(true);
        when(aiWorkspaceService.getWorkspace(2L)).thenReturn(response);

        mockMvc.perform(get("/api/ai/workspace")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(2L, "support"),
                                null,
                                List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.heuristicBased").value(true))
                .andExpect(jsonPath("$.data.overview.pendingSuggestions").value(3))
                .andExpect(jsonPath("$.data.primarySuggestion.statusKey").value("needs-reply"))
                .andExpect(jsonPath("$.data.primarySuggestion.adopted").value(true))
                .andExpect(jsonPath("$.data.primarySuggestion.adoptedByName").value("Support Wang"))
                .andExpect(jsonPath("$.data.primarySuggestion.lastActivityAt").value("2026-05-19T11:48:00"))
                .andExpect(jsonPath("$.data.primarySuggestion.claimFreshness").value("fresh"))
                .andExpect(jsonPath("$.data.adoptedTicketIds[0]").value(12))
                .andExpect(jsonPath("$.data.primarySuggestion.ticketNo").value("INC-20260519-0012"))
                .andExpect(jsonPath("$.data.feed[0].title").value("Pending reply suggestions"));
    }

    @Test
    void getReplyDraftShouldReturnUnifiedSuccessResponse() throws Exception {
        AiReplyDraftResponse response = new AiReplyDraftResponse();
        response.setTicketId(18L);
        response.setStatusKey("reply-in-progress");
        response.setAdopted(true);
        response.setAdoptedByUserId(2L);
        response.setAdoptedByName("Support Wang");
        response.setAdoptedAt(LocalDateTime.of(2026, 5, 19, 12, 0));
        response.setLastActivityAt(LocalDateTime.of(2026, 5, 19, 12, 40));
        response.setClaimFreshness("stale");
        response.setTicketNo("INC-20260519-0018");
        response.setTicketTitle("Permission sync failed");
        response.setScene("incident / In Progress");
        response.setConfidence("High");
        response.setOpener("Thanks for reporting the issue.");
        response.setDiagnosis("The latest operator note already narrows the issue to the permission sync chain.");
        response.setNextStep("Verify the affected scope and send the next customer update.");
        response.setCustomerReply("We are checking the permission sync path now.");
        response.setOperatorNotes(List.of("Keep the update window explicit."));
        response.setRelatedKnowledge(List.of());

        when(userAccessService.canAccessAiCenter(2L)).thenReturn(true);
        when(aiWorkspaceService.getReplyDraft(2L, 18L)).thenReturn(response);

        mockMvc.perform(get("/api/ai/workspace/reply-drafts/18")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(2L, "support"),
                                null,
                                List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticketId").value(18))
                .andExpect(jsonPath("$.data.statusKey").value("reply-in-progress"))
                .andExpect(jsonPath("$.data.adopted").value(true))
                .andExpect(jsonPath("$.data.adoptedByName").value("Support Wang"))
                .andExpect(jsonPath("$.data.lastActivityAt").value("2026-05-19T12:40:00"))
                .andExpect(jsonPath("$.data.claimFreshness").value("stale"))
                .andExpect(jsonPath("$.data.ticketNo").value("INC-20260519-0018"))
                .andExpect(jsonPath("$.data.confidence").value("High"))
                .andExpect(jsonPath("$.data.operatorNotes[0]").value("Keep the update window explicit."));
    }

    @Test
    void markReplyDraftAdoptedShouldReturnUnifiedSuccessResponse() throws Exception {
        AiWorkspaceAdoptionResponse response = new AiWorkspaceAdoptionResponse();
        response.setTicketId(18L);
        response.setAdopted(true);
        response.setAdoptedByUserId(2L);
        response.setAdoptedByName("Support Wang");
        response.setAdoptedAt(LocalDateTime.of(2026, 5, 19, 12, 5));
        response.setLastActivityAt(LocalDateTime.of(2026, 5, 19, 12, 5));
        response.setClaimFreshness("fresh");

        when(userAccessService.canAccessAiCenter(2L)).thenReturn(true);
        when(aiWorkspaceService.markReplyDraftAdopted(2L, 18L)).thenReturn(response);

        mockMvc.perform(post("/api/ai/workspace/reply-drafts/18/adopt")
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(2L, "support"),
                                null,
                                List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticketId").value(18))
                .andExpect(jsonPath("$.data.adopted").value(true))
                .andExpect(jsonPath("$.data.adoptedByName").value("Support Wang"))
                .andExpect(jsonPath("$.data.lastActivityAt").value("2026-05-19T12:05:00"))
                .andExpect(jsonPath("$.data.claimFreshness").value("fresh"));
    }

    @Test
    void unmarkReplyDraftAdoptedShouldReturnUnifiedSuccessResponse() throws Exception {
        AiWorkspaceAdoptionResponse response = new AiWorkspaceAdoptionResponse();
        response.setTicketId(18L);
        response.setAdopted(false);

        when(userAccessService.canAccessAiCenter(2L)).thenReturn(true);
        when(aiWorkspaceService.unmarkReplyDraftAdopted(2L, 18L)).thenReturn(response);

        mockMvc.perform(delete("/api/ai/workspace/reply-drafts/18/adopt")
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(2L, "support"),
                                null,
                                List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ticketId").value(18))
                .andExpect(jsonPath("$.data.adopted").value(false));
    }

    @Test
    void markReplyDraftAdoptedShouldReturnForbiddenWhenAiCenterPermissionDenied() throws Exception {
        when(userAccessService.canAccessAiCenter(1L)).thenReturn(false);

        mockMvc.perform(post("/api/ai/workspace/reply-drafts/18/adopt")
                        .with(csrf())
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(1L, "user"),
                                null,
                                List.of()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300))
                .andExpect(jsonPath("$.error").value("AUTH_FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/ai/workspace/reply-drafts/18/adopt"));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
