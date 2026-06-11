import { http } from "@/utils/http";
type ApiResult<T> = { code: number; message: string; data: T; };

export type AutomationRule = {
  id: number; name: string; description?: string;
  triggerType: string; conditions: string; actions: string;
  status: number; priority: number; createTime?: string;
};

export type CreateRuleRequest = {
  name: string; description?: string; triggerType: string;
  conditions: string; actions: string; priority?: number;
};

export const getAutomationRules = () =>
  http.request<ApiResult<AutomationRule[]>>("get", "/api/system/automation-rules");

export const createAutomationRule = (data: CreateRuleRequest) =>
  http.request<ApiResult<AutomationRule>>("post", "/api/system/automation-rules", { data });

export const updateAutomationRule = (id: number, data: CreateRuleRequest) =>
  http.request<ApiResult<AutomationRule>>("put", `/api/system/automation-rules/${id}`, { data });

export const deleteAutomationRule = (id: number) =>
  http.request<ApiResult<void>>("delete", `/api/system/automation-rules/${id}`);

export const toggleAutomationRule = (id: number, status: number) =>
  http.request<ApiResult<void>>("post", `/api/system/automation-rules/${id}/toggle`, { params: { status } });
