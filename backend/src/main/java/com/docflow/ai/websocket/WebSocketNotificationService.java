package com.docflow.ai.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send notification to a specific user
     */
    public void sendToUser(Long userId, String type, String title, String content) {
        Map<String, Object> payload = Map.of(
                "type", type,
                "title", title,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                payload
        );
        log.info("WebSocket notification sent to user {}: {}", userId, title);
    }

    /**
     * Broadcast to all connected users
     */
    public void broadcast(String type, String title, String content) {
        Map<String, Object> payload = Map.of(
                "type", type,
                "title", title,
                "content", content,
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend("/topic/broadcast", payload);
        log.info("WebSocket broadcast: {}", title);
    }
}
