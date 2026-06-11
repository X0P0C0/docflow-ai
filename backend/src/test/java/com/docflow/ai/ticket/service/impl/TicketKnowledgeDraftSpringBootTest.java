package com.docflow.ai.ticket.service.impl;

import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.dto.CreateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.mapper.TicketCommentMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import com.docflow.ai.ticket.service.SlaService;
import com.docflow.ai.ticket.mapper.TicketLinkMapper;
import com.docflow.ai.ticket.mapper.TicketMergeLogMapper;
import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.common.pattern.RedisEventPublisher;
import com.docflow.ai.common.pattern.RedisEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TicketKnowledgeDraftSpringBootTest.Config.class)
class TicketKnowledgeDraftSpringBootTest {

    @Autowired
    private TicketServiceImpl ticketService;

    @MockBean
    private TicketMapper ticketMapper;

    @MockBean
    private TicketRecordMapper ticketRecordMapper;

    @MockBean
    private TicketCommentMapper ticketCommentMapper;

    @MockBean
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @MockBean
    private SysUserMapper sysUserMapper;

    @MockBean
    private KnowledgeArticleService knowledgeArticleService;

    @MockBean
    private UserAccessService userAccessService;

    @Test
    void createKnowledgeDraftShouldRejectTicketsThatAreNotReadyForKnowledgeCapture() {
        Ticket ticket = baseTicket();
        ticket.setStatus(2);

        SysUser submitter = new SysUser();
        submitter.setId(1L);
        submitter.setUsername("user01");
        submitter.setRealName("User One");
        submitter.setDeleted(0);
        submitter.setStatus(1);

        doNothing().when(userAccessService).requireKnowledgeManager(2L);
        when(userAccessService.canOperateTickets(2L)).thenReturn(true);
        when(ticketMapper.selectById(100L)).thenReturn(ticket);
        when(ticketRecordMapper.selectList(any())).thenReturn(List.of());
        when(ticketCommentMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeArticleMapper.selectList(any())).thenReturn(List.of());
        when(sysUserMapper.selectById(1L)).thenReturn(submitter);

        assertThatThrownBy(() -> ticketService.createKnowledgeDraft(100L, 2L, new CreateTicketKnowledgeDraftRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("可沉淀");
    }

    @Test
    void createKnowledgeDraftShouldAssembleDraftFromResolvedTicket() {
        Ticket ticket = baseTicket();
        ticket.setStatus(3);

        SysUser submitter = new SysUser();
        submitter.setId(1L);
        submitter.setUsername("user01");
        submitter.setRealName("User One");
        submitter.setDeleted(0);
        submitter.setStatus(1);

        KnowledgeArticleResponse created = new KnowledgeArticleResponse();
        created.setId(300L);
        created.setTitle("Payment callback failed处理复盘");

        doNothing().when(userAccessService).requireKnowledgeManager(2L);
        when(userAccessService.canOperateTickets(2L)).thenReturn(true);
        when(ticketMapper.selectById(100L)).thenReturn(ticket);
        when(ticketRecordMapper.selectList(any())).thenReturn(List.of());
        when(ticketCommentMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeArticleMapper.selectList(any())).thenReturn(List.of());
        when(knowledgeArticleMapper.selectOne(any())).thenReturn(null);
        when(sysUserMapper.selectById(1L)).thenReturn(submitter);
        when(knowledgeArticleService.createArticle(any(), any())).thenReturn(created);

        CreateTicketKnowledgeDraftRequest request = new CreateTicketKnowledgeDraftRequest();
        request.setOrigin("ai-center");
        request.setCloseRemark("Root cause fixed and customer informed.");

        KnowledgeArticleResponse response = ticketService.createKnowledgeDraft(100L, 2L, request);

        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getTitle()).contains("处理复盘");

        ArgumentCaptor<CreateKnowledgeArticleRequest> requestCaptor = ArgumentCaptor.forClass(CreateKnowledgeArticleRequest.class);
        verify(knowledgeArticleService).createArticle(org.mockito.Mockito.eq(2L), requestCaptor.capture());

        CreateKnowledgeArticleRequest articleRequest = requestCaptor.getValue();
        assertThat(articleRequest.getSourceTicketId()).isEqualTo(100L);
        assertThat(articleRequest.getStatus()).isEqualTo(0);
        assertThat(articleRequest.getCategoryId()).isEqualTo(3L);
        assertThat(articleRequest.getTitle()).contains("Payment callback failed");
        assertThat(articleRequest.getSummary()).contains("INC-20260519-0100");
        assertThat(articleRequest.getContent()).contains("Root cause fixed and customer informed.");
        assertThat(articleRequest.getContent()).contains("Payment callback failed for several orders");
        assertThat(articleRequest.getContent()).contains("当前状态：Resolved");
    }

    private Ticket baseTicket() {
        Ticket ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTicketNo("INC-20260519-0100");
        ticket.setTitle("Payment callback failed");
        ticket.setContent("Payment callback failed for several orders after the latest release.");
        ticket.setType("INCIDENT");
        ticket.setCategoryId(3L);
        ticket.setPriority(3);
        ticket.setSubmitUserId(1L);
        ticket.setDeleted(0);
        return ticket;
    }

    @TestConfiguration
    static class Config {

        @Bean
        TicketServiceImpl ticketService(TicketMapper ticketMapper,
                                        TicketRecordMapper ticketRecordMapper,
                                        TicketCommentMapper ticketCommentMapper,
                                        KnowledgeArticleMapper knowledgeArticleMapper,
                                        SysUserMapper sysUserMapper,
                                        KnowledgeArticleService knowledgeArticleService,
                                        SlaService slaService,
                                        TicketLinkMapper ticketLinkMapper,
                                        TicketMergeLogMapper ticketMergeLogMapper,
                                        StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        UserAccessService userAccessService,
                                        BusinessMetricsService metrics,
                                        RedisEventPublisher redisEventPublisher) {
            return new TicketServiceImpl(
                    ticketMapper, redisEventPublisher,
                    ticketRecordMapper, ticketCommentMapper,
                    knowledgeArticleMapper, sysUserMapper, knowledgeArticleService,
                    userAccessService, metrics, objectMapper,
                    redisTemplate, ticketMergeLogMapper, ticketLinkMapper, slaService);
        }
    }
}
