package com.docflow.ai.knowledge.service.impl;

import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.dto.CreateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleVersionMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

class KnowledgeArticleServiceImplTest {

    private KnowledgeArticleMapper knowledgeArticleMapper;
    private KnowledgeArticleVersionMapper knowledgeArticleVersionMapper;
    private TicketMapper ticketMapper;
    private UserAccessService userAccessService;
    private KnowledgeArticleServiceImpl service;

    @BeforeEach
    void setUp() {
        knowledgeArticleMapper = Mockito.mock(KnowledgeArticleMapper.class);
        knowledgeArticleVersionMapper = Mockito.mock(KnowledgeArticleVersionMapper.class);
        ticketMapper = Mockito.mock(TicketMapper.class);
        userAccessService = Mockito.mock(UserAccessService.class);
        service = new KnowledgeArticleServiceImpl(
                knowledgeArticleMapper,
                knowledgeArticleVersionMapper,
                ticketMapper,
                userAccessService
        );
    }

    @Test
    void createArticleShouldRequireKnowledgeManagerBeforeWriting() {
        CreateKnowledgeArticleRequest request = new CreateKnowledgeArticleRequest();
        request.setTitle("Payment troubleshooting");
        request.setContent("Check gateway logs first.");
        request.setSummary("Triage guide");
        request.setCategoryId(1L);
        request.setStatus(0);

        Mockito.doThrow(new BusinessException(ResultCode.FORBIDDEN))
                .when(userAccessService)
                .requireKnowledgeManager(5L);

        assertThatThrownBy(() -> service.createArticle(5L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());

        verify(userAccessService).requireKnowledgeManager(5L);
        Mockito.verifyNoInteractions(knowledgeArticleMapper, knowledgeArticleVersionMapper, ticketMapper);
    }
}
