package com.docflow.ai.ticket.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicketDetailResponse extends TicketListItemResponse {

    private List<TicketLinkedKnowledgeArticleResponse> sourceKnowledgeArticles;

    private List<TicketTimelineItemResponse> timeline;

    private List<TicketCommentResponse> comments;

    private List<TicketRelatedArticleResponse> relatedArticles;
}
