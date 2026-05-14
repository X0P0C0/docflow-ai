package com.docflow.ai.ticket.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.ticket.dto.TicketDetailResponse;
import com.docflow.ai.ticket.service.TicketService;
import com.docflow.ai.auth.security.JwtTokenProvider;
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

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createTicketShouldReturnTicketDetail() throws Exception {
        TicketDetailResponse response = new TicketDetailResponse();
        response.setId(100L);
        response.setTitle("Payment failed");
        response.setStatus(1);

        when(ticketService.createTicket(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(1L, "alice"),
                                null,
                                List.of()
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Payment failed",
                                "content", "Order payment failed for multiple users",
                                "type", "INCIDENT",
                                "categoryId", 1,
                                "priority", 3
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.title").value("Payment failed"));
    }

    @Test
    void createTicketShouldReturnValidationErrorsWhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new AuthUserPrincipal(1L, "alice"),
                                null,
                                List.of()
                        )))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "",
                                "content", "",
                                "type", "",
                                "categoryId", 1,
                                "priority", 9
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/tickets"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
