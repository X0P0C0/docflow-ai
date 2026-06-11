import { http } from "@/utils/http";
type ApiResult<T> = { code: number; message: string; data: T; };

export type TicketTemplate = {
  id: number; name: string; title: string; content: string;
  type?: string; priority?: number; categoryId?: number; createTime?: string;
};

export type CreateTemplateRequest = {
  name: string; title: string; content: string;
  type?: string; priority?: number; categoryId?: number;
};

export const getTemplates = () =>
  http.request<ApiResult<TicketTemplate[]>>("get", "/api/ticket-templates");

export const createTemplate = (data: CreateTemplateRequest) =>
  http.request<ApiResult<TicketTemplate>>("post", "/api/ticket-templates", { data });

export const updateTemplate = (id: number, data: CreateTemplateRequest) =>
  http.request<ApiResult<TicketTemplate>>("put", `/api/ticket-templates/${id}`, { data });

export const deleteTemplate = (id: number) =>
  http.request<ApiResult<void>>("delete", `/api/ticket-templates/${id}`);
