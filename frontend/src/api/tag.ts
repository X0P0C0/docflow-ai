import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type TagItem = {
  id: number;
  name: string;
  color?: string;
  createTime?: string;
};

export const getTags = () => {
  return http.request<ApiResult<TagItem[]>>("get", "/api/tags");
};

export const createTag = (data: { name: string; color?: string }) => {
  return http.request<ApiResult<TagItem>>("post", "/api/tags", { data });
};

export const deleteTag = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/tags/${id}`);
};

export const addTagToTicket = (ticketId: number, tagId: number) => {
  return http.request<ApiResult<void>>("post", `/api/tags/ticket/${ticketId}/tag/${tagId}`);
};

export const removeTagFromTicket = (ticketId: number, tagId: number) => {
  return http.request<ApiResult<void>>("delete", `/api/tags/ticket/${ticketId}/tag/${tagId}`);
};

export const getTicketTags = (ticketId: number) => {
  return http.request<ApiResult<TagItem[]>>("get", `/api/tags/ticket/${ticketId}`);
};
