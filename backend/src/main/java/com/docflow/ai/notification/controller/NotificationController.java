package com.docflow.ai.notification.controller;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.common.domain.PageResponse;
import com.docflow.ai.logging.AuditLog;
import com.docflow.ai.notification.dto.NotificationResponse;
import com.docflow.ai.notification.dto.NotificationUnreadCount;
import com.docflow.ai.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心", description = "站内通知的查询、标记已读")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "通知列表", description = "分页获取当前用户的通知")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notificationService.list(principal.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    public ApiResponse<NotificationUnreadCount> unreadCount(
            @AuthenticationPrincipal AuthUserPrincipal principal) {
        NotificationUnreadCount result = new NotificationUnreadCount();
        result.setUnreadCount(notificationService.getUnreadCount(principal.getUserId()));
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条通知已读")
    @AuditLog(module = "通知", action = "标记已读")
    public ApiResponse<Void> markAsRead(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthUserPrincipal principal) {
        notificationService.markAsRead(id, principal.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/read-all")
    @Operation(summary = "标记所有通知已读")
    @AuditLog(module = "通知", action = "全部标记已读")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal AuthUserPrincipal principal) {
        notificationService.markAllAsRead(principal.getUserId());
        return ApiResponse.success();
    }
}