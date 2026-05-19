package com.docflow.ai.ai.service.impl;

import com.docflow.ai.ai.config.AiWorkspaceProperties;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.entity.KnowledgeArticle;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.entity.TicketComment;
import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketCommentMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiWorkspaceServiceImplTest {

    private AiWorkspaceServiceImpl service;
    private TicketMapper ticketMapper;
    private TicketCommentMapper ticketCommentMapper;
    private TicketRecordMapper ticketRecordMapper;
    private KnowledgeArticleMapper knowledgeArticleMapper;
    private UserAccessService userAccessService;

    @BeforeEach
    void setUp() {
        AiWorkspaceProperties properties = new AiWorkspaceProperties();
        properties.setClaimStaleAfter(Duration.ofMinutes(45));
        ticketMapper = Mockito.mock(TicketMapper.class);
        ticketCommentMapper = Mockito.mock(TicketCommentMapper.class);
        ticketRecordMapper = Mockito.mock(TicketRecordMapper.class);
        knowledgeArticleMapper = Mockito.mock(KnowledgeArticleMapper.class);
        userAccessService = Mockito.mock(UserAccessService.class);
        service = new AiWorkspaceServiceImpl(
                ticketMapper,
                ticketCommentMapper,
                ticketRecordMapper,
                knowledgeArticleMapper,
                Mockito.mock(SysUserMapper.class),
                userAccessService,
                Mockito.mock(StringRedisTemplate.class),
                properties
        );
    }

    @Test
    void resolveLastActivityShouldIgnorePlainRequesterTicketTouch() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setSubmitUserId(100L);
        ticket.setUpdateTime(LocalDateTime.now().minusMinutes(2));

        TicketComment requesterComment = new TicketComment();
        requesterComment.setUserId(100L);
        requesterComment.setCommentType(1);
        requesterComment.setIsInternal(0);
        requesterComment.setCreateTime(LocalDateTime.now().minusMinutes(3));

        LocalDateTime adoptedAt = LocalDateTime.now().minusMinutes(50);
        LocalDateTime lastActivityAt = service.resolveLastActivity(
                ticket,
                List.of(requesterComment),
                List.of(),
                List.of(),
                new AiWorkspaceServiceImpl.AdoptionRecord(2L, "Support Wang", adoptedAt)
        );

        assertThat(lastActivityAt).isEqualTo(adoptedAt);
    }

    @Test
    void resolveLastActivityShouldUseRealOperatorHandlingEvents() {
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setSubmitUserId(100L);

        TicketComment internalOperatorComment = new TicketComment();
        internalOperatorComment.setUserId(2L);
        internalOperatorComment.setCommentType(2);
        internalOperatorComment.setIsInternal(1);
        internalOperatorComment.setCreateTime(LocalDateTime.now().minusMinutes(30));

        TicketRecord assignRecord = new TicketRecord();
        assignRecord.setActionType("ASSIGN");
        assignRecord.setCreateTime(LocalDateTime.now().minusMinutes(20));

        KnowledgeArticle article = new KnowledgeArticle();
        article.setUpdateTime(LocalDateTime.now().minusMinutes(10));

        LocalDateTime lastActivityAt = service.resolveLastActivity(
                ticket,
                List.of(internalOperatorComment),
                List.of(assignRecord),
                List.of(article),
                new AiWorkspaceServiceImpl.AdoptionRecord(2L, "Support Wang", LocalDateTime.now().minusMinutes(40))
        );

        assertThat(lastActivityAt).isEqualTo(article.getUpdateTime());
    }

    @Test
    void resolveClaimFreshnessShouldRespectConfiguredBoundary() {
        String fresh = service.resolveClaimFreshness(
                LocalDateTime.now().minusMinutes(44),
                LocalDateTime.now().minusMinutes(44)
        );
        String stale = service.resolveClaimFreshness(
                LocalDateTime.now().minusMinutes(46),
                LocalDateTime.now().minusMinutes(46)
        );

        assertThat(fresh).isEqualTo("fresh");
        assertThat(stale).isEqualTo("stale");
    }

    @Test
    void getWorkspaceShouldRequireAiCenterAccessBeforeLoadingData() {
        Mockito.doThrow(new BusinessException(ResultCode.FORBIDDEN))
                .when(userAccessService)
                .requireAiCenterAccess(2L);

        assertThatThrownBy(() -> service.getWorkspace(2L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(userAccessService).requireAiCenterAccess(2L);
        Mockito.verifyNoInteractions(ticketMapper, ticketCommentMapper, ticketRecordMapper, knowledgeArticleMapper);
    }
}
