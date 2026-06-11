import { http } from "@/utils/http";

type ApiResult<T> = { code: number; message: string; data: T; };

export type AuditLogEntry = {
  id: number;
  userId: number;
  username: string;
  module: string;
  action: string;
  method: string;
  uri: string;
  statusCode: number;
  durationMs: number;
  clientIp: string;
  operateTime: string;
  success: boolean;
  errorMessage?: string;
  traceId?: string;
};

export type PageResponse<T> = {
  records: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
};

export const getAuditLogs = (params?: {
  page?: number; size?: number;
  module?: string; username?: string;
  success?: boolean; startTime?: string; endTime?: string;
}) => {
  return http.request<ApiResult<PageResponse<AuditLogEntry>>>("get", "/api/system/audit-logs", { params });
};

export const getAuditLogModules = () => {
  return http.request<ApiResult<string[]>>("get", "/api/system/audit-logs/modules");
};
