package com.docflow.ai.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.ai.config.AiWorkspaceProperties;
import com.docflow.ai.ai.dto.AiFeedItem;
import com.docflow.ai.ai.dto.AiFollowupItem;
import com.docflow.ai.ai.dto.AiKnowledgeRecommendation;
import com.docflow.ai.ai.dto.AiReplyDraftResponse;
import com.docflow.ai.ai.dto.AiReplySuggestion;
import com.docflow.ai.ai.dto.AiWorkspaceAdoptionResponse;
import com.docflow.ai.ai.dto.AiWorkspaceOverview;
import com.docflow.ai.ai.dto.AiWorkspaceResponse;
import com.docflow.ai.auth.entity.SysUser;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.ai.service.AiWorkspaceService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiWorkspaceServiceImpl implements AiWorkspaceService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "have", "will", "your",
            "ticket", "issue", "user", "system", "please", "about", "after", "before"
    );

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final String ADOPTED_TICKETS_KEY = "docflow:ai:workspace:adoptedTickets";
    private static final String ADOPTION_KEY_PREFIX = "docflow:ai:workspace:adoption:";
    private static final String ADOPTION_FIELD_USER_ID = "userId";
    private static final String ADOPTION_FIELD_USER_NAME = "userName";
    private static final String ADOPTION_FIELD_ADOPTED_AT = "adoptedAt";
    private static final Set<String> CLAIM_ACTIVITY_ACTION_TYPES = Set.of("ASSIGN", "STATUS_CHANGE", "CLOSE");
    private final TicketMapper ticketMapper;
    private final TicketCommentMapper ticketCommentMapper;
    private final TicketRecordMapper ticketRecordMapper;
    private final KnowledgeArticleMapper knowledgeArticleMapper;
    private final SysUserMapper sysUserMapper;
    private final UserAccessService userAccessService;
    private final StringRedisTemplate stringRedisTemplate;
    private final AiWorkspaceProperties aiWorkspaceProperties;

    @Override
    public AiWorkspaceResponse getWorkspace(Long userId) {
        userAccessService.requireAiCenterAccess(userId);

        List<Ticket> tickets = listTickets();
        Map<Long, List<TicketComment>> commentsByTicketId = listCommentsByTicketId(tickets);
        Map<Long, List<TicketRecord>> recordsByTicketId = listRecordsByTicketId(tickets);
        Map<Long, List<KnowledgeArticle>> articlesByTicketId = listArticlesByTicketId(tickets);
        Set<Long> adoptedTicketIds = listAdoptedTicketIds();
        Map<Long, AdoptionRecord> adoptionByTicketId = listAdoptionByTicketId(adoptedTicketIds);
        Map<Long, LocalDateTime> activityByTicketId = buildActivityByTicketId(tickets, commentsByTicketId, recordsByTicketId, articlesByTicketId, adoptionByTicketId);

        List<Ticket> pendingTickets = tickets.stream()
                .filter(ticket -> ticket.getStatus() != null && ticket.getStatus() < 3)
                .toList();
        List<Ticket> resolvedTickets = tickets.stream()
                .filter(ticket -> ticket.getStatus() != null && ticket.getStatus() >= 3)
                .toList();

        Ticket primaryTicket = pendingTickets.stream()
                .max(Comparator
                        .comparing(Ticket::getPriority, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Ticket::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .orElseGet(() -> tickets.stream()
                        .max(Comparator.comparing(Ticket::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                        .orElse(null));

        AiWorkspaceResponse response = new AiWorkspaceResponse();
        response.setGeneratedAt(LocalDateTime.now());
        response.setHeuristicBased(true);
        response.setOverview(buildOverview(pendingTickets, resolvedTickets, articlesByTicketId, adoptedTicketIds));
        response.setPrimarySuggestion(buildPrimarySuggestion(primaryTicket, commentsByTicketId, articlesByTicketId, adoptedTicketIds, adoptionByTicketId, activityByTicketId));
        response.setRecommendations(buildRecommendations(primaryTicket, articlesByTicketId, commentsByTicketId));
        response.setFeed(buildFeed(pendingTickets, resolvedTickets, articlesByTicketId, commentsByTicketId));
        response.setFollowups(buildFollowups(pendingTickets, resolvedTickets, articlesByTicketId, commentsByTicketId, adoptedTicketIds, adoptionByTicketId, activityByTicketId));
        response.setAdoptedTicketIds(adoptedTicketIds);
        return response;
    }

    @Override
    public AiReplyDraftResponse getReplyDraft(Long userId, Long ticketId) {
        userAccessService.requireAiCenterAccess(userId);
        Ticket ticket = requireTicket(ticketId);
        List<TicketComment> comments = listCommentsByTicketId(List.of(ticket)).getOrDefault(ticketId, List.of());
        Map<Long, List<KnowledgeArticle>> articlesByTicketId = listArticlesByTicketId(List.of(ticket));
        List<KnowledgeArticle> linkedArticles = articlesByTicketId.getOrDefault(ticketId, List.of());
        TicketComment latestComment = comments.isEmpty() ? null : comments.get(comments.size() - 1);
        Set<Long> adoptedTicketIds = listAdoptedTicketIds();
        AdoptionRecord adoptionRecord = adoptedTicketIds.contains(ticket.getId()) ? getAdoptionRecord(ticket.getId()) : null;
        List<TicketRecord> records = listRecordsByTicketId(List.of(ticket)).getOrDefault(ticket.getId(), List.of());
        LocalDateTime lastActivityAt = resolveLastActivity(ticket, comments, records, linkedArticles, adoptionRecord);

        AiReplyDraftResponse response = new AiReplyDraftResponse();
        response.setTicketId(ticket.getId());
        response.setStatusKey(resolveTicketStatusKey(ticket, comments, linkedArticles));
        response.setAdopted(adoptedTicketIds.contains(ticket.getId()));
        applyAdoptionDetails(response, adoptionRecord, lastActivityAt);
        response.setTicketNo(ticket.getTicketNo());
        response.setTicketTitle(ticket.getTitle());
        response.setScene(ticket.getType() + " / " + statusLabel(ticket.getStatus()));
        response.setConfidence(resolveConfidence(comments.size(), linkedArticles.size()));
        response.setOpener(buildOpener(ticket));
        response.setDiagnosis(buildDiagnosis(ticket, latestComment, linkedArticles));
        response.setNextStep(buildNextStep(ticket, latestComment));
        response.setCustomerReply(buildCustomerReply(ticket, latestComment, linkedArticles));
        response.setOperatorNotes(buildOperatorNotes(ticket, comments, linkedArticles));
        response.setRelatedKnowledge(buildRecommendations(ticket, articlesByTicketId, Map.of(ticketId, comments)));
        return response;
    }

    @Override
    public AiWorkspaceAdoptionResponse markReplyDraftAdopted(Long userId, Long ticketId) {
        userAccessService.requireAiCenterAccess(userId);
        requireTicket(ticketId);
        AdoptionRecord record = persistAdoptedTicketId(ticketId, userId);

        AiWorkspaceAdoptionResponse response = new AiWorkspaceAdoptionResponse();
        response.setTicketId(ticketId);
        response.setAdopted(true);
        applyAdoptionDetails(response, record, record == null ? null : record.adoptedAt());
        return response;
    }

    @Override
    public AiWorkspaceAdoptionResponse unmarkReplyDraftAdopted(Long userId, Long ticketId) {
        userAccessService.requireAiCenterAccess(userId);
        requireTicket(ticketId);
        removeAdoptedTicketId(ticketId);

        AiWorkspaceAdoptionResponse response = new AiWorkspaceAdoptionResponse();
        response.setTicketId(ticketId);
        response.setAdopted(false);
        return response;
    }

    private List<Ticket> listTickets() {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getDeleted, 0)
                .orderByDesc(Ticket::getUpdateTime, Ticket::getCreateTime);
        return ticketMapper.selectList(wrapper);
    }

    private Ticket requireTicket(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ticket;
    }

    private Map<Long, List<TicketComment>> listCommentsByTicketId(List<Ticket> tickets) {
        Map<Long, List<TicketComment>> grouped = new HashMap<>();
        List<Long> ticketIds = tickets.stream()
                .map(Ticket::getId)
                .toList();
        if (ticketIds.isEmpty()) {
            return grouped;
        }

        LambdaQueryWrapper<TicketComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TicketComment::getTicketId, ticketIds)
                .eq(TicketComment::getDeleted, 0)
                .orderByAsc(TicketComment::getCreateTime);

        for (TicketComment comment : ticketCommentMapper.selectList(wrapper)) {
            grouped.computeIfAbsent(comment.getTicketId(), key -> new ArrayList<>()).add(comment);
        }
        return grouped;
    }

    private Map<Long, List<KnowledgeArticle>> listArticlesByTicketId(List<Ticket> tickets) {
        Map<Long, List<KnowledgeArticle>> grouped = new HashMap<>();
        List<Long> ticketIds = tickets.stream()
                .map(Ticket::getId)
                .toList();
        if (ticketIds.isEmpty()) {
            return grouped;
        }

        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeArticle::getSourceTicketId, ticketIds)
                .eq(KnowledgeArticle::getDeleted, 0)
                .orderByDesc(KnowledgeArticle::getPublishTime, KnowledgeArticle::getUpdateTime, KnowledgeArticle::getCreateTime);

        for (KnowledgeArticle article : knowledgeArticleMapper.selectList(wrapper)) {
            if (article.getSourceTicketId() == null) {
                continue;
            }
            grouped.computeIfAbsent(article.getSourceTicketId(), key -> new ArrayList<>()).add(article);
        }
        return grouped;
    }

    private Map<Long, List<TicketRecord>> listRecordsByTicketId(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            return Map.of();
        }
        List<Long> ticketIds = tickets.stream().map(Ticket::getId).toList();
        LambdaQueryWrapper<TicketRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TicketRecord::getTicketId, ticketIds)
                .orderByAsc(TicketRecord::getCreateTime, TicketRecord::getId);

        Map<Long, List<TicketRecord>> grouped = new HashMap<>();
        for (TicketRecord record : ticketRecordMapper.selectList(wrapper)) {
            grouped.computeIfAbsent(record.getTicketId(), key -> new ArrayList<>()).add(record);
        }
        return grouped;
    }

    private Map<Long, LocalDateTime> buildActivityByTicketId(List<Ticket> tickets,
                                                             Map<Long, List<TicketComment>> commentsByTicketId,
                                                             Map<Long, List<TicketRecord>> recordsByTicketId,
                                                             Map<Long, List<KnowledgeArticle>> articlesByTicketId,
                                                             Map<Long, AdoptionRecord> adoptionByTicketId) {
        Map<Long, LocalDateTime> activityByTicketId = new HashMap<>();
        for (Ticket ticket : tickets) {
            activityByTicketId.put(
                    ticket.getId(),
                    resolveLastActivity(
                            ticket,
                            commentsByTicketId.getOrDefault(ticket.getId(), List.of()),
                            recordsByTicketId.getOrDefault(ticket.getId(), List.of()),
                            articlesByTicketId.getOrDefault(ticket.getId(), List.of()),
                            adoptionByTicketId.get(ticket.getId())
                    )
            );
        }
        return activityByTicketId;
    }

    private AiWorkspaceOverview buildOverview(List<Ticket> pendingTickets,
                                              List<Ticket> resolvedTickets,
                                              Map<Long, List<KnowledgeArticle>> articlesByTicketId,
                                              Set<Long> adoptedTicketIds) {
        int resolvedKnowledgeLinked = (int) resolvedTickets.stream()
                .filter(ticket -> !articlesByTicketId.getOrDefault(ticket.getId(), List.of()).isEmpty())
                .count();
        int activeAdopted = (int) pendingTickets.stream()
                .filter(ticket -> adoptedTicketIds.contains(ticket.getId()))
                .count();
        int knowledgeRecommendations = (int) articlesByTicketId.values().stream()
                .flatMap(Collection::stream)
                .count();

        AiWorkspaceOverview overview = new AiWorkspaceOverview();
        overview.setPendingSuggestions(pendingTickets.size());
        overview.setAdoptedSuggestions(resolvedKnowledgeLinked + activeAdopted);
        overview.setKnowledgeRecommendations(knowledgeRecommendations);
        return overview;
    }

    private AiReplySuggestion buildPrimarySuggestion(Ticket ticket,
                                                     Map<Long, List<TicketComment>> commentsByTicketId,
                                                     Map<Long, List<KnowledgeArticle>> articlesByTicketId,
                                                     Set<Long> adoptedTicketIds,
                                                     Map<Long, AdoptionRecord> adoptionByTicketId,
                                                     Map<Long, LocalDateTime> activityByTicketId) {
        AiReplySuggestion suggestion = new AiReplySuggestion();
        if (ticket == null) {
            suggestion.setTitle("No active ticket needs a reply suggestion");
            suggestion.setSummary("The current workspace does not have an open ticket that needs an AI-first response draft.");
            suggestion.setScene("workspace idle");
            suggestion.setStatusKey("idle");
            suggestion.setAdopted(false);
            suggestion.setConfidence("Medium");
            suggestion.setChecklist(List.of(
                    "Confirm whether a new high-priority ticket has arrived",
                    "Review recent ticket comments before drafting a reply",
                    "Check if an existing knowledge article should be surfaced first"
            ));
            return suggestion;
        }

        List<TicketComment> comments = commentsByTicketId.getOrDefault(ticket.getId(), List.of());
        List<KnowledgeArticle> linkedArticles = articlesByTicketId.getOrDefault(ticket.getId(), List.of());
        TicketComment latestComment = comments.isEmpty() ? null : comments.get(comments.size() - 1);

        suggestion.setTicketId(ticket.getId());
        suggestion.setStatusKey(resolveTicketStatusKey(ticket, comments, linkedArticles));
        suggestion.setAdopted(adoptedTicketIds.contains(ticket.getId()));
        applyAdoptionDetails(suggestion, adoptionByTicketId.get(ticket.getId()), activityByTicketId.get(ticket.getId()));
        suggestion.setTicketNo(ticket.getTicketNo());
        suggestion.setTitle(ticket.getTitle());
        suggestion.setSummary(buildReplySummary(ticket, latestComment, linkedArticles));
        suggestion.setScene(ticket.getType() + " / " + statusLabel(ticket.getStatus()));
        suggestion.setConfidence(resolveConfidence(comments.size(), linkedArticles.size()));
        suggestion.setChecklist(buildChecklist(ticket, latestComment, linkedArticles));
        return suggestion;
    }

    private String buildReplySummary(Ticket ticket, TicketComment latestComment, List<KnowledgeArticle> linkedArticles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Acknowledge the reported issue in ").append(ticket.getTicketNo())
                .append(", confirm impact scope, and commit to the next investigation checkpoint.");
        if (latestComment != null && latestComment.getContent() != null && !latestComment.getContent().isBlank()) {
            builder.append(" Reference the latest internal context: ")
                    .append(clip(latestComment.getContent(), 96))
                    .append('.');
        }
        if (!linkedArticles.isEmpty()) {
            builder.append(" Reuse the existing knowledge context before sending the external reply.");
        }
        return builder.toString();
    }

    private String buildOpener(Ticket ticket) {
        return "Thanks for reporting " + ticket.getTicketNo()
                + ". We have started checking the issue and are aligning the next troubleshooting checkpoint now.";
    }

    private String buildDiagnosis(Ticket ticket, TicketComment latestComment, List<KnowledgeArticle> linkedArticles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Current assessment for ").append(ticket.getTicketNo())
                .append(" is based on the ticket description");
        if (latestComment != null && latestComment.getContent() != null && !latestComment.getContent().isBlank()) {
            builder.append(" and the latest operator note: ")
                    .append(clip(latestComment.getContent(), 120));
        } else {
            builder.append(" and the existing ticket context already on record");
        }
        if (!linkedArticles.isEmpty()) {
            builder.append(". Similar knowledge already exists and should be reused before sending a final answer");
        } else {
            builder.append(". No linked knowledge article has been attached yet");
        }
        builder.append(".");
        return builder.toString();
    }

    private String buildNextStep(Ticket ticket, TicketComment latestComment) {
        if (latestComment != null && latestComment.getCreateTime() != null) {
            return "Confirm the affected scope, continue the current investigation thread, and send the next update after "
                    + latestComment.getCreateTime().format(TIME_FORMATTER) + ".";
        }
        return "Confirm the affected scope, add the first internal troubleshooting note, and promise the next update window.";
    }

    private String buildCustomerReply(Ticket ticket, TicketComment latestComment, List<KnowledgeArticle> linkedArticles) {
        StringBuilder builder = new StringBuilder();
        builder.append("Hello, we have received your report for ").append(ticket.getTicketNo()).append(". ");
        builder.append("Our team is currently checking the affected scope and tracing the most likely failure path. ");
        if (latestComment != null && latestComment.getContent() != null && !latestComment.getContent().isBlank()) {
            builder.append("Based on the latest internal review, we are already verifying ")
                    .append(clip(latestComment.getContent(), 88))
                    .append(". ");
        }
        builder.append("We will share the next update as soon as the current troubleshooting checkpoint is confirmed.");
        if (!linkedArticles.isEmpty()) {
            builder.append(" We are also reusing the closest existing knowledge guidance to shorten turnaround time.");
        }
        return builder.toString();
    }

    private List<String> buildOperatorNotes(Ticket ticket, List<TicketComment> comments, List<KnowledgeArticle> linkedArticles) {
        List<String> notes = new ArrayList<>();
        notes.add("Ticket priority: " + ticket.getPriority());
        notes.add("Ticket status: " + statusLabel(ticket.getStatus()));
        notes.add("Internal comments collected: " + comments.size());
        if (!linkedArticles.isEmpty()) {
            notes.add("Closest knowledge article: " + linkedArticles.get(0).getTitle());
        } else {
            notes.add("No linked knowledge article yet. Consider drafting one after resolution.");
        }
        return notes;
    }

    private String resolveConfidence(int commentCount, int linkedArticleCount) {
        if (linkedArticleCount > 0 || commentCount >= 2) {
            return "High";
        }
        if (commentCount == 1) {
            return "Medium";
        }
        return "Medium";
    }

    private List<String> buildChecklist(Ticket ticket, TicketComment latestComment, List<KnowledgeArticle> linkedArticles) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Confirm business impact, affected scope, and urgency before replying");
        checklist.add("State the next concrete troubleshooting step instead of only apologizing");
        if (latestComment != null && latestComment.getCreateTime() != null) {
            checklist.add("Synchronize the next update window after " + latestComment.getCreateTime().format(TIME_FORMATTER));
        } else {
            checklist.add("Set an explicit next-update time in the first reply draft");
        }
        if (!linkedArticles.isEmpty()) {
            checklist.add("Link the closest existing knowledge article to reduce repeated explanation");
        }
        return checklist;
    }

    private List<AiKnowledgeRecommendation> buildRecommendations(Ticket primaryTicket,
                                                                 Map<Long, List<KnowledgeArticle>> articlesByTicketId,
                                                                 Map<Long, List<TicketComment>> commentsByTicketId) {
        List<KnowledgeArticle> candidateArticles = articlesByTicketId.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(KnowledgeArticle::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .toList();

        Set<String> keywords = primaryTicket == null
                ? Set.of()
                : extractKeywords(primaryTicket.getTitle() + " " + nullSafe(primaryTicket.getContent()) + " "
                + commentsByTicketId.getOrDefault(primaryTicket.getId(), List.of()).stream()
                .map(TicketComment::getContent)
                .collect(Collectors.joining(" ")));

        return candidateArticles.stream()
                .sorted(Comparator.comparingInt((KnowledgeArticle article) -> scoreArticle(article, keywords)).reversed()
                        .thenComparing(KnowledgeArticle::getUpdateTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(3)
                .map(article -> toRecommendation(article, scoreArticle(article, keywords), primaryTicket))
                .toList();
    }

    private AiKnowledgeRecommendation toRecommendation(KnowledgeArticle article, int score, Ticket primaryTicket) {
        AiKnowledgeRecommendation recommendation = new AiKnowledgeRecommendation();
        recommendation.setArticleId(article.getId());
        recommendation.setTitle(article.getTitle());
        if (article.getSourceTicketId() != null && primaryTicket != null && article.getSourceTicketId().equals(primaryTicket.getId())) {
            recommendation.setReason("This article already originated from the same ticket and is the closest reusable context.");
        } else if (score > 0) {
            recommendation.setReason("The title and summary overlap with the current ticket context, so it is a strong reuse candidate.");
        } else {
            recommendation.setReason("This is one of the freshest knowledge assets available for operator reuse.");
        }
        int boundedScore = score > 0 ? Math.min(95, 70 + score * 8) : 68;
        recommendation.setMatchRate(boundedScore + "% match");
        return recommendation;
    }

    private List<AiFeedItem> buildFeed(List<Ticket> pendingTickets,
                                       List<Ticket> resolvedTickets,
                                       Map<Long, List<KnowledgeArticle>> articlesByTicketId,
                                       Map<Long, List<TicketComment>> commentsByTicketId) {
        int openWithoutComments = (int) pendingTickets.stream()
                .filter(ticket -> commentsByTicketId.getOrDefault(ticket.getId(), List.of()).isEmpty())
                .count();
        int knowledgeLinked = (int) resolvedTickets.stream()
                .filter(ticket -> !articlesByTicketId.getOrDefault(ticket.getId(), List.of()).isEmpty())
                .count();

        return List.of(
                feedItem("Pending reply suggestions", String.valueOf(pendingTickets.size())),
                feedItem("Resolved tickets", String.valueOf(resolvedTickets.size())),
                feedItem("Open tickets without comments", String.valueOf(openWithoutComments)),
                feedItem("Knowledge-linked tickets", String.valueOf(knowledgeLinked))
        );
    }

    private List<AiFollowupItem> buildFollowups(List<Ticket> pendingTickets,
                                                List<Ticket> resolvedTickets,
                                                Map<Long, List<KnowledgeArticle>> articlesByTicketId,
                                                Map<Long, List<TicketComment>> commentsByTicketId,
                                                Set<Long> adoptedTicketIds,
                                                Map<Long, AdoptionRecord> adoptionByTicketId,
                                                Map<Long, LocalDateTime> activityByTicketId) {
        List<AiFollowupItem> items = new ArrayList<>();

        pendingTickets.stream()
                .filter(ticket -> commentsByTicketId.getOrDefault(ticket.getId(), List.of()).isEmpty())
                .sorted(Comparator.comparing(Ticket::getPriority, Comparator.nullsLast(Integer::compareTo)).reversed())
                .limit(2)
                .forEach(ticket -> items.add(followup(
                        ticket.getId(),
                        "active-reply",
                        "needs-reply",
                        adoptedTicketIds.contains(ticket.getId()),
                        adoptionByTicketId.get(ticket.getId()),
                        activityByTicketId.get(ticket.getId()),
                        ticket.getTitle(),
                        "No ticket comment has been added yet. Draft the first reply and set the next update commitment.",
                        "Needs reply",
                        "chip-orange"
                )));

        resolvedTickets.stream()
                .filter(ticket -> articlesByTicketId.getOrDefault(ticket.getId(), List.of()).isEmpty())
                .limit(Math.max(0, 3 - items.size()))
                .forEach(ticket -> items.add(followup(
                        ticket.getId(),
                        "knowledge-capture",
                        "needs-knowledge",
                        adoptedTicketIds.contains(ticket.getId()),
                        adoptionByTicketId.get(ticket.getId()),
                        activityByTicketId.get(ticket.getId()),
                        ticket.getTitle(),
                        "The ticket is already resolved but has not been turned into a reusable knowledge draft yet.",
                        "Needs knowledge",
                        "chip-blue"
                )));

        if (items.isEmpty()) {
            items.add(followup(
                    null,
                    "stable",
                    "stable",
                    false,
                    null,
                    null,
                    "AI workspace follow-up queue is clear",
                    "No immediate reply or knowledge follow-up was detected from the current ticket set.",
                    "Stable",
                    "chip-green"
            ));
        }

        return items;
    }

    private AiFeedItem feedItem(String title, String value) {
        AiFeedItem item = new AiFeedItem();
        item.setTitle(title);
        item.setValue(value);
        return item;
    }

    private AiFollowupItem followup(Long ticketId,
                                    String queueKey,
                                    String statusKey,
                                    boolean adopted,
                                    AdoptionRecord adoptionRecord,
                                    LocalDateTime lastActivityAt,
                                    String title,
                                    String desc,
                                    String chip,
                                    String chipClass) {
        AiFollowupItem item = new AiFollowupItem();
        item.setTicketId(ticketId);
        item.setQueueKey(queueKey);
        item.setStatusKey(statusKey);
        item.setAdopted(adopted);
        applyAdoptionDetails(item, adoptionRecord, lastActivityAt);
        item.setTitle(title);
        item.setDesc(desc);
        item.setChip(chip);
        item.setChipClass(chipClass);
        return item;
    }

    private Set<Long> listAdoptedTicketIds() {
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(ADOPTED_TICKETS_KEY);
            if (members == null || members.isEmpty()) {
                return Set.of();
            }
            return members.stream()
                    .map(this::parseTicketId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (RedisConnectionFailureException ex) {
            return Set.of();
        }
    }

    private Map<Long, AdoptionRecord> listAdoptionByTicketId(Set<Long> ticketIds) {
        if (ticketIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AdoptionRecord> adoptionByTicketId = new HashMap<>();
        for (Long ticketId : ticketIds) {
            AdoptionRecord record = getAdoptionRecord(ticketId);
            if (record != null) {
                adoptionByTicketId.put(ticketId, record);
            }
        }
        return adoptionByTicketId;
    }

    private AdoptionRecord persistAdoptedTicketId(Long ticketId, Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        String userName = resolveUserDisplayName(user, userId);
        LocalDateTime adoptedAt = LocalDateTime.now();
        try {
            stringRedisTemplate.opsForSet().add(ADOPTED_TICKETS_KEY, String.valueOf(ticketId));
            stringRedisTemplate.opsForHash().put(adoptionKey(ticketId), ADOPTION_FIELD_USER_ID, String.valueOf(userId));
            stringRedisTemplate.opsForHash().put(adoptionKey(ticketId), ADOPTION_FIELD_USER_NAME, userName);
            stringRedisTemplate.opsForHash().put(adoptionKey(ticketId), ADOPTION_FIELD_ADOPTED_AT, adoptedAt.toString());
        } catch (RedisConnectionFailureException ex) {
            throw new BusinessException(ResultCode.ERROR, "AI workspace adoption state is temporarily unavailable.");
        }
        return new AdoptionRecord(userId, userName, adoptedAt);
    }

    private void removeAdoptedTicketId(Long ticketId) {
        try {
            stringRedisTemplate.opsForSet().remove(ADOPTED_TICKETS_KEY, String.valueOf(ticketId));
            stringRedisTemplate.delete(adoptionKey(ticketId));
        } catch (RedisConnectionFailureException ex) {
            throw new BusinessException(ResultCode.ERROR, "AI workspace adoption state is temporarily unavailable.");
        }
    }

    private AdoptionRecord getAdoptionRecord(Long ticketId) {
        try {
            Object userIdValue = stringRedisTemplate.opsForHash().get(adoptionKey(ticketId), ADOPTION_FIELD_USER_ID);
            Object userNameValue = stringRedisTemplate.opsForHash().get(adoptionKey(ticketId), ADOPTION_FIELD_USER_NAME);
            Object adoptedAtValue = stringRedisTemplate.opsForHash().get(adoptionKey(ticketId), ADOPTION_FIELD_ADOPTED_AT);
            if (userIdValue == null && userNameValue == null && adoptedAtValue == null) {
                return null;
            }
            return new AdoptionRecord(
                    parseTicketId(String.valueOf(userIdValue)),
                    userNameValue == null ? null : String.valueOf(userNameValue),
                    parseLocalDateTime(adoptedAtValue == null ? null : String.valueOf(adoptedAtValue))
            );
        } catch (RedisConnectionFailureException ex) {
            return null;
        }
    }

    private void applyClaimActivity(Consumer<LocalDateTime> lastActivityConsumer,
                                    Consumer<String> freshnessConsumer,
                                    AdoptionRecord record,
                                    LocalDateTime lastActivityAt) {
        if (record == null) {
            return;
        }
        LocalDateTime effectiveLastActivityAt = lastActivityAt == null ? record.adoptedAt() : lastActivityAt;
        lastActivityConsumer.accept(effectiveLastActivityAt);
        freshnessConsumer.accept(resolveClaimFreshness(record.adoptedAt(), effectiveLastActivityAt));
    }

    LocalDateTime resolveLastActivity(Ticket ticket,
                                      List<TicketComment> comments,
                                      List<TicketRecord> records,
                                      List<KnowledgeArticle> linkedArticles,
                                      AdoptionRecord adoptionRecord) {
        LocalDateTime latest = adoptionRecord == null ? null : adoptionRecord.adoptedAt();
        for (TicketComment comment : comments) {
            if (isClaimActivityComment(ticket, comment)) {
                latest = laterOf(latest, laterOf(comment.getUpdateTime(), comment.getCreateTime()));
            }
        }
        for (TicketRecord record : records) {
            if (isClaimActivityRecord(record)) {
                latest = laterOf(latest, record.getCreateTime());
            }
        }
        for (KnowledgeArticle article : linkedArticles) {
            latest = laterOf(latest, laterOf(article.getUpdateTime(), laterOf(article.getPublishTime(), article.getCreateTime())));
        }
        return latest;
    }

    boolean isClaimActivityComment(Ticket ticket, TicketComment comment) {
        if (comment == null || Integer.valueOf(1).equals(comment.getDeleted())) {
            return false;
        }
        if (Integer.valueOf(1).equals(comment.getIsInternal())) {
            return true;
        }
        if (Integer.valueOf(2).equals(comment.getCommentType()) || Integer.valueOf(3).equals(comment.getCommentType())) {
            return true;
        }
        return ticket != null
                && ticket.getSubmitUserId() != null
                && comment.getUserId() != null
                && !ticket.getSubmitUserId().equals(comment.getUserId());
    }

    boolean isClaimActivityRecord(TicketRecord record) {
        return record != null && CLAIM_ACTIVITY_ACTION_TYPES.contains(record.getActionType());
    }

    String resolveClaimFreshness(LocalDateTime adoptedAt, LocalDateTime lastActivityAt) {
        LocalDateTime freshnessBase = laterOf(lastActivityAt, adoptedAt);
        if (freshnessBase == null) {
            return "unknown";
        }
        return freshnessBase.isBefore(LocalDateTime.now().minus(aiWorkspaceProperties.getClaimStaleAfter())) ? "stale" : "fresh";
    }

    private LocalDateTime laterOf(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private void applyAdoptionDetails(AiReplyDraftResponse response, AdoptionRecord record, LocalDateTime lastActivityAt) {
        applyClaimActivity(response::setLastActivityAt, response::setClaimFreshness, record, lastActivityAt);
        if (record == null) {
            return;
        }
        response.setAdoptedByUserId(record.userId());
        response.setAdoptedByName(record.userName());
        response.setAdoptedAt(record.adoptedAt());
    }

    private void applyAdoptionDetails(AiReplySuggestion response, AdoptionRecord record, LocalDateTime lastActivityAt) {
        applyClaimActivity(response::setLastActivityAt, response::setClaimFreshness, record, lastActivityAt);
        if (record == null) {
            return;
        }
        response.setAdoptedByUserId(record.userId());
        response.setAdoptedByName(record.userName());
        response.setAdoptedAt(record.adoptedAt());
    }

    private void applyAdoptionDetails(AiFollowupItem response, AdoptionRecord record, LocalDateTime lastActivityAt) {
        applyClaimActivity(response::setLastActivityAt, response::setClaimFreshness, record, lastActivityAt);
        if (record == null) {
            return;
        }
        response.setAdoptedByUserId(record.userId());
        response.setAdoptedByName(record.userName());
        response.setAdoptedAt(record.adoptedAt());
    }

    private void applyAdoptionDetails(AiWorkspaceAdoptionResponse response, AdoptionRecord record, LocalDateTime lastActivityAt) {
        applyClaimActivity(response::setLastActivityAt, response::setClaimFreshness, record, lastActivityAt);
        if (record == null) {
            return;
        }
        response.setAdoptedByUserId(record.userId());
        response.setAdoptedByName(record.userName());
        response.setAdoptedAt(record.adoptedAt());
    }

    private String resolveUserDisplayName(SysUser user, Long userId) {
        if (user == null) {
            return "User #" + userId;
        }
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "User #" + userId;
    }

    private String adoptionKey(Long ticketId) {
        return ADOPTION_KEY_PREFIX + ticketId;
    }

    private Long parseTicketId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    record AdoptionRecord(Long userId, String userName, LocalDateTime adoptedAt) {
    }

    private String resolveTicketStatusKey(Ticket ticket,
                                          List<TicketComment> comments,
                                          List<KnowledgeArticle> linkedArticles) {
        if (ticket == null) {
            return "idle";
        }
        boolean resolved = ticket.getStatus() != null && ticket.getStatus() >= 3;
        if (resolved && linkedArticles.isEmpty()) {
            return "needs-knowledge";
        }
        if (comments.isEmpty()) {
            return "needs-reply";
        }
        if (resolved) {
            return "knowledge-linked";
        }
        return "reply-in-progress";
    }

    private int scoreArticle(KnowledgeArticle article, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        String haystack = (nullSafe(article.getTitle()) + " " + nullSafe(article.getSummary()) + " " + nullSafe(article.getContent()))
                .toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            if (haystack.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 4)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String clip(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String statusLabel(Integer status) {
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
        return "Status " + status;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
