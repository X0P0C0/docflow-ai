package com.docflow.ai.knowledge.service;

import com.docflow.ai.knowledge.dto.KnowledgeArticleQuery;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.knowledge.dto.CreateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.dto.UpdateKnowledgeArticleRequest;

import java.util.List;

public interface KnowledgeArticleService {

    List<KnowledgeArticleResponse> listArticles(KnowledgeArticleQuery query);

    java.util.Map<Long, Long> countArticlesBySourceTickets(java.util.List<Long> sourceTicketIds);

    KnowledgeArticleResponse getArticleById(Long id);

    KnowledgeArticleResponse createArticle(Long userId, CreateKnowledgeArticleRequest request);

    KnowledgeArticleResponse updateArticle(Long id, Long userId, UpdateKnowledgeArticleRequest request);

    KnowledgeArticleResponse restoreArticleVersion(Long id, Long versionId, Long userId);

    KnowledgeArticleResponse archiveArticle(Long id, Long userId);

    void deleteArticle(Long id, Long userId);
}
