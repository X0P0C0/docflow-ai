package com.docflow.ai.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.constant.RoleCodes;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.knowledge.dto.CreateKnowledgeArticleRequest;
import com.docflow.ai.knowledge.dto.KnowledgeArticleResponse;
import com.docflow.ai.knowledge.entity.KnowledgeArticle;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.knowledge.service.KnowledgeArticleService;
import com.docflow.ai.ticket.dto.AddTicketCommentRequest;
import com.docflow.ai.ticket.dto.AssignTicketRequest;
import com.docflow.ai.ticket.dto.CreateTicketKnowledgeDraftRequest;
import com.docflow.ai.ticket.dto.CreateTicketRequest;
import com.docflow.ai.ticket.dto.TicketDetailResponse;
import com.docflow.ai.ticket.dto.TicketAssigneeOptionResponse;
import com.docflow.ai.ticket.dto.TicketCommentResponse;
import com.docflow.ai.ticket.dto.TicketListItemResponse;
import com.docflow.ai.ticket.dto.TicketLinkedKnowledgeArticleResponse;
import com.docflow.ai.ticket.dto.TicketQueryRequest;
import com.docflow.ai.ticket.dto.TicketRelatedArticleResponse;
import com.docflow.ai.ticket.dto.TicketTimelineItemResponse;
import com.docflow.ai.ticket.dto.UpdateTicketStatusRequest;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.entity.TicketComment;
import com.docflow.ai.ticket.entity.TicketRecord;
import com.docflow.ai.ticket.mapper.TicketCommentMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.docflow.ai.ticket.mapper.TicketRecordMapper;
import com.docflow.ai.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final List<String> ASSIGNABLE_ROLE_CODES = List.of(RoleCodes.ADMIN, RoleCodes.SUPPORT);
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "have", "will",
            "支付", "问题", "处理", "工单", "用户", "系统", "相关", "需要", "排查", "异常"
    );

    private final TicketMapper ticketMapper;
    private final TicketRecordMapper ticketRecordMapper;
    private final TicketCommentMapper ticketCommentMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final SysUserMapper sysUserMapper;
    private final KnowledgeArticleService knowledgeArticleService;
    private final UserAccessService userAccessService;

    @Override
    public List<TicketListItemResponse> listTickets(Long userId, TicketQueryRequest request) {
        userAccessService.requireActiveUser(userId);
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (request != null) {
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = request.getKeyword().trim();
                wrapper.and(q -> q.like(Ticket::getTitle, keyword)
                        .or()
                        .like(Ticket::getTicketNo, keyword)
                        .or()
                        .like(Ticket::getContent, keyword));
            }
            if (request.getStatus() != null) {
                validateStatus(request.getStatus());
                wrapper.eq(Ticket::getStatus, request.getStatus());
            }
            if (request.getPriority() != null) {
                validatePriority(request.getPriority());
                wrapper.eq(Ticket::getPriority, request.getPriority());
            }
            if (request.getType() != null && !request.getType().trim().isEmpty()) {
                wrapper.eq(Ticket::getType, normalizeTicketType(request.getType()));
            }
            if (request.getAssigneeUserId() != null) {
                wrapper.eq(Ticket::getAssigneeUserId, request.getAssigneeUserId());
            }
        }
        applyTicketScope(wrapper, userId);
        wrapper.orderByAsc(Ticket::getStatus)
                .orderByDesc(Ticket::getPriority, Ticket::getUpdateTime);

        List<Ticket> tickets = ticketMapper.selectList(wrapper);
        Map<Long, List<KnowledgeArticle>> sourceArticleMap = buildSourceArticleMap(tickets.stream()
                .map(Ticket::getId)
                .toList());

        return tickets.stream()
                .map(ticket -> toListItem(ticket, sourceArticleMap.getOrDefault(ticket.getId(), List.of())))
                .toList();
    }

    @Override
    public TicketDetailResponse getTicketById(Long id, Long userId) {
        userAccessService.requireActiveUser(userId);
        Ticket ticket = requireAccessibleTicket(id, userId);
        List<KnowledgeArticle> linkedArticles = buildSourceArticleMap(List.of(id)).getOrDefault(id, List.of());

        TicketDetailResponse response = new TicketDetailResponse();
        copyBaseFields(ticket, response, linkedArticles);
        response.setSourceKnowledgeArticles(buildLinkedKnowledgeArticles(linkedArticles));

        LambdaQueryWrapper<TicketRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketRecord::getTicketId, id)
                .orderByAsc(TicketRecord::getCreateTime);

        response.setTimeline(ticketRecordMapper.selectList(wrapper).stream()
                .map(this::toTimelineItem)
                .toList());

        LambdaQueryWrapper<TicketComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(TicketComment::getTicketId, id)
                .eq(TicketComment::getDeleted, 0)
                .orderByAsc(TicketComment::getCreateTime);

        response.setComments(ticketCommentMapper.selectList(commentWrapper).stream()
                .filter(comment -> canViewComment(comment, userId))
                .map(this::toCommentItem)
                .toList());
        response.setRelatedArticles(buildRelatedArticles(ticket));

        return response;
    }

    @Override
    public List<TicketAssigneeOptionResponse> listAssignableUsers(Long userId) {
        userAccessService.requireTicketOperator(userId);
        return sysUserMapper.selectAssignableUsers().stream()
                .map(this::toAssigneeOption)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse createTicket(Long userId, CreateTicketRequest request) {
        SysUser submitUser = userAccessService.requireActiveUser(userId);

        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo(request.getType()));
        ticket.setTitle(request.getTitle().trim());
        ticket.setContent(request.getContent().trim());
        ticket.setType(normalizeTicketType(request.getType()));
        ticket.setCategoryId(request.getCategoryId());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(1);
        ticket.setSubmitUserId(userId);
        ticket.setDeptId(submitUser.getDeptId());
        ticket.setSource("WEB");
        ticket.setCreateBy(userId);
        ticket.setUpdateBy(userId);
        ticket.setDeleted(0);
        ticketMapper.insert(ticket);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticket.getId());
        record.setOperatorUserId(userId);
        record.setActionType("CREATE");
        record.setNewStatus(1);
        record.setRemark("用户提交新工单");
        ticketRecordMapper.insert(record);

        return getTicketById(ticket.getId(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse addComment(Long ticketId, Long userId, AddTicketCommentRequest request) {
        String content = request.getContent().trim();
        Integer commentType = normalizeCommentType(request.getCommentType());
        requireAccessibleTicket(ticketId, userId);
        validateCommentPermission(userId, commentType, request.getInternal());

        TicketComment comment = new TicketComment();
        comment.setTicketId(ticketId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setCommentType(commentType);
        comment.setIsInternal(Boolean.TRUE.equals(request.getInternal()) ? 1 : 0);
        comment.setDeleted(0);
        ticketCommentMapper.insert(comment);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticketId);
        record.setOperatorUserId(userId);
        record.setActionType("COMMENT");
        record.setRemark(buildCommentRemark(commentType, content));
        ticketRecordMapper.insert(record);

        return getTicketById(ticketId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse assignTicket(Long ticketId, Long userId, AssignTicketRequest request) {
        userAccessService.requireTicketOperator(userId);
        Ticket ticket = requireAccessibleTicket(ticketId, userId);
        Long assigneeUserId = request.getAssigneeUserId();
        SysUser assignee = requireAssignableUser(assigneeUserId);

        if (assigneeUserId.equals(ticket.getAssigneeUserId())) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "当前工单已经指派给该处理人");
        }

        Ticket updateTicket = new Ticket();
        updateTicket.setId(ticketId);
        updateTicket.setAssigneeUserId(assigneeUserId);
        updateTicket.setUpdateBy(userId);
        if (ticket.getStatus() == null || ticket.getStatus() == 1) {
            updateTicket.setStatus(2);
        }
        ticketMapper.updateById(updateTicket);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticketId);
        record.setOperatorUserId(userId);
        record.setActionType("ASSIGN");
        record.setOldStatus(ticket.getStatus());
        record.setNewStatus(ticket.getStatus() == null || ticket.getStatus() == 1 ? 2 : ticket.getStatus());
        record.setRemark(buildAssignRemark(assignee, request.getRemark()));
        ticketRecordMapper.insert(record);

        return getTicketById(ticketId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketDetailResponse updateStatus(Long ticketId, Long userId, UpdateTicketStatusRequest request) {
        userAccessService.requireTicketOperator(userId);
        Ticket ticket = requireAccessibleTicket(ticketId, userId);
        Integer newStatus = request.getStatus();
        validateStatus(newStatus);

        if (newStatus.equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_CONFLICT, "工单状态未发生变化");
        }

        Integer oldStatus = ticket.getStatus();
        Ticket updateTicket = new Ticket();
        updateTicket.setId(ticketId);
        updateTicket.setStatus(newStatus);
        updateTicket.setUpdateBy(userId);

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == 3) {
            updateTicket.setActualFinishTime(now);
            updateTicket.setCloseTime(null);
        } else if (newStatus == 4) {
            updateTicket.setActualFinishTime(ticket.getActualFinishTime() == null ? now : ticket.getActualFinishTime());
            updateTicket.setCloseTime(now);
        } else {
            updateTicket.setActualFinishTime(null);
            updateTicket.setCloseTime(null);
        }

        ticketMapper.updateById(updateTicket);

        TicketRecord record = new TicketRecord();
        record.setTicketId(ticketId);
        record.setOperatorUserId(userId);
        record.setActionType(newStatus == 4 ? "CLOSE" : "STATUS_CHANGE");
        record.setOldStatus(oldStatus);
        record.setNewStatus(newStatus);
        record.setRemark(buildStatusRemark(newStatus, request.getRemark()));
        ticketRecordMapper.insert(record);

        return getTicketById(ticketId, userId);
    }

    @Override
    public KnowledgeArticleResponse createKnowledgeDraft(Long ticketId, Long userId, CreateTicketKnowledgeDraftRequest request) {
        userAccessService.requireTicketOperator(userId);
        TicketDetailResponse ticket = getTicketById(ticketId, userId);
        if (ticket.getStatus() == null || ticket.getStatus() < 3) {
            throw new BusinessException("当前工单还未进入可沉淀阶段，请先处理完成或关闭后再生成知识草稿");
        }

        KnowledgeArticle existingDraft = findExistingKnowledgeDraft(ticketId);
        if (existingDraft != null) {
            return knowledgeArticleService.getArticleById(existingDraft.getId());
        }

        CreateKnowledgeArticleRequest articleRequest = new CreateKnowledgeArticleRequest();
        articleRequest.setTitle(buildKnowledgeDraftTitle(ticket));
        articleRequest.setSummary(buildKnowledgeDraftSummary(ticket));
        articleRequest.setContent(buildKnowledgeDraftContent(ticket, request));
        articleRequest.setCategoryId(resolveKnowledgeCategoryId(ticket));
        articleRequest.setSourceTicketId(ticket.getId());
        articleRequest.setStatus(0);
        return knowledgeArticleService.createArticle(userId, articleRequest);
    }

    private TicketListItemResponse toListItem(Ticket ticket, List<KnowledgeArticle> linkedArticles) {
        TicketListItemResponse response = new TicketListItemResponse();
        copyBaseFields(ticket, response, linkedArticles);
        return response;
    }

    private void copyBaseFields(Ticket ticket, TicketListItemResponse response, List<KnowledgeArticle> linkedArticles) {
        response.setId(ticket.getId());
        response.setTicketNo(ticket.getTicketNo());
        response.setTitle(ticket.getTitle());
        response.setContent(ticket.getContent());
        response.setType(ticket.getType());
        response.setCategoryId(ticket.getCategoryId());
        response.setPriority(ticket.getPriority());
        response.setPriorityLabel(mapPriorityLabel(ticket.getPriority()));
        response.setStatus(ticket.getStatus());
        response.setStatusLabel(mapStatusLabel(ticket.getStatus()));
        response.setSubmitUserId(ticket.getSubmitUserId());
        response.setAssigneeUserId(ticket.getAssigneeUserId());
        response.setSubmitterName(mapUserName(ticket.getSubmitUserId()));
        response.setAssigneeName(mapUserName(ticket.getAssigneeUserId()));
        response.setLinkedKnowledgeArticleCount((long) linkedArticles.size());
        response.setLatestLinkedKnowledgeArticle(buildLatestLinkedArticle(linkedArticles));
        response.setCreateTime(ticket.getCreateTime());
        response.setUpdateTime(ticket.getUpdateTime());
    }

    private Map<Long, List<KnowledgeArticle>> buildSourceArticleMap(List<Long> ticketIds) {
        Map<Long, List<KnowledgeArticle>> sourceArticleMap = new HashMap<>();
        if (ticketIds == null || ticketIds.isEmpty()) {
            return sourceArticleMap;
        }

        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeArticle::getSourceTicketId, ticketIds)
                .eq(KnowledgeArticle::getDeleted, 0)
                .orderByDesc(KnowledgeArticle::getPublishTime, KnowledgeArticle::getUpdateTime, KnowledgeArticle::getId);

        for (KnowledgeArticle article : knowledgeArticleMapper.selectList(wrapper)) {
            if (article.getSourceTicketId() == null) {
                continue;
            }
            sourceArticleMap.computeIfAbsent(article.getSourceTicketId(), key -> new java.util.ArrayList<>()).add(article);
        }
        return sourceArticleMap;
    }

    private TicketLinkedKnowledgeArticleResponse buildLatestLinkedArticle(List<KnowledgeArticle> linkedArticles) {
        if (linkedArticles == null || linkedArticles.isEmpty()) {
            return null;
        }

        KnowledgeArticle article = linkedArticles.stream()
                .sorted(Comparator
                        .comparing((KnowledgeArticle item) -> item.getPublishTime() != null ? item.getPublishTime() : item.getUpdateTime(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeArticle::getId, Comparator.reverseOrder()))
                .findFirst()
                .orElse(null);
        if (article == null) {
            return null;
        }

        TicketLinkedKnowledgeArticleResponse response = new TicketLinkedKnowledgeArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setStatus(article.getStatus());
        response.setStatusLabel(mapKnowledgeArticleStatusLabel(article.getStatus()));
        response.setPublishTime(article.getPublishTime());
        response.setUpdateTime(article.getUpdateTime());
        return response;
    }

    private List<TicketLinkedKnowledgeArticleResponse> buildLinkedKnowledgeArticles(List<KnowledgeArticle> linkedArticles) {
        if (linkedArticles == null || linkedArticles.isEmpty()) {
            return List.of();
        }
        return linkedArticles.stream()
                .map(this::toLinkedKnowledgeArticle)
                .toList();
    }

    private TicketLinkedKnowledgeArticleResponse toLinkedKnowledgeArticle(KnowledgeArticle article) {
        TicketLinkedKnowledgeArticleResponse response = new TicketLinkedKnowledgeArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setStatus(article.getStatus());
        response.setStatusLabel(mapKnowledgeArticleStatusLabel(article.getStatus()));
        response.setPublishTime(article.getPublishTime());
        response.setUpdateTime(article.getUpdateTime());
        return response;
    }

    private TicketTimelineItemResponse toTimelineItem(TicketRecord record) {
        TicketTimelineItemResponse response = new TicketTimelineItemResponse();
        response.setId(record.getId());
        response.setOperatorName(mapUserName(record.getOperatorUserId()));
        response.setTitle(mapActionTitle(record.getActionType()));
        response.setDesc(record.getRemark());
        response.setCreateTime(record.getCreateTime());
        return response;
    }

    private TicketCommentResponse toCommentItem(TicketComment comment) {
        TicketCommentResponse response = new TicketCommentResponse();
        response.setId(comment.getId());
        response.setAuthorName(mapUserName(comment.getUserId()));
        response.setContent(comment.getContent());
        response.setCommentTypeLabel(mapCommentTypeLabel(comment.getCommentType()));
        response.setInternal(Integer.valueOf(1).equals(comment.getIsInternal()));
        response.setCreateTime(comment.getCreateTime());
        return response;
    }

    private List<TicketRelatedArticleResponse> buildRelatedArticles(Ticket ticket) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getDeleted, 0)
                .eq(KnowledgeArticle::getStatus, 1)
                .orderByDesc(KnowledgeArticle::getPublishTime, KnowledgeArticle::getUpdateTime);

        Map<Long, Integer> scoreMap = knowledgeArticleMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(KnowledgeArticle::getId, article -> scoreArticle(ticket, article)));

        return knowledgeArticleMapper.selectList(wrapper).stream()
                .filter(article -> scoreMap.getOrDefault(article.getId(), 0) > 0)
                .sorted(Comparator
                        .comparingInt((KnowledgeArticle article) -> scoreMap.getOrDefault(article.getId(), 0))
                        .reversed()
                        .thenComparing(KnowledgeArticle::getPublishTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(article -> toRelatedArticle(article, ticket, scoreMap.getOrDefault(article.getId(), 0)))
                .toList();
    }

    private int scoreArticle(Ticket ticket, KnowledgeArticle article) {
        int score = 0;
        String haystack = normalizeText(article.getTitle() + " " + nullSafe(article.getSummary()));
        for (String keyword : extractKeywords(ticket)) {
            if (haystack.contains(keyword)) {
                score += keyword.length() >= 4 ? 3 : 2;
            }
        }

        if (article.getCategoryId() != null && Objects.equals(article.getCategoryId(), ticket.getCategoryId())) {
            score += 2;
        }
        if ("INCIDENT".equals(ticket.getType()) && containsAny(haystack, List.of("故障", "异常", "排查", "应急"))) {
            score += 2;
        }
        if ("QUESTION".equals(ticket.getType()) && containsAny(haystack, List.of("指南", "说明", "使用"))) {
            score += 2;
        }
        if ("TASK".equals(ticket.getType()) && containsAny(haystack, List.of("流程", "发布", "操作"))) {
            score += 2;
        }
        return score;
    }

    private TicketRelatedArticleResponse toRelatedArticle(KnowledgeArticle article, Ticket ticket, int score) {
        TicketRelatedArticleResponse response = new TicketRelatedArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setReason(buildRelatedReason(ticket, article, score));
        return response;
    }

    private KnowledgeArticle findExistingKnowledgeDraft(Long ticketId) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getSourceTicketId, ticketId)
                .eq(KnowledgeArticle::getStatus, 0)
                .eq(KnowledgeArticle::getDeleted, 0)
                .orderByDesc(KnowledgeArticle::getUpdateTime, KnowledgeArticle::getId)
                .last("LIMIT 1");
        return knowledgeArticleMapper.selectOne(wrapper);
    }

    private String buildKnowledgeDraftTitle(TicketDetailResponse ticket) {
        return nullSafe(ticket.getTitle()) + "处理复盘";
    }

    private String buildKnowledgeDraftSummary(TicketDetailResponse ticket) {
        String contentPreview = nullSafe(ticket.getContent()).trim();
        if (contentPreview.length() > 48) {
            contentPreview = contentPreview.substring(0, 48) + "...";
        }

        java.util.List<String> parts = new java.util.ArrayList<>();
        parts.add("来源工单：" + (ticket.getTicketNo() != null ? ticket.getTicketNo() : "#" + ticket.getId()));
        parts.add("适用场景：" + nullSafe(ticket.getStatusLabel()) + "工单的处理复盘与经验沉淀。");
        if (!contentPreview.isEmpty()) {
            parts.add("问题概述：" + contentPreview);
        }
        return String.join(" ", parts);
    }

    private String buildKnowledgeDraftContent(TicketDetailResponse ticket, CreateTicketKnowledgeDraftRequest request) {
        String timelineSection = "";
        if (ticket.getTimeline() != null && !ticket.getTimeline().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            int index = 1;
            for (TicketTimelineItemResponse item : ticket.getTimeline().stream().limit(5).toList()) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(index++)
                        .append(". ")
                        .append(nullSafe(item.getTitle()));
                if (item.getCreateTime() != null) {
                    builder.append("（").append(formatDateTime(item.getCreateTime())).append("）");
                }
                builder.append("\n").append(nullSafe(item.getDesc()));
            }
            timelineSection = builder.toString();
        }

        String commentSection = "";
        if (ticket.getComments() != null && !ticket.getComments().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            int index = 1;
            for (TicketCommentResponse item : ticket.getComments().stream().limit(5).toList()) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(index++)
                        .append(". ")
                        .append(nullSafe(item.getAuthorName()))
                        .append(" · ")
                        .append(nullSafe(item.getCommentTypeLabel()));
                if (Boolean.TRUE.equals(item.getInternal())) {
                    builder.append(" · 内部可见");
                }
                builder.append("\n").append(nullSafe(item.getContent()));
            }
            commentSection = builder.toString();
        }

        String closeRemark = request == null ? "" : nullSafe(request.getCloseRemark()).trim();
        java.util.List<String> sections = new java.util.ArrayList<>();
        sections.add("一、问题现象\n" + valueOrDefault(ticket.getContent(), "请补充工单现象描述。"));
        sections.add("二、影响范围\n请补充受影响用户、业务链路和紧急程度。");
        sections.add("三、处理过程\n" + valueOrDefault(timelineSection, "请补充关键处理步骤、时间线和定位动作。"));
        sections.add("四、关键信息\n提交人：" + valueOrDefault(ticket.getSubmitterName(), "待补充")
                + "\n处理人：" + valueOrDefault(ticket.getAssigneeName(), "待补充")
                + "\n当前状态：" + valueOrDefault(ticket.getStatusLabel(), "待补充"));
        sections.add("五、协作记录\n" + valueOrDefault(commentSection, "请补充评论区里的关键结论、日志和内部备注。"));
        sections.add("六、结论与预防\n请补充最终根因、解决方案、预防动作和后续建议。");
        if (!closeRemark.isEmpty()) {
            sections.add("七、关闭备注\n" + closeRemark);
        }
        return String.join("\n\n", sections);
    }

    private Long resolveKnowledgeCategoryId(TicketDetailResponse ticket) {
        if (ticket.getCategoryId() != null) {
            return ticket.getCategoryId();
        }

        String content = normalizeText(ticket.getTitle() + " " + nullSafe(ticket.getContent()));
        if (containsAny(content, List.of("支付", "订单"))) {
            return 3L;
        }
        if (containsAny(content, List.of("使用", "指南", "咨询"))) {
            return 1L;
        }
        return 2L;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String buildRelatedReason(Ticket ticket, KnowledgeArticle article, int score) {
        if (article.getCategoryId() != null && Objects.equals(article.getCategoryId(), ticket.getCategoryId())) {
            return "同分类知识，适合当前处理场景";
        }
        if (score >= 6) {
            return "标题和摘要命中了当前工单关键词";
        }
        if ("INCIDENT".equals(ticket.getType())) {
            return "适合作为异常处理时的排查参考";
        }
        if ("TASK".equals(ticket.getType())) {
            return "适合作为任务执行时的操作参考";
        }
        return "适合作为当前问题的辅助说明";
    }

    private List<String> extractKeywords(Ticket ticket) {
        return List.of(ticket.getTitle(), ticket.getContent(), ticket.getType())
                .stream()
                .filter(Objects::nonNull)
                .map(this::normalizeText)
                .flatMap(text -> List.of(text.split("\\s+")).stream())
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private String normalizeText(String value) {
        return nullSafe(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+", " ")
                .trim();
    }

    private boolean containsAny(String source, List<String> candidates) {
        return candidates.stream().anyMatch(source::contains);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String mapPriorityLabel(Integer priority) {
        if (priority == null) {
            return "Unspecified";
        }
        if (priority == 4) {
            return "Critical";
        }
        if (priority == 3) {
            return "High";
        }
        if (priority == 2) {
            return "Normal";
        }
        if (priority == 1) {
            return "Low";
        }
        return "Unspecified";
    }

    private String mapStatusLabel(Integer status) {
        if (status == null) {
            return "Unknown";
        }
        if (status == 1) {
            return "New";
        }
        if (status == 2) {
            return "In Progress";
        }
        if (status == 3) {
            return "Resolved";
        }
        if (status == 4) {
            return "Closed";
        }
        return "Unknown";
    }

    private String mapUserName(Long userId) {
        if (userId == null) {
            return "Unassigned";
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            return "User#" + userId;
        }
        return getDisplayName(user);
    }

    private String mapActionTitle(String actionType) {
        if (actionType == null) {
            return "Ticket Updated";
        }
        if ("CREATE".equals(actionType)) {
            return "Ticket Created";
        }
        if ("ASSIGN".equals(actionType)) {
            return "Ticket Assigned";
        }
        if ("STATUS_CHANGE".equals(actionType)) {
            return "Status Updated";
        }
        if ("COMMENT".equals(actionType)) {
            return "Comment Added";
        }
        if ("CLOSE".equals(actionType)) {
            return "Ticket Closed";
        }
        return actionType;
    }

    private String mapCommentTypeLabel(Integer commentType) {
        if (commentType == null) {
            return "Comment";
        }
        if (commentType == 2) {
            return "Handling Note";
        }
        if (commentType == 3) {
            return "Internal Memo";
        }
        return "Comment";
    }

    private String mapKnowledgeArticleStatusLabel(Integer status) {
        if (status == null) {
            return "Unknown";
        }
        if (status == 1) {
            return "Published";
        }
        if (status == 2) {
            return "Archived";
        }
        return "Draft";
    }

    private Ticket requireTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ticket;
    }

    private Ticket requireAccessibleTicket(Long ticketId, Long userId) {
        Ticket ticket = requireTicket(ticketId);
        if (userAccessService.canOperateTickets(userId)) {
            return ticket;
        }
        if (Objects.equals(ticket.getSubmitUserId(), userId) || Objects.equals(ticket.getAssigneeUserId(), userId)) {
            return ticket;
        }
        throw new BusinessException(ResultCode.FORBIDDEN);
    }

    private Integer normalizeCommentType(Integer commentType) {
        if (commentType == null) {
            return 1;
        }
        if (commentType < 1 || commentType > 3) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "评论类型不合法");
        }
        return commentType;
    }

    private void validateCommentPermission(Long userId, Integer commentType, Boolean internal) {
        if (userAccessService.canOperateTickets(userId)) {
            return;
        }
        if (Boolean.TRUE.equals(internal) || commentType != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || status < 1 || status > 4) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "工单状态不合法");
        }
    }

    private void validatePriority(Integer priority) {
        if (priority == null || priority < 1 || priority > 4) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "工单优先级不合法");
        }
    }

    private String buildCommentRemark(Integer commentType, String content) {
        String prefix;
        if (Integer.valueOf(2).equals(commentType)) {
            prefix = "??????";
        } else if (Integer.valueOf(3).equals(commentType)) {
            prefix = "??????";
        } else {
            prefix = "????";
        }
        return prefix + "?" + abbreviate(content, 80);
    }

    private String buildStatusRemark(Integer status, String remark) {
        String trimmedRemark = remark == null ? "" : remark.trim();
        String defaultRemark = "工单状态更新为 " + mapStatusLabel(status);
        if (trimmedRemark.isEmpty()) {
            return defaultRemark;
        }
        return defaultRemark + "，备注：" + abbreviate(trimmedRemark, 80);
    }

    private String buildAssignRemark(SysUser assignee, String remark) {
        String defaultRemark = "已指派给 " + getDisplayName(assignee);
        String trimmedRemark = remark == null ? "" : remark.trim();
        if (trimmedRemark.isEmpty()) {
            return defaultRemark;
        }
        return defaultRemark + "，备注：" + abbreviate(trimmedRemark, 80);
    }

    private String abbreviate(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private String generateTicketNo(String type) {
        String normalizedType = normalizeTicketType(type);
        String prefix;
        switch (normalizedType) {
            case "INCIDENT":
                prefix = "INC";
                break;
            case "TASK":
                prefix = "TASK";
                break;
            case "QUESTION":
                prefix = "QST";
                break;
            default:
                prefix = "TICKET";
                break;
        }
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return prefix + "-" + datePart + "-" + timePart;
    }

    private String normalizeTicketType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!List.of("INCIDENT", "TASK", "QUESTION").contains(normalized)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "工单类型不合法");
        }
        return normalized;
    }

    private TicketAssigneeOptionResponse toAssigneeOption(SysUser user) {
        TicketAssigneeOptionResponse response = new TicketAssigneeOptionResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(getDisplayName(user));
        response.setRoles(sysUserMapper.selectRoleCodesByUserId(user.getId()));
        return response;
    }

    private SysUser requireAssignableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "处理人不存在或不可用");
        }

        List<String> roleCodes = userAccessService.getRoleCodes(userId);
        boolean assignable = roleCodes.stream().anyMatch(ASSIGNABLE_ROLE_CODES::contains);
        if (!assignable) {
            throw new BusinessException(ResultCode.BUSINESS_RULE_VIOLATION, "该用户不能被指派为工单处理人");
        }
        return user;
    }

    private void applyTicketScope(LambdaQueryWrapper<Ticket> wrapper, Long userId) {
        if (userAccessService.canOperateTickets(userId)) {
            return;
        }
        wrapper.and(q -> q.eq(Ticket::getSubmitUserId, userId)
                .or()
                .eq(Ticket::getAssigneeUserId, userId));
    }

    private boolean canViewComment(TicketComment comment, Long userId) {
        if (!Integer.valueOf(1).equals(comment.getIsInternal())) {
            return true;
        }
        return userAccessService.canOperateTickets(userId);
    }

    private String getDisplayName(SysUser user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
