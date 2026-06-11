package com.docflow.ai.ai.controller;

import com.docflow.ai.ai.dto.*;
import com.docflow.ai.ai.service.AiAnalysisService;
import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "AI-powered ticket analysis and suggestions")
public class AiAnalysisController {

    private final AiAnalysisService aiService;

    @PostMapping("/analyze-intent")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Analyze ticket intent (classify type + priority)")
    public ApiResponse<IntentAnalysisResult> analyzeIntent(@RequestBody IntentRequest request) {
        return ApiResponse.success(aiService.analyzeIntent(request.getTitle(), request.getContent()));
    }

    @PostMapping("/analyze-sentiment")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Analyze text sentiment")
    public ApiResponse<SentimentResult> analyzeSentiment(@RequestBody SentimentRequest request) {
        return ApiResponse.success(aiService.analyzeSentiment(request.getContent()));
    }

    @GetMapping("/suggest-routing/{ticketId}")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Suggest optimal routing for a ticket")
    public ApiResponse<SmartRoutingResult> suggestRouting(@PathVariable Long ticketId) {
        return ApiResponse.success(aiService.suggestRouting(ticketId));
    }

    @GetMapping("/auto-reply/{ticketId}")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Generate auto-reply suggestion")
    public ApiResponse<AutoReplyResult> autoReply(@PathVariable Long ticketId) {
        return ApiResponse.success(aiService.generateAutoReply(ticketId));
    }

    @GetMapping("/summarize/{ticketId}")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Summarize ticket conversation")
    public ApiResponse<ConversationSummary> summarize(@PathVariable Long ticketId) {
        return ApiResponse.success(aiService.summarizeConversation(ticketId));
    }

    @PostMapping("/recommend-knowledge")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Recommend knowledge articles")
    public ApiResponse<List<KnowledgeRecommendation>> recommendKnowledge(@RequestBody KnowledgeRecommendRequest request) {
        return ApiResponse.success(aiService.recommendKnowledge(request.getTitle(), request.getContent(), request.getCategoryId()));
    }

    @GetMapping("/full-analysis/{ticketId}")
    @Timed(value = "ai.analyze", histogram = true)
    @Operation(summary = "Full AI analysis for a ticket")
    public ApiResponse<AiAnalysisResponse> fullAnalysis(@PathVariable Long ticketId) {
        return ApiResponse.success(aiService.fullAnalysis(ticketId));
    }

    // Simple request DTOs
    @lombok.Data
    public static class IntentRequest {
        private String title;
        private String content;
    }

    @lombok.Data
    public static class SentimentRequest {
        private String content;
    }

    @lombok.Data
    public static class KnowledgeRecommendRequest {
        private String title;
        private String content;
        private Long categoryId;
    }
}
