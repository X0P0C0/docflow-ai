package com.docflow.ai.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.dto.CreateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.dto.KnowledgeArticleQuery;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.knowledge.dto.KnowledgeArticleSourceTicketResponse;
import com.docflow.ai.knowledge.dto.KnowledgeArticleVersionResponse;
import com.docflow.ai.knowledge.dto.UpdateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.entity.KnowledgeArticle;
import com.docflow.ai.knowledge.entity.KnowledgeArticleVersion;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleVersionMapper;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final KnowledgeArticleVersionMapper knowledgeArticleVersionMapper;
    private final TicketMapper ticketMapper;
    private final UserAccessService userAccessService;

    @Override
    public List<KnowledgeArticleResponse> listArticles(KnowledgeArticleQuery query) {
        List<Long> sourceTicketIds = resolveSourceTicketIds(query);
        if (sourceTicketIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getStatus() != null, KnowledgeArticle::getStatus, query.getStatus())
                .eq(query.getCategoryId() != null, KnowledgeArticle::getCategoryId, query.getCategoryId())
                .eq(query.getSourceTicketId() != null, KnowledgeArticle::getSourceTicketId, query.getSourceTicketId())
                .in(sourceTicketIds != null, KnowledgeArticle::getSourceTicketId, sourceTicketIds)
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(KnowledgeArticle::getTitle, query.getKeyword())
                        .or()
                        .like(KnowledgeArticle::getSummary, query.getKeyword()))
                .orderByDesc(KnowledgeArticle::getPublishTime, KnowledgeArticle::getCreateTime);

        return knowledgeArticleMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
    }

    private List<Long> resolveSourceTicketIds(KnowledgeArticleQuery query) {
        if (query == null) {
            return null;
        }
        if (query.getSourceTicketId() != null) {
            return null;
        }
        if (!StringUtils.hasText(query.getSourceTicketNo())) {
            return null;
        }

        LambdaQueryWrapper<Ticket> ticketWrapper = new LambdaQueryWrapper<>();
        ticketWrapper.eq(Ticket::getDeleted, 0)
                .like(Ticket::getTicketNo, query.getSourceTicketNo().trim())
                .select(Ticket::getId);

        List<Long> ticketIds = ticketMapper.selectList(ticketWrapper).stream()
                .map(Ticket::getId)
                .toList();
        return ticketIds.isEmpty() ? Collections.emptyList() : ticketIds;
    }

    @Override
    public Map<Long, Long> countArticlesBySourceTickets(List<Long> sourceTicketIds) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        if (sourceTicketIds == null || sourceTicketIds.isEmpty()) {
            return counts;
        }

        sourceTicketIds.stream().distinct().forEach(ticketId -> counts.put(ticketId, 0L));

        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeArticle::getSourceTicketId, counts.keySet())
                .eq(KnowledgeArticle::getDeleted, 0);

        for (KnowledgeArticle article : knowledgeArticleMapper.selectList(wrapper)) {
            Long ticketId = article.getSourceTicketId();
            if (ticketId != null) {
                counts.computeIfPresent(ticketId, (key, value) -> value + 1);
            }
        }

        return counts;
    }

    @Override
    public KnowledgeArticleResponse getArticleById(Long id) {
        KnowledgeArticle article = requireArticle(id);
        return toResponse(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleResponse createArticle(Long userId, CreateKnowledgeArticleRequest request) {
        userAccessService.requireTicketOperator(userId);

        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.getTitle().trim());
        article.setSummary(normalizeSummary(request.getSummary()));
        article.setContent(request.getContent().trim());
        article.setCategoryId(request.getCategoryId());
        article.setSourceTicketId(resolveSourceTicketId(request.getSourceTicketId()));
        article.setAuthorUserId(userId);
        article.setStatus(request.getStatus());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCollectCount(0);
        article.setPublishTime(resolvePublishTime(request.getStatus(), null));
        article.setCreateBy(userId);
        article.setUpdateBy(userId);
        article.setDeleted(0);
        knowledgeArticleMapper.insert(article);
        createVersionSnapshot(article, userId, 1, "创建初始版本");
        return getArticleById(article.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleResponse updateArticle(Long id, Long userId, UpdateKnowledgeArticleRequest request) {
        userAccessService.requireTicketOperator(userId);
        KnowledgeArticle existing = requireArticle(id);

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(id);
        update.setTitle(request.getTitle().trim());
        update.setSummary(normalizeSummary(request.getSummary()));
        update.setContent(request.getContent().trim());
        update.setCategoryId(request.getCategoryId());
        update.setSourceTicketId(resolveSourceTicketId(request.getSourceTicketId()));
        update.setStatus(request.getStatus());
        update.setPublishTime(resolvePublishTime(request.getStatus(), existing.getPublishTime()));
        update.setUpdateBy(userId);
        knowledgeArticleMapper.updateById(update);
        createVersionSnapshot(buildVersionSource(existing, update), userId, nextVersionNo(id), "更新文章内容");

        return getArticleById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleResponse restoreArticleVersion(Long id, Long versionId, Long userId) {
        userAccessService.requireTicketOperator(userId);
        KnowledgeArticle article = requireArticle(id);
        KnowledgeArticleVersion version = requireVersion(id, versionId);

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(id);
        update.setTitle(version.getTitle());
        update.setSummary(version.getSummary());
        update.setContent(version.getContent());
        update.setStatus(article.getStatus());
        update.setCategoryId(article.getCategoryId());
        update.setSourceTicketId(article.getSourceTicketId());
        update.setPublishTime(article.getPublishTime());
        update.setUpdateBy(userId);
        knowledgeArticleMapper.updateById(update);

        createVersionSnapshot(buildVersionSource(article, update), userId, nextVersionNo(id),
                "从 v" + version.getVersionNo() + " 恢复文章内容");
        return getArticleById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeArticleResponse archiveArticle(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        KnowledgeArticle article = requireArticle(id);
        if (Integer.valueOf(2).equals(article.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "文章已归档，无需重复操作");
        }

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(id);
        update.setStatus(2);
        update.setUpdateBy(userId);
        update.setSourceTicketId(article.getSourceTicketId());
        update.setPublishTime(article.getPublishTime());
        knowledgeArticleMapper.updateById(update);

        createVersionSnapshot(buildVersionSource(article, update), userId, nextVersionNo(id), "归档文章");
        return getArticleById(id);
    }

    @Override
    public void deleteArticle(Long id, Long userId) {
        userAccessService.requireTicketOperator(userId);
        requireArticle(id);

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(id);
        update.setDeleted(1);
        update.setUpdateBy(userId);
        knowledgeArticleMapper.updateById(update);
    }

    private KnowledgeArticleResponse toResponse(KnowledgeArticle article) {
        KnowledgeArticleResponse response = new KnowledgeArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setContent(article.getContent());
        response.setCategoryId(article.getCategoryId());
        response.setSourceTicketId(article.getSourceTicketId());
        response.setSourceTicket(buildSourceTicket(article.getSourceTicketId()));
        response.setAuthorUserId(article.getAuthorUserId());
        response.setStatus(article.getStatus());
        response.setViewCount(article.getViewCount());
        response.setLikeCount(article.getLikeCount());
        response.setCollectCount(article.getCollectCount());
        response.setPublishTime(article.getPublishTime());
        response.setCreateTime(article.getCreateTime());
        response.setUpdateTime(article.getUpdateTime());
        response.setVersions(listVersions(article.getId()));
        return response;
    }

    private List<KnowledgeArticleVersionResponse> listVersions(Long articleId) {
        LambdaQueryWrapper<KnowledgeArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticleVersion::getArticleId, articleId)
                .orderByDesc(KnowledgeArticleVersion::getVersionNo, KnowledgeArticleVersion::getCreateTime);

        return knowledgeArticleVersionMapper.selectList(wrapper).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    private KnowledgeArticle requireArticle(Long id) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null || Integer.valueOf(1).equals(article.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return article;
    }

    private KnowledgeArticleVersion requireVersion(Long articleId, Long versionId) {
        KnowledgeArticleVersion version = knowledgeArticleVersionMapper.selectById(versionId);
        if (version == null || !articleId.equals(version.getArticleId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return version;
    }

    private String normalizeSummary(String summary) {
        if (!StringUtils.hasText(summary)) {
            return "";
        }
        return summary.trim();
    }

    private Long resolveSourceTicketId(Long sourceTicketId) {
        if (sourceTicketId == null) {
            return null;
        }

        Ticket ticket = ticketMapper.selectById(sourceTicketId);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "来源工单不存在或已删除");
        }
        return sourceTicketId;
    }

    private LocalDateTime resolvePublishTime(Integer status, LocalDateTime currentPublishTime) {
        if (status != null && status == 1) {
            return currentPublishTime == null ? LocalDateTime.now() : currentPublishTime;
        }
        if (status != null && status == 2) {
            return currentPublishTime;
        }
        return null;
    }

    private void createVersionSnapshot(KnowledgeArticle article, Long operatorUserId, Integer versionNo, String remark) {
        KnowledgeArticleVersion version = new KnowledgeArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(versionNo);
        version.setTitle(article.getTitle());
        version.setSummary(article.getSummary());
        version.setContent(article.getContent());
        version.setOperatorUserId(operatorUserId);
        version.setRemark(remark);
        knowledgeArticleVersionMapper.insert(version);
    }

    private Integer nextVersionNo(Long articleId) {
        LambdaQueryWrapper<KnowledgeArticleVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticleVersion::getArticleId, articleId)
                .orderByDesc(KnowledgeArticleVersion::getVersionNo)
                .last("LIMIT 1");

        KnowledgeArticleVersion latest = knowledgeArticleVersionMapper.selectOne(wrapper);
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    private KnowledgeArticle buildVersionSource(KnowledgeArticle existing, KnowledgeArticle update) {
        KnowledgeArticle merged = new KnowledgeArticle();
        merged.setId(existing.getId());
        merged.setTitle(update.getTitle());
        merged.setSummary(update.getSummary());
        merged.setContent(update.getContent());
        merged.setCategoryId(update.getCategoryId());
        merged.setSourceTicketId(update.getSourceTicketId());
        merged.setAuthorUserId(existing.getAuthorUserId());
        merged.setStatus(update.getStatus());
        merged.setViewCount(existing.getViewCount());
        merged.setLikeCount(existing.getLikeCount());
        merged.setCollectCount(existing.getCollectCount());
        merged.setPublishTime(update.getPublishTime());
        return merged;
    }

    private KnowledgeArticleVersionResponse toVersionResponse(KnowledgeArticleVersion version) {
        KnowledgeArticleVersionResponse response = new KnowledgeArticleVersionResponse();
        response.setId(version.getId());
        response.setVersionNo(version.getVersionNo());
        response.setTitle(version.getTitle());
        response.setSummary(version.getSummary());
        response.setRemark(version.getRemark());
        response.setOperatorUserId(version.getOperatorUserId());
        response.setCreateTime(version.getCreateTime());
        return response;
    }

    private KnowledgeArticleSourceTicketResponse buildSourceTicket(Long sourceTicketId) {
        if (sourceTicketId == null) {
            return null;
        }

        Ticket ticket = ticketMapper.selectById(sourceTicketId);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            return null;
        }

        KnowledgeArticleSourceTicketResponse response = new KnowledgeArticleSourceTicketResponse();
        response.setId(ticket.getId());
        response.setTicketNo(ticket.getTicketNo());
        response.setTitle(ticket.getTitle());
        return response;
    }
}
