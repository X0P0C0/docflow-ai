package com.docflow.ai.ticket.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicketDetailResponse extends TicketListItemResponse {

    // 这是“已经由当前工单沉淀出来的知识文章”，用于展示沉淀结果和回跳入口。
    private List<TicketLinkedKnowledgeArticleResponse> sourceKnowledgeArticles;

    // timeline 代表可追溯处理过程，是详情页还原工单生命周期的主轴。
    private List<TicketTimelineItemResponse> timeline;

    // comments 偏向协作沟通内容，和 timeline 一起组成“发生了什么 + 大家说了什么”。
    private List<TicketCommentResponse> comments;

    // relatedArticles 是给处理人和提单人做快速复用的推荐知识，不一定直接来自当前工单。
    private List<TicketRelatedArticleResponse> relatedArticles;
}
