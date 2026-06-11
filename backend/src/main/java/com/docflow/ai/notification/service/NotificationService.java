package com.docflow.ai.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.docflow.ai.common.domain.PageResponse;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.notification.dto.NotificationResponse;
import com.docflow.ai.notification.entity.Notification;
import com.docflow.ai.notification.mapper.NotificationMapper;
import com.docflow.ai.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final WebSocketNotificationService wsNotificationService;

    /**
     * 创建通知（供其他模块调用，如工单指派、状态变更时）。
     * 同时通过 WebSocket 推送给在线用户。
     */
    public void create(Long receiverId, Long senderId, String type, String title,
                       String content, String refType, Long refId) {
        Notification notification = new Notification();
        notification.setReceiverUserId(receiverId);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedBusinessType(refType);
        notification.setRelatedBusinessId(refId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        notification.setDeleted(0);
        notificationMapper.insert(notification);

        // Push real-time notification via WebSocket
        try {
            wsNotificationService.sendToUser(receiverId, type, title, content);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to user {}: {}", receiverId, e.getMessage());
        }
    }

    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        Page<Notification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverUserId, userId)
                .eq(Notification::getDeleted, 0)
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> result = notificationMapper.selectPage(pageParam, wrapper);
        List<NotificationResponse> items = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.<NotificationResponse>builder()
                .records(items)
                .total(result.getTotal())
                .page(page)
                .size(size)
                .pages(size > 0 ? (result.getTotal() + size - 1) / size : 0)
                .build();
    }

    public long getUnreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getReceiverUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (Integer.valueOf(0).equals(notification.getIsRead())) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverUserId, userId)
                        .eq(Notification::getIsRead, 0)
                        .eq(Notification::getDeleted, 0)
        );
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : unread) {
            n.setIsRead(1);
            n.setReadTime(now);
            notificationMapper.updateById(n);
        }
    }

    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setType(n.getNotificationType());
        r.setTitle(n.getTitle());
        r.setContent(n.getContent());
        r.setRefType(n.getRelatedBusinessType());
        r.setRefId(n.getRelatedBusinessId());
        r.setReadFlag(n.getIsRead());
        r.setCreateTime(n.getCreateTime());
        r.setReadTime(n.getReadTime());
        return r;
    }
}
