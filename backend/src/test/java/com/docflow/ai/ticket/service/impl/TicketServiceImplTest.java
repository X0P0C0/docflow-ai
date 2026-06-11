package com.docflow.ai.ticket.service.impl;

import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import com.docflow.ai.monitoring.BusinessMetricsService;
import com.docflow.ai.common.pattern.RedisEventPublisher;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.mapper.*;
import com.docflow.ai.ticket.service.SlaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

class TicketServiceImplTest {

    private TicketServiceImpl service;
    private UserAccessService userAccessService;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(
                Mockito.mock(TicketMapper.class),
                Mockito.mock(RedisEventPublisher.class),
                Mockito.mock(TicketRecordMapper.class),
                Mockito.mock(TicketCommentMapper.class),
                Mockito.mock(KnowledgeArticleMapper.class),
                Mockito.mock(SysUserMapper.class),
                Mockito.mock(KnowledgeArticleService.class),
                userAccessService = Mockito.mock(UserAccessService.class),
                Mockito.mock(BusinessMetricsService.class),
                Mockito.mock(ObjectMapper.class),
                Mockito.mock(StringRedisTemplate.class),
                Mockito.mock(TicketMergeLogMapper.class),
                Mockito.mock(TicketLinkMapper.class),
                Mockito.mock(SlaService.class)
        );
    }

    @Test
    void createKnowledgeDraftShouldRequireKnowledgeManagerBeforeReadingTicket() {
        CreateTicketKnowledgeDraftRequest request = new CreateTicketKnowledgeDraftRequest();
        Mockito.doThrow(new BusinessException(ResultCode.FORBIDDEN))
                .when(userAccessService).requireKnowledgeManager(8L);
        assertThatThrownBy(() -> service.createKnowledgeDraft(100L, 8L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ResultCode.FORBIDDEN.getCode());
        verify(userAccessService).requireKnowledgeManager(8L);
    }
}
