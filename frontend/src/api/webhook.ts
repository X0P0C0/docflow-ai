import { http } from "@/utils/http";
type ApiResult<T> = { code: number; message: string; data: T; };

export type WebhookConfig = {
  id: number; name: string; url: string; secret?: string;
  events: string; enabled: boolean; createTime?: string;
};

export type WebhookRequest = {
  name: string; url: string; secret?: string; events: string;
};

export const getWebhooks = () =>
  http.request<ApiResult<WebhookConfig[]>>("get", "/api/system/webhooks");

export const createWebhook = (data: WebhookRequest) =>
  http.request<ApiResult<WebhookConfig>>("post", "/api/system/webhooks", { data });

export const updateWebhook = (id: number, data: WebhookRequest) =>
  http.request<ApiResult<WebhookConfig>>("put", `/api/system/webhooks/${id}`, { data });

export const deleteWebhook = (id: number) =>
  http.request<ApiResult<void>>("delete", `/api/system/webhooks/${id}`);

export const testWebhook = (id: number) =>
  http.request<ApiResult<void>>("post", `/api/system/webhooks/${id}/test`);
