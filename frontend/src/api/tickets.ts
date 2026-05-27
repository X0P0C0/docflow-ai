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

export type TicketListItem = {
  id: number;
  ticketNo: string;
  title: string;
  content: string;
  type: string;
  categoryId: number | null;
  priority: number | null;
  priorityLabel: string;
  status: number | null;
  statusLabel: string;
  submitUserId: number | null;
  assigneeUserId: number | null;
  submitterName: string;
  assigneeName: string;
  linkedKnowledgeArticleCount: number;
  latestLinkedKnowledgeArticle: TicketLinkedKnowledgeArticle | null;
  createTime: string | null;
  updateTime: string | null;
};

export type TicketLinkedKnowledgeArticle = {
  id: number;
  title: string;
  status: number | null;
  statusLabel: string;
  publishTime: string | null;
  updateTime: string | null;
};

export type TicketComment = {
  id: number;
  authorName: string;
  content: string;
  commentTypeLabel: string;
  internal: boolean;
  createTime: string | null;
};

export type TicketTimelineItem = {
  id: number;
  operatorName: string;
  title: string;
  desc: string;
  createTime: string | null;
};

export type TicketRelatedArticle = {
  id: number;
  title: string;
  summary: string;
  reason: string;
};

export type TicketDetail = TicketListItem & {
  sourceKnowledgeArticles: TicketLinkedKnowledgeArticle[];
  timeline: TicketTimelineItem[];
  comments: TicketComment[];
  relatedArticles: TicketRelatedArticle[];
};

export type TicketQuery = {
  keyword?: string;
  status?: number;
  priority?: number;
  type?: string;
  assigneeUserId?: number;
  page?: number;
  size?: number;
};

export type AddTicketCommentRequest = {
  content: string;
  commentType?: number;
  internal?: boolean;
};

export type TicketAssigneeOption = {
  id: number;
  username: string;
  displayName: string;
  roles: string[];
};

export type CreateTicketRequest = {
  title: string;
  content: string;
  type: string;
  categoryId: number;
  priority: number;
};

export type UpdateTicketStatusRequest = {
  status: number;
  remark?: string;
};

export type AssignTicketRequest = {
  assigneeUserId: number;
  remark?: string;
};


export type TicketStats = {
  total: number;
  newCount: number;
  inProgress: number;
  resolved: number;
  unassigned: number;
};

export const getTickets = (params?: TicketQuery) => {
  return http.request<ApiResult<PageResponse<TicketListItem>>>("get", "/api/tickets", {
    params
  });
};

export const getTicketDetail = (id: number) => {
  return http.request<ApiResult<TicketDetail>>("get", `/api/tickets/${id}`);
};

export const addTicketComment = (
  id: number,
  data: AddTicketCommentRequest
) => {
  return http.request<ApiResult<TicketDetail>>(
    "post",
    `/api/tickets/${id}/comments`,
    { data }
  );
};

export const getTicketAssignees = () => {
  return http.request<ApiResult<TicketAssigneeOption[]>>(
    "get",
    "/api/tickets/assignees"
  );
};

export const updateTicketStatus = (
  id: number,
  data: UpdateTicketStatusRequest
) => {
  return http.request<ApiResult<TicketDetail>>(
    "post",
    `/api/tickets/${id}/status`,
    { data }
  );
};

export const assignTicket = (id: number, data: AssignTicketRequest) => {
  return http.request<ApiResult<TicketDetail>>(
    "post",
    `/api/tickets/${id}/assignee`,
    { data }
  );
};

export const createTicketKnowledgeDraft = (id: number) => {
  return http.request<ApiResult<{ id: number }>>(
    "post",
    `/api/tickets/${id}/knowledge-draft`
  );
};

export const createTicket = (data: CreateTicketRequest) => {
  return http.request<ApiResult<TicketDetail>>("post", "/api/tickets", {
    data
  });
};

export const getTicketStats = () => {
  return http.request<ApiResult<TicketStats>>("get", "/api/tickets/stats");
};

