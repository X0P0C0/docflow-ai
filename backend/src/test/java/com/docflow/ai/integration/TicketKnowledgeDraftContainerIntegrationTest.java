package com.docflow.ai.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.entity.KnowledgeArticle;
import com.docflow.ai.knowledge.entity.KnowledgeArticleVersion;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleVersionMapper;
import com.docflow.ai.support.AbstractContainerIntegrationTest;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TicketKnowledgeDraftContainerIntegrationTest extends AbstractContainerIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Autowired
    private KnowledgeArticleVersionMapper knowledgeArticleVersionMapper;

    @Test
    void shouldCreateKnowledgeDraftWithRealMysqlBackedWorkflow() {
        CreateTicketKnowledgeDraftRequest request = new CreateTicketKnowledgeDraftRequest();
        request.setOrigin("ai-center");
        request.setCloseRemark("Root cause fixed and customer informed.");

        var response = ticketService.createKnowledgeDraft(100L, 2L, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getSourceTicketId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(0);
        assertThat(response.getTitle()).contains("Payment callback failed");
        assertThat(response.getContent()).contains("Root cause fixed and customer informed.");

        LambdaQueryWrapper<KnowledgeArticle> articleWrapper = new LambdaQueryWrapper<>();
        articleWrapper.eq(KnowledgeArticle::getSourceTicketId, 100L)
                .eq(KnowledgeArticle::getDeleted, 0)
                .orderByDesc(KnowledgeArticle::getId);

        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectList(articleWrapper);
        assertThat(articles).hasSize(1);

        KnowledgeArticle article = articles.get(0);
        assertThat(article.getAuthorUserId()).isEqualTo(2L);
        assertThat(article.getStatus()).isEqualTo(0);

        LambdaQueryWrapper<KnowledgeArticleVersion> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(KnowledgeArticleVersion::getArticleId, article.getId())
                .orderByAsc(KnowledgeArticleVersion::getVersionNo);

        List<KnowledgeArticleVersion> versions = knowledgeArticleVersionMapper.selectList(versionWrapper);
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).getVersionNo()).isEqualTo(1);
        assertThat(versions.get(0).getRemark()).isNotBlank();
    }

    @Test
    void shouldRejectKnowledgeDraftWhenTicketNotReadyForCapture() {
        assertThatThrownBy(() -> ticketService.createKnowledgeDraft(101L, 2L, new CreateTicketKnowledgeDraftRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("可沉淀");
    }
}
