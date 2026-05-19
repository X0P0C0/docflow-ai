package com.docflow.ai.ticket.service.impl;

import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.mapper.TicketCommentMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

class TicketServiceImplTest {

    private TicketMapper ticketMapper;
    private TicketRecordMapper ticketRecordMapper;
    private TicketCommentMapper ticketCommentMapper;
    private KnowledgeArticleMapper knowledgeArticleMapper;
    private SysUserMapper sysUserMapper;
    private KnowledgeArticleService knowledgeArticleService;
    private UserAccessService userAccessService;
    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        ticketMapper = Mockito.mock(TicketMapper.class);
        ticketRecordMapper = Mockito.mock(TicketRecordMapper.class);
        ticketCommentMapper = Mockito.mock(TicketCommentMapper.class);
        knowledgeArticleMapper = Mockito.mock(KnowledgeArticleMapper.class);
        sysUserMapper = Mockito.mock(SysUserMapper.class);
        knowledgeArticleService = Mockito.mock(KnowledgeArticleService.class);
        userAccessService = Mockito.mock(UserAccessService.class);
        service = new TicketServiceImpl(
                ticketMapper,
                ticketRecordMapper,
                ticketCommentMapper,
                knowledgeArticleMapper,
                sysUserMapper,
                knowledgeArticleService,
                userAccessService
        );
    }

    @Test
    void createKnowledgeDraftShouldRequireKnowledgeManagerBeforeReadingTicket() {
        CreateTicketKnowledgeDraftRequest request = new CreateTicketKnowledgeDraftRequest();

        Mockito.doThrow(new BusinessException(ResultCode.FORBIDDEN))
                .when(userAccessService)
                .requireKnowledgeManager(8L);

        assertThatThrownBy(() -> service.createKnowledgeDraft(100L, 8L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(userAccessService).requireKnowledgeManager(8L);
        Mockito.verifyNoInteractions(ticketMapper, ticketRecordMapper, ticketCommentMapper, knowledgeArticleMapper,
                sysUserMapper, knowledgeArticleService);
    }
}
