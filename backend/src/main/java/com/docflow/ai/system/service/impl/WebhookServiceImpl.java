package com.docflow.ai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docflow.ai.auth.service.UserAccessService;
import com.docflow.ai.common.enums.ResultCode;
import com.docflow.ai.exception.BusinessException;
import com.docflow.ai.system.dto.WebhookConfigRequest;
import com.docflow.ai.system.dto.WebhookConfigResponse;
import com.docflow.ai.system.entity.WebhookConfig;
import com.docflow.ai.system.entity.WebhookLog;
import com.docflow.ai.system.mapper.WebhookConfigMapper;
import com.docflow.ai.system.mapper.WebhookLogMapper;
import com.docflow.ai.system.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookConfigMapper webhookMapper;
    private final WebhookLogMapper logMapper;
    private final UserAccessService userAccessService;
    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);

    @Override
    public List<WebhookConfigResponse> listWebhooks(Long userId) {
        userAccessService.requireSystemAdmin(userId);
        return webhookMapper.selectList(new LambdaQueryWrapper<WebhookConfig>()
                .eq(WebhookConfig::getDeleted, 0).orderByDesc(WebhookConfig::getCreateTime))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebhookConfigResponse createWebhook(Long userId, WebhookConfigRequest request) {
        userAccessService.requireSystemAdmin(userId);
        WebhookConfig wh = new WebhookConfig();
        wh.setName(request.getName());
        wh.setUrl(request.getUrl());
        wh.setSecret(request.getSecret());
        wh.setEvents(request.getEvents());
        wh.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        wh.setCreateBy(userId);
        wh.setDeleted(0);
        webhookMapper.insert(wh);
        return toResponse(wh);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWebhook(Long id, Long userId) {
        userAccessService.requireSystemAdmin(userId);
        WebhookConfig wh = webhookMapper.selectById(id);
        if (wh == null) throw new BusinessException(ResultCode.TICKET_NOT_FOUND);
        wh.setDeleted(1);
        webhookMapper.updateById(wh);
    }

    @Override
    public void triggerWebhooks(String event, String payload) {
        List<WebhookConfig> webhooks = webhookMapper.selectList(
                new LambdaQueryWrapper<WebhookConfig>()
                        .eq(WebhookConfig::getStatus, 1)
                        .eq(WebhookConfig::getDeleted, 0));

        for (WebhookConfig wh : webhooks) {
            if (wh.getEvents() != null && wh.getEvents().contains(event)) {
                try {
                    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(wh.getUrl()))
                            .header("Content-Type", "application/json")
                            .header("X-Webhook-Event", event)
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .timeout(Duration.ofSeconds(10))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    WebhookLog whLog = new WebhookLog();
                    whLog.setWebhookId(wh.getId());
                    whLog.setEvent(event);
                    whLog.setPayload(payload);
                    whLog.setResponseCode(response.statusCode());
                    whLog.setResponseBody(response.body());
                    whLog.setStatus(response.statusCode() >= 200 && response.statusCode() < 300 ? 1 : 0);
                    logMapper.insert(whLog);
                } catch (Exception e) {
                    log.warn("Webhook {} failed for event {}: {}", wh.getName(), event, e.getMessage());
                    WebhookLog whLog = new WebhookLog();
                    whLog.setWebhookId(wh.getId());
                    whLog.setEvent(event);
                    whLog.setPayload(payload);
                    whLog.setResponseCode(0);
                    whLog.setResponseBody(e.getMessage());
                    whLog.setStatus(0);
                    logMapper.insert(whLog);
                }
            }
        }
    }

    private WebhookConfigResponse toResponse(WebhookConfig wh) {
        WebhookConfigResponse r = new WebhookConfigResponse();
        r.setId(wh.getId());
        r.setName(wh.getName());
        r.setUrl(wh.getUrl());
        r.setEvents(wh.getEvents());
        r.setStatus(wh.getStatus());
        r.setCreateTime(wh.getCreateTime());
        return r;
    }
}
