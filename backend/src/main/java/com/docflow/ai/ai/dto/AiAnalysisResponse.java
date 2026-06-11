package com.docflow.ai.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiAnalysisResponse {
    private IntentAnalysisResult intent;
    private SentimentResult sentiment;
    private SmartRoutingResult routing;
    private List<KnowledgeRecommendation> knowledgeRecommendations;
    private AutoReplyResult suggestedReply;
}
