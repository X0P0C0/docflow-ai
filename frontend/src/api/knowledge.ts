import { http } from "@/utils/http";
import type { PageResponse } from "@/api/tickets";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

export type KnowledgeArticle = {
  id: number;
  title: string;
  summary: string;
  content: string;
  categoryId: number | null;
  sourceTicketId: number | null;
  sourceTicket: KnowledgeArticleSourceTicket | null;
  authorUserId: number | null;
  status: number | null;
  viewCount: number;
  likeCount: number;
  collectCount: number;
  publishTime: string | null;
  createTime: string | null;
  updateTime: string | null;
  versions: KnowledgeArticleVersion[];
};

export type KnowledgeArticleSourceTicket = {
  id: number;
  ticketNo: string;
  title: string;
};

export type KnowledgeArticleVersion = {
  id: number;
  versionNo: number;
  title: string;
  summary: string;
  remark: string;
  operatorUserId: number | null;
  createTime: string | null;
};

export type KnowledgeArticleQuery = {
  keyword?: string;
  categoryId?: number;
  status?: number;
  sourceTicketId?: number;
  sourceTicketNo?: string;
  page?: number;
  size?: number;
};

export type CreateKnowledgeArticleRequest = {
  title: string;
  summary?: string;
  content: string;
  categoryId?: number;
  sourceTicketId?: number;
  status: number;
};

export const getKnowledgeArticles = (params?: KnowledgeArticleQuery) => {
  return http.request<ApiResult<PageResponse<KnowledgeArticle>>>(
    "get",
    "/api/knowledge/articles",
    { params }
  );
};

export const getKnowledgeArticle = (id: number) => {
  return http.request<ApiResult<KnowledgeArticle>>(
    "get",
    `/api/knowledge/articles/${id}`
  );
};

export const createKnowledgeArticle = (data: CreateKnowledgeArticleRequest) => {
  return http.request<ApiResult<KnowledgeArticle>>(
    "post",
    "/api/knowledge/articles",
    { data }
  );
};

export const updateKnowledgeArticle = (
  id: number,
  data: CreateKnowledgeArticleRequest
) => {
  return http.request<ApiResult<KnowledgeArticle>>(
    "put",
    `/api/knowledge/articles/${id}`,
    { data }
  );
};

export const deleteKnowledgeArticle = (id: number) => {
  return http.request<ApiResult<void>>(
    "delete",
    `/api/knowledge/articles/${id}`
  );
};