package com.docflow.ai.common.pattern;

import com.docflow.ai.ticket.dto.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单详情 Builder 模式
 * <p>
 * 适用场景：
 * <ul>
 *   <li>对象有大量可选字段</li>
 *   <li>需要构建不同视图的 DTO（如列表视图 vs 详情视图）</li>
 *   <li>避免构造函数参数过多（telescoping constructor）</li>
 * </ul>
 * <p>
 * 对比 lombok @Builder：
 * <ul>
 *   <li>自定义 Builder 可以添加校验逻辑</li>
 *   <li>可以复用部分构建步骤</li>
 *   <li>可以构建不同类型的对象</li>
 * </ul>
 */
public class TicketDetailBuilder {

    private Long id;
    private String ticketNo;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private Integer status;
    private Long submitUserId;
    private String submitUserName;
    private Long assigneeUserId;
    private String assigneeUserName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketCommentResponse> comments = new ArrayList<>();
    private List<TicketTimelineItemResponse> timeline = new ArrayList<>();
    private List<TicketRelatedArticleResponse> relatedArticles = new ArrayList<>();

    public static TicketDetailBuilder create() {
        return new TicketDetailBuilder();
    }

    public TicketDetailBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public TicketDetailBuilder ticketNo(String ticketNo) {
        this.ticketNo = ticketNo;
        return this;
    }

    public TicketDetailBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TicketDetailBuilder content(String content) {
        this.content = content;
        return this;
    }

    public TicketDetailBuilder type(String type) {
        this.type = type;
        return this;
    }

    public TicketDetailBuilder priority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public TicketDetailBuilder status(Integer status) {
        this.status = status;
        return this;
    }

    public TicketDetailBuilder submitUser(Long userId, String userName) {
        this.submitUserId = userId;
        this.submitUserName = userName;
        return this;
    }

    public TicketDetailBuilder assignee(Long userId, String userName) {
        this.assigneeUserId = userId;
        this.assigneeUserName = userName;
        return this;
    }

    public TicketDetailBuilder timestamps(LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        return this;
    }

    public TicketDetailBuilder comments(List<TicketCommentResponse> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        return this;
    }

    public TicketDetailBuilder timeline(List<TicketTimelineItemResponse> timeline) {
        this.timeline = timeline != null ? timeline : new ArrayList<>();
        return this;
    }

    public TicketDetailBuilder relatedArticles(List<TicketRelatedArticleResponse> articles) {
        this.relatedArticles = articles != null ? articles : new ArrayList<>();
        return this;
    }

    /**
     * 构建完整详情 DTO
     */
    public TicketDetailResponse build() {
        TicketDetailResponse detail = new TicketDetailResponse();
        detail.setId(id);
        detail.setTicketNo(ticketNo);
        detail.setTitle(title);
        detail.setContent(content);
        detail.setType(type);
        detail.setPriority(priority);
        detail.setStatus(status);
        detail.setSubmitUserId(submitUserId);
        detail.setSubmitterName(submitUserName);
        detail.setAssigneeUserId(assigneeUserId);
        detail.setAssigneeName(assigneeUserName);
        detail.setCreateTime(createdAt);
        detail.setUpdateTime(updatedAt);
        detail.setComments(comments);
        detail.setTimeline(timeline);
        detail.setRelatedArticles(relatedArticles);
        return detail;
    }

    /**
     * 构建简化列表 DTO（不含评论和时间线）
     */
    public TicketListItemResponse buildListItem() {
        TicketListItemResponse item = new TicketListItemResponse();
        item.setId(id);
        item.setTicketNo(ticketNo);
        item.setTitle(title);
        item.setType(type);
        item.setPriority(priority);
        item.setStatus(status);
        item.setSubmitterName(submitUserName);
        item.setAssigneeName(assigneeUserName);
        item.setCreateTime(createdAt);
        return item;
    }
}
