package com.docflow.ai.ai.service.impl;

import com.docflow.ai.ai.dto.*;
import com.docflow.ai.ai.service.AiAnalysisService;
import com.docflow.ai.auth.mapper.SysUserMapper;
import com.docflow.ai.knowledge.entity.KnowledgeArticle;
import com.docflow.ai.knowledge.mapper.KnowledgeArticleMapper;
import com.docflow.ai.ticket.entity.Ticket;
import com.docflow.ai.ticket.entity.TicketComment;
import com.docflow.ai.ticket.mapper.TicketCommentMapper;
import com.docflow.ai.ticket.mapper.TicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final TicketMapper ticketMapper;
    private final TicketCommentMapper commentMapper;
    private final KnowledgeArticleMapper articleMapper;
    private final SysUserMapper userMapper;

    @Override
    public IntentAnalysisResult analyzeIntent(String title, String content) {
        IntentAnalysisResult result = new IntentAnalysisResult();
        String text = (title + " " + content).toLowerCase();

        // Keyword-based intent classification
        if (containsAny(text, "crash", "error", "fail", "bug", "broken", "down", "outage", "宕机", "崩溃", "报错", "故障")) {
            result.setTicketType("INCIDENT");
            result.setPriority(containsAny(text, "urgent", "critical", "production", "紧急", "生产") ? 1 : 2);
            result.setConfidence(0.85);
            result.setReason("Detected incident keywords indicating system failure");
        } else if (containsAny(text, "how", "help", "guide", "tutorial", "how to", "怎么", "如何", "教程", "帮助")) {
            result.setTicketType("QUESTION");
            result.setPriority(3);
            result.setConfidence(0.80);
            result.setReason("Detected question keywords indicating user needs guidance");
        } else if (containsAny(text, "feature", "request", "suggest", "improve", "add", "功能", "建议", "改进", "新增")) {
            result.setTicketType("QUESTION");
            result.setPriority(3);
            result.setConfidence(0.75);
            result.setReason("Detected feature request keywords");
        } else if (containsAny(text, "task", "implement", "deploy", "update", "upgrade", "任务", "部署", "升级")) {
            result.setTicketType("TASK");
            result.setPriority(2);
            result.setConfidence(0.70);
            result.setReason("Detected task-related keywords");
        } else {
            result.setTicketType("INCIDENT");
            result.setPriority(3);
            result.setConfidence(0.50);
            result.setReason("Unable to determine clear intent, defaulting to incident");
        }

        // Category detection
        if (containsAny(text, "login", "password", "account", "auth", "登录", "密码", "账号")) {
            result.setCategoryId(3L);
            result.setCategoryName("Account");
        } else if (containsAny(text, "payment", "pay", "charge", "refund", "支付", "退款", "扣费")) {
            result.setCategoryId(2L);
            result.setCategoryName("Payment");
        } else if (containsAny(text, "network", "connection", "wifi", "vpn", "网络", "连接")) {
            result.setCategoryId(1L);
            result.setCategoryName("Network");
        } else if (containsAny(text, "slow", "performance", "timeout", "延迟", "慢", "超时")) {
            result.setCategoryId(4L);
            result.setCategoryName("Performance");
        }

        return result;
    }

    @Override
    public SentimentResult analyzeSentiment(String content) {
        SentimentResult result = new SentimentResult();
        String text = content.toLowerCase();

        int positiveScore = countMatches(text, "thank", "great", "good", "excellent", "happy", "满意", "感谢", "好", "棒");
        int negativeScore = countMatches(text, "bad", "terrible", "angry", "frustrated", "disappointed", "差", "糟", "怒", "失望", "投诉");
        int urgentScore = countMatches(text, "urgent", "asap", "immediately", "紧急", "立刻", "马上");

        double sentiment = (positiveScore - negativeScore) / Math.max(1.0, positiveScore + negativeScore);
        result.setScore(Math.round(sentiment * 100.0) / 100.0);

        if (sentiment > 0.3) {
            result.setSentiment("POSITIVE");
            result.setEmotion(positiveScore > 2 ? "grateful" : "satisfied");
        } else if (sentiment < -0.3) {
            result.setSentiment("NEGATIVE");
            result.setEmotion(urgentScore > 0 ? "frustrated" : "disappointed");
        } else {
            result.setSentiment("NEUTRAL");
            result.setEmotion("calm");
        }

        if (urgentScore > 0) {
            result.setSummary("Customer is expressing urgency. Priority escalation may be needed.");
        } else if (negativeScore > 1) {
            result.setSummary("Customer shows significant dissatisfaction. Consider proactive outreach.");
        } else if (positiveScore > 0) {
            result.setSummary("Customer interaction is positive. Standard resolution approach is appropriate.");
        } else {
            result.setSummary("Neutral tone detected. Proceed with standard workflow.");
        }

        return result;
    }

    @Override
    public SmartRoutingResult suggestRouting(Long ticketId) {
        SmartRoutingResult result = new SmartRoutingResult();
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) return result;

        // Find available support agents (role: SUPPORT or ADMIN)
        var agents = userMapper.selectList(new LambdaQueryWrapper<com.docflow.ai.auth.entity.SysUser>()
                .in(com.docflow.ai.auth.entity.SysUser::getStatus, 1)
                .eq(com.docflow.ai.auth.entity.SysUser::getDeleted, 0));

        if (agents.isEmpty()) return result;

        // Simple load-balanced routing
        Map<Long, Long> workload = new HashMap<>();
        for (var agent : agents) {
            LambdaQueryWrapper<Ticket> w = new LambdaQueryWrapper<>();
            w.eq(Ticket::getAssigneeUserId, agent.getId()).in(Ticket::getStatus, List.of(1, 2));
            workload.put(agent.getId(), ticketMapper.selectCount(w));
        }

        // Find agent with least workload
        Long bestAgentId = workload.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(agents.get(0).getId());

        var bestAgent = userMapper.selectById(bestAgentId);
        result.setRecommendedAssigneeId(bestAgentId);
        result.setRecommendedAssigneeName(bestAgent != null ? bestAgent.getRealName() : "Unknown");
        result.setConfidence(0.75);
        result.setReason("Selected based on lowest current workload (" + workload.getOrDefault(bestAgentId, 0L) + " active tickets)");

        // Build alternatives
        List<SmartRoutingResult.AlternativeRoute> alternatives = new ArrayList<>();
        workload.entrySet().stream()
                .filter(e -> !e.getKey().equals(bestAgentId))
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .forEach(e -> {
                    SmartRoutingResult.AlternativeRoute alt = new SmartRoutingResult.AlternativeRoute();
                    alt.setAssigneeId(e.getKey());
                    var u = userMapper.selectById(e.getKey());
                    alt.setAssigneeName(u != null ? u.getRealName() : "Unknown");
                    alt.setScore(Math.max(0.3, 0.8 - e.getValue() * 0.1));
                    alt.setReason("Active tickets: " + e.getValue());
                    alternatives.add(alt);
                });
        result.setAlternatives(alternatives);

        return result;
    }

    @Override
    public AutoReplyResult generateAutoReply(Long ticketId) {
        AutoReplyResult result = new AutoReplyResult();
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) return result;

        String text = (ticket.getTitle() + " " + ticket.getContent()).toLowerCase();

        if (containsAny(text, "login", "password", "登录", "密码")) {
            result.setReplyContent("Thank you for contacting us. For login issues, please try:\n1. Clear your browser cache and cookies\n2. Reset your password using the \'Forgot Password\' link\n3. Try a different browser\n\nIf the issue persists, please provide your account email and we will investigate further.");
            result.setTone("helpful");
            result.setConfidence(0.80);
            result.setSources(List.of("Login Troubleshooting Guide", "Account Recovery FAQ"));
        } else if (containsAny(text, "payment", "charge", "refund", "支付", "退款")) {
            result.setReplyContent("Thank you for reaching out about this payment concern. We take payment issues seriously.\n\nCould you please provide:\n1. The transaction ID or order number\n2. The date and amount of the charge\n3. A screenshot of any error message\n\nOur billing team will review and respond within 24 hours.");
            result.setTone("professional");
            result.setConfidence(0.75);
            result.setSources(List.of("Payment FAQ", "Refund Policy"));
        } else if (containsAny(text, "slow", "performance", "timeout", "慢", "超时")) {
            result.setReplyContent("We apologize for the performance issues you\'re experiencing. Our team is actively monitoring system performance.\n\nTo help us diagnose:\n1. What specific action is slow?\n2. When did this start?\n3. What browser/device are you using?\n\nWe will investigate and keep you updated on our progress.");
            result.setTone("apologetic");
            result.setConfidence(0.70);
            result.setSources(List.of("Performance Monitoring", "System Status Page"));
        } else {
            result.setReplyContent("Thank you for your message. We have received your request and a support agent will review it shortly.\n\nIn the meantime, you may find helpful information in our knowledge base. We aim to respond within our SLA timeframe.");
            result.setTone("professional");
            result.setConfidence(0.50);
            result.setSources(List.of("Knowledge Base"));
        }

        result.setDisclaimer("This is an AI-generated suggestion. Please review before sending.");
        return result;
    }

    @Override
    public ConversationSummary summarizeConversation(Long ticketId) {
        ConversationSummary summary = new ConversationSummary();
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) return summary;

        LambdaQueryWrapper<TicketComment> w = new LambdaQueryWrapper<>();
        w.eq(TicketComment::getTicketId, ticketId).orderByAsc(TicketComment::getCreateTime);
        List<TicketComment> comments = commentMapper.selectList(w);

        summary.setSummary("Ticket " + ticket.getTicketNo() + ": " + ticket.getTitle());
        summary.setKeyPoints(new ArrayList<>());
        summary.getKeyPoints().add("Issue: " + ticket.getTitle());
        summary.getKeyPoints().add("Status: " + mapStatus(ticket.getStatus()));
        summary.getKeyPoints().add("Comments: " + comments.size());

        if (ticket.getStatus() >= 3) {
            summary.setResolution("Ticket has been resolved.");
            summary.setRootCause("Identified through support workflow.");
        } else {
            summary.setResolution("Pending resolution.");
            summary.setRootCause("Under investigation.");
        }

        summary.setActionItems(new ArrayList<>());
        if (ticket.getStatus() == 1) summary.getActionItems().add("Assign to support agent");
        if (ticket.getStatus() == 2) summary.getActionItems().add("Follow up on progress");
        if ((Integer) null == null && ticket.getStatus() >= 3) summary.getActionItems().add("Request customer feedback");

        summary.setSentiment((Integer) null != null && (Integer) null >= 4 ? "positive" : "neutral");
        return summary;
    }

    @Override
    public List<KnowledgeRecommendation> recommendKnowledge(String title, String content, Long categoryId) {
        List<KnowledgeArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .eq(KnowledgeArticle::getStatus, 1)
                        .eq(KnowledgeArticle::getDeleted, 0)
                        .orderByDesc(KnowledgeArticle::getViewCount));

        List<KnowledgeRecommendation> recommendations = new ArrayList<>();
        String[] titleWords = title.toLowerCase().split("\\s+");

        for (KnowledgeArticle article : articles) {
            double score = 0;
            String articleText = (article.getTitle() + " " + (article.getSummary() != null ? article.getSummary() : "")).toLowerCase();
            for (String word : titleWords) {
                if (word.length() > 2 && articleText.contains(word)) score += 0.3;
            }
            if (categoryId != null && categoryId.equals(article.getCategoryId())) score += 0.2;

            if (score > 0.2) {
                KnowledgeRecommendation rec = new KnowledgeRecommendation();
                rec.setArticleId(article.getId());
                rec.setTitle(article.getTitle());
                rec.setSummary(article.getSummary());
                rec.setRelevanceScore(Math.min(1.0, score));
                rec.setMatchReason(score > 0.5 ? "High content relevance" : "Category match");
                recommendations.add(rec);
            }
        }

        recommendations.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        return recommendations.stream().limit(5).toList();
    }

    @Override
    public AiAnalysisResponse fullAnalysis(Long ticketId) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) return new AiAnalysisResponse();

        AiAnalysisResponse response = new AiAnalysisResponse();
        response.setIntent(analyzeIntent(ticket.getTitle(), ticket.getContent()));
        response.setSentiment(analyzeSentiment(ticket.getContent()));
        response.setRouting(suggestRouting(ticketId));
        response.setKnowledgeRecommendations(recommendKnowledge(ticket.getTitle(), ticket.getContent(), ticket.getCategoryId()));
        response.setSuggestedReply(generateAutoReply(ticketId));
        return response;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private int countMatches(String text, String... keywords) {
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) count++;
        }
        return count;
    }

    private String mapStatus(int status) {
        return switch (status) {
            case 1 -> "Open";
            case 2 -> "In Progress";
            case 3 -> "Resolved";
            case 4 -> "Closed";
            default -> "Unknown";
        };
    }
}
