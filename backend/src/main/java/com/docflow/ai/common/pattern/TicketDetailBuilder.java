package com.docflow.ai.common.pattern;

import com.docflow.ai.ticket.dto.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单详情构建器 - 建造者模式 (Builder Pattern)
 *
 * 【设计模式】建造者模式
 * 【面试考点】
 *   - 建造者 vs Lombok @Builder：自定义 Builder 可以添加校验和复用逻辑
 *   - 适用场景：对象有大量可选字段，避免"伸缩构造函数"问题
 *   - 链式调用：每个 set 方法返回 this，支持 fluent API
 *   - 构建不同类型：同一个 Builder 可以构建详情 DTO 和列表 DTO
 *
 * 【为什么不用构造函数？】
 *   工单详情有 15+ 个字段，构造函数参数太多，可读性差
 *   很多字段是可选的（评论、时间线、关联文章）
 *   Builder 模式让代码更清晰：builder.id(1).title("...").build()
 *
 * 【真实业务场景】
 *   工单详情页需要组装：基本信息 + 评论列表 + 时间线 + 关联知识文章
 *   工单列表页只需要：基本信息（不含评论和时间线）
 *   同一个 Builder，不同的 build 方法，产出不同的 DTO
 */
public class TicketDetailBuilder {

    // 基本信息
    private Long id;
    private String ticketNo;
    private String title;
    private String content;
    private String type;
    private Integer priority;
    private Integer status;

    // 用户信息
    private Long submitUserId;
    private String submitUserName;
    private Long assigneeUserId;
    private String assigneeUserName;

    // 时间信息
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联信息（可选）
    private List<TicketCommentResponse> comments = new ArrayList<>();
    private List<TicketTimelineItemResponse> timeline = new ArrayList<>();
    private List<TicketRelatedArticleResponse> relatedArticles = new ArrayList<>();

    /** 静态工厂方法（比 new TicketDetailBuilder() 更语义化） */
    public static TicketDetailBuilder create() {
        return new TicketDetailBuilder();
    }

    // ===== 链式设置方法（每个返回 this 支持 fluent API）=====

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

    /** 设置提交人信息（用户ID + 用户名一起设置，避免不一致） */
    public TicketDetailBuilder submitUser(Long userId, String userName) {
        this.submitUserId = userId;
        this.submitUserName = userName;
        return this;
    }

    /** 设置处理人信息 */
    public TicketDetailBuilder assignee(Long userId, String userName) {
        this.assigneeUserId = userId;
        this.assigneeUserName = userName;
        return this;
    }

    /** 设置时间戳 */
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
     * 构建完整详情 DTO（包含评论、时间线、关联文章）
     * 用于工单详情页
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
     * 构建简化列表 DTO（不含评论和时间线，减少数据传输）
     * 用于工单列表页
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