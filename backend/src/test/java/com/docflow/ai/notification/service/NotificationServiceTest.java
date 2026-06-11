package com.docflow.ai.notification.service;

import com.docflow.ai.common.domain.PageResponse;
import com.docflow.ai.notification.dto.NotificationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("List notifications for user")
    void list_validUser_returnsPage() {
        PageResponse<NotificationResponse> result = notificationService.list(1L, 1, 20);
        assertNotNull(result);
        assertNotNull(result.getRecords());
    }

    @Test
    @DisplayName("Get unread count")
    void getUnreadCount_validUser_returnsCount() {
        long count = notificationService.getUnreadCount(1L);
        assertTrue(count >= 0);
    }

    @Test
    @DisplayName("Mark notification as read")
    void markAsRead_validId_success() {
        PageResponse<NotificationResponse> page = notificationService.list(1L, 1, 1);
        if (!page.getRecords().isEmpty()) {
            NotificationResponse notif = page.getRecords().get(0);
            assertDoesNotThrow(() -> notificationService.markAsRead(notif.getId(), 1L));
        }
    }

    @Test
    @DisplayName("Mark all as read")
    void markAllAsRead_validUser_success() {
        assertDoesNotThrow(() -> notificationService.markAllAsRead(1L));
    }
}
