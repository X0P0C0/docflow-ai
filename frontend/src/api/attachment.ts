import { http } from "@/utils/http";

type ApiResult<T> = { code: number; message: string; data: T; };

export type Attachment = {
  id: number;
  ticketId: number;
  fileName: string;
  fileUrl: string;
  fileSize?: number;
  fileType?: string;
  uploadUserId: number;
  createTime?: string;
};

export const getAttachments = (ticketId: number) => {
  return http.request<ApiResult<Attachment[]>>("get", `/api/tickets/${ticketId}/attachments`);
};

export const uploadAttachment = (ticketId: number, file: File) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.request<ApiResult<Attachment>>("post", `/api/tickets/${ticketId}/attachments`, {
    data: formData,
    headers: { "Content-Type": "multipart/form-data" }
  });
};

export const deleteAttachment = (ticketId: number, attachmentId: number) => {
  return http.request<ApiResult<void>>("delete", `/api/tickets/${ticketId}/attachments/${attachmentId}`);
};

export const formatFileSize = (bytes?: number): string => {
  if (!bytes) return "-";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};
