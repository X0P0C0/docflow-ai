package com.docflow.ai.common.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.docflow.ai.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 事件订阅者。
 * 增强版：消息去重 + 死信队列 + 重试机制。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final MessageDeduplicationService deduplicationService;
    private final DeadLetterQueueService deadLetterQueueService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        DomainEvent event = null;
        try {
            String json = new StringRedisSerializer().deserialize(message.getBody());
            event = objectMapper.readValue(json, DomainEvent.class);

            // 去重：同一事件只处理一次
            if (event.getEventId() != null && !deduplicationService.tryMarkProcessed(event.getEventId())) {
                log.debug("Duplicate event skipped: eventId={}", event.getEventId());
                return;
            }

            log.info("Event received: eventId={}, type={}, entity={}({})",
                    event.getEventId(), event.getEventType(), event.getEntityType(), event.getEntityId());

            switch (event.getEventType()) {
                case "TICKET_CREATED" -> handleTicketCreated(event);
                case "TICKET_STATUS_CHANGED" -> handleStatusChanged(event);
                case "TICKET_ASSIGNED" -> handleTicketAssigned(event);
                default -> log.debug("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Event processing failed, sending to DLQ: {}", e.getMessage(), e);
            // 处理失败进入死信队列
            if (event != null) {
                deadLetterQueueService.addToDeadLetter(event);
            }
        }
    }

    private void handleTicketCreated(DomainEvent event) {
        log.info("Handler: ticket created, entityId={}", event.getEntityId());
    }

    private void handleStatusChanged(DomainEvent event) {
        log.info("Handler: status changed, entityId={}", event.getEntityId());
    }

    private void handleTicketAssigned(DomainEvent event) {
        log.info("Handler: ticket assigned, entityId={}", event.getEntityId());
    }
}
