import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type CategoryItem = {
  id: number;
  name: string;
  description?: string;
  parentId: number | null;
  sort: number;
  enabled: boolean;
  children?: CategoryItem[];
  createTime?: string;
};

export type CreateCategoryRequest = {
  name: string;
  description?: string;
  parentId?: number | null;
  sort?: number;
};

export const getCategories = () => {
  return http.request<ApiResult<CategoryItem[]>>("get", "/api/categories");
};

export const createCategory = (data: CreateCategoryRequest) => {
  return http.request<ApiResult<CategoryItem>>("post", "/api/categories", { data });
};

export const updateCategory = (id: number, data: CreateCategoryRequest) => {
  return http.request<ApiResult<CategoryItem>>("put", `/api/categories/${id}`, { data });
};

export const deleteCategory = (id: number) => {
  return http.request<ApiResult<void>>("delete", `/api/categories/${id}`);
};
