import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type PageResponse<T> = {
  records: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
};

export type NotificationItem = {
  id: number;
  title: string;
  content: string;
  type: string;
  read: boolean;
  relatedId?: number;
  relatedType?: string;
  createTime?: string;
};

export type UnreadCount = {
  unreadCount: number;
};

export const getNotifications = (params?: { page?: number; size?: number }) => {
  return http.request<ApiResult<PageResponse<NotificationItem>>>("get", "/api/notifications", { params });
};

export const getUnreadCount = () => {
  return http.request<ApiResult<UnreadCount>>("get", "/api/notifications/unread-count");
};

export const markAsRead = (id: number) => {
  return http.request<ApiResult<void>>("post", `/api/notifications/${id}/read`);
};

export const markAllAsRead = () => {
  return http.request<ApiResult<void>>("post", "/api/notifications/read-all");
};
