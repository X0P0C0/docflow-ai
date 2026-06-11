package com.docflow.ai.ai.service;

import com.docflow.ai.ai.dto.*;
import java.util.List;

public interface AiAnalysisService {
    IntentAnalysisResult analyzeIntent(String title, String content);
    SentimentResult analyzeSentiment(String content);
    SmartRoutingResult suggestRouting(Long ticketId);
    AutoReplyResult generateAutoReply(Long ticketId);
    ConversationSummary summarizeConversation(Long ticketId);
    List<KnowledgeRecommendation> recommendKnowledge(String title, String content, Long categoryId);
    AiAnalysisResponse fullAnalysis(Long ticketId);
}
