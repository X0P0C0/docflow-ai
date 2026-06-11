import { http } from "@/utils/http";
type ApiResult<T> = { code: number; message: string; data: T; };

export type CustomField = {
  id: number; name: string; fieldType: string; required: boolean;
  options?: string; categoryId?: number; sort: number; createTime?: string;
};

export type CreateFieldRequest = {
  name: string; fieldType: string; required?: boolean;
  options?: string; categoryId?: number; sort?: number;
};

export const getCustomFields = (params?: { categoryId?: number }) =>
  http.request<ApiResult<CustomField[]>>("get", "/api/custom-fields", { params });

export const createCustomField = (data: CreateFieldRequest) =>
  http.request<ApiResult<CustomField>>("post", "/api/custom-fields", { data });

export const updateCustomField = (id: number, data: CreateFieldRequest) =>
  http.request<ApiResult<CustomField>>("put", `/api/custom-fields/${id}`, { data });

export const deleteCustomField = (id: number) =>
  http.request<ApiResult<void>>("delete", `/api/custom-fields/${id}`);
