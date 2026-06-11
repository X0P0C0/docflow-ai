import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type SlaPolicy = {
  id: number;
  name: string;
  description?: string;
  priority: number;
  responseHours: number;
  resolveHours: number;
  businessHoursOnly: boolean;
  status: number;
  createTime?: string;
};

export type SlaStatusResponse = {
  ticketId: number;
  ticketNo: string;
  policyName: string;
  responseDeadline: string;
  resolveDeadline: string;
  responseBreached: boolean;
  resolveBreached: boolean;
  elapsedMinutes: number;
};

export type SlaPolicyRequest = {
  name: string;
  description?: string;
  priority: number;
  responseHours: number;
  resolveHours: number;
  businessHoursOnly?: boolean;
};

export const getSlaPolicies = () => {
  return http.request<ApiResult<SlaPolicy[]>>("get", "/api/sla/policies");
};

export const createSlaPolicy = (data: SlaPolicyRequest) => {
  return http.request<ApiResult<SlaPolicy>>("post", "/api/sla/policies", { data });
};

export const updateSlaPolicy = (id: number, data: SlaPolicyRequest) => {
  return http.request<ApiResult<SlaPolicy>>("put", `/api/sla/policies/${id}`, { data });
};

export const deleteSlaPolicy = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/sla/policies/${id}`);
};

export const getSlaBreaches = () => {
  return http.request<ApiResult<SlaStatusResponse[]>>("get", "/api/sla/breaches");
};

export const checkTicketSla = (ticketId: number) => {
  return http.request<ApiResult<SlaStatusResponse>>("get", `/api/sla/ticket/${ticketId}`);
};
