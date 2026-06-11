import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type SystemHealth = {
  status: string;
  timestamp: string;
  redis: { status: string };
  circuitBreakers: Record<string, {
    state: string;
    failureRate: number;
    bufferedCalls: number;
  }>;
  jvm: {
    heapUsedMB: number;
    heapMaxMB: number;
    uptimeMinutes: number;
  };
};

export const getSystemHealth = () => {
  return http.request<ApiResult<SystemHealth>>("get", "/api/system/health");
};

// ===== Monitoring APIs (stubs for build compatibility) =====

export type LogEntry = {
  id: number;
  module: string;
  url: string;
  method: string;
  ip: string;
  address: string;
  system: string;
  browser: string;
  takesTime: number;
  requestTime: string;
  [key: string]: any;
};

export type LogListResponse = {
  list: LogEntry[];
  total: number;
  pageSize: number;
  currentPage: number;
};

export type OnlineUser = {
  id: number;
  username: string;
  ip: string;
  address: string;
  browser: string;
  system: string;
  loginTime: string;
};

export const getLoginLogsList = (params?: any) => {
  return http.request<ApiResult<LogListResponse>>("get", "/api/system/logs/login", { params });
};

export const getOperationLogsList = (params?: any) => {
  return http.request<ApiResult<LogListResponse>>("get", "/api/system/logs/operation", { params });
};

export const getSystemLogsList = (params?: any) => {
  return http.request<ApiResult<LogListResponse>>("get", "/api/system/logs/system", { params });
};

export const getSystemLogsDetail = (params?: { id: number }) => {
  return http.request<ApiResult<LogEntry>>("get", `/api/system/logs/system/${params?.id}`);
};

export const getOnlineLogsList = (params?: any) => {
  return http.request<ApiResult<LogListResponse>>("get", "/api/system/online", { params });
};

// ===== System Management APIs =====

export type RoleItem = {
  id: number;
  name: string;
  code: string;
  status: number;
  remark?: string;
  createTime?: string;
};

export type DeptItem = {
  id: number;
  name: string;
  parentId: number;
  sort: number;
  status: number;
  children?: DeptItem[];
};

export type MenuItem = {
  id: number;
  title: string;
  parentId: number;
  path?: string;
  icon?: string;
  sort: number;
  status: number;
  children?: MenuItem[];
};

export type UserItem = {
  id: number;
  username: string;
  nickname: string;
  phone?: string;
  email?: string;
  status: number;
  deptId?: number;
  roles?: string[];
  createTime?: string;
};

export const getAllRoleList = () => {
  return http.request<ApiResult<RoleItem[]>>("get", "/api/system/roles/all");
};

export const getRoleList = (params?: any) => {
  return http.request<ApiResult<{ list: RoleItem[]; total: number }>>("get", "/api/system/roles", { params });
};

export const getRoleMenu = (params?: { roleId: number }) => {
  return http.request<ApiResult<MenuItem[]>>("get", `/api/system/roles/${params?.roleId}/menus`);
};

export const getRoleMenuIds = (params?: { roleId: number }) => {
  return http.request<ApiResult<number[]>>("get", `/api/system/roles/${params?.roleId}/menu-ids`);
};

export const getDeptList = (params?: any) => {
  return http.request<ApiResult<DeptItem[]>>("get", "/api/system/depts", { params });
};

export const getMenuList = (params?: any) => {
  return http.request<ApiResult<MenuItem[]>>("get", "/api/system/menus", { params });
};

export const getUserList = (params?: any) => {
  return http.request<ApiResult<{ list: UserItem[]; total: number }>>("get", "/api/system/users", { params });
};

export const getRoleIds = (params?: { userId: number }) => {
  return http.request<ApiResult<number[]>>("get", `/api/system/users/${params?.userId}/role-ids`);
};
