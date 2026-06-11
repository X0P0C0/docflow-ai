package com.docflow.ai.ai.service.impl;

import com.docflow.ai.ai.dto.*;
import com.docflow.ai.ai.service.AiAnalysisService;
import com.docflow.ai.common.resilience.ResilienceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Primary
@Service
public class ResilientAiAnalysisServiceImpl implements AiAnalysisService {

    private static final String CB_NAME = "ai-service";
    private final AiAnalysisService delegate;
    private final ResilienceService resilience;

    public ResilientAiAnalysisServiceImpl(AiAnalysisServiceImpl delegate, ResilienceService resilience) {
        this.delegate = delegate;
        this.resilience = resilience;
    }

    @Override
    public IntentAnalysisResult analyzeIntent(String title, String content) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.analyzeIntent(title, content),
                () -> { IntentAnalysisResult fb = new IntentAnalysisResult(); fb.setTicketType("INCIDENT"); fb.setPriority(3); fb.setConfidence(0.0); fb.setReason("AI unavailable"); return fb; });
    }

    @Override
    public SentimentResult analyzeSentiment(String content) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.analyzeSentiment(content),
                () -> { SentimentResult fb = new SentimentResult(); fb.setSentiment("NEUTRAL"); fb.setScore(0.0); return fb; });
    }

    @Override
    public SmartRoutingResult suggestRouting(Long ticketId) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.suggestRouting(ticketId),
                () -> { SmartRoutingResult fb = new SmartRoutingResult(); fb.setReason("AI unavailable"); fb.setConfidence(0.0); return fb; });
    }

    @Override
    public AutoReplyResult generateAutoReply(Long ticketId) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.generateAutoReply(ticketId),
                () -> { AutoReplyResult fb = new AutoReplyResult(); fb.setReplyContent("Our team will review your request shortly."); fb.setDisclaimer("AI temporarily unavailable"); return fb; });
    }

    @Override
    public ConversationSummary summarizeConversation(Long ticketId) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.summarizeConversation(ticketId),
                () -> { ConversationSummary fb = new ConversationSummary(); fb.setSummary("AI service temporarily down"); fb.setKeyPoints(Collections.emptyList()); return fb; });
    }

    @Override
    public List<KnowledgeRecommendation> recommendKnowledge(String title, String content, Long categoryId) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.recommendKnowledge(title, content, categoryId),
                () -> Collections.emptyList());
    }

    @Override
    public AiAnalysisResponse fullAnalysis(Long ticketId) {
        return resilience.executeWithFallback(CB_NAME,
                () -> delegate.fullAnalysis(ticketId),
                () -> { AiAnalysisResponse fb = new AiAnalysisResponse(); fb.setSentiment(new SentimentResult()); fb.setKnowledgeRecommendations(Collections.emptyList()); return fb; });
    }
}
