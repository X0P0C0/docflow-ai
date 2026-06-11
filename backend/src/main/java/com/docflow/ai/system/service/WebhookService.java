package com.docflow.ai.system.service;

import com.docflow.ai.system.dto.WebhookConfigRequest;
import com.docflow.ai.system.dto.WebhookConfigResponse;
import java.util.List;

public interface WebhookService {
    List<WebhookConfigResponse> listWebhooks(Long userId);
    WebhookConfigResponse createWebhook(Long userId, WebhookConfigRequest request);
    void deleteWebhook(Long id, Long userId);
    void triggerWebhooks(String event, String payload);
}
