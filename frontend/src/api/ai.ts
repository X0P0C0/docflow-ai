import { http } from "@/utils/http";

type ApiResult<T> = {
  code: number;
  message: string;
  data: T;
};

type AiWorkspaceOverview = {
  pendingSuggestions: number;
  adoptedSuggestions: number;
  knowledgeRecommendations: number;
};

export type AiReplySuggestion = {
  ticketId: number;
  ticketNo: string;
  title: string;
  summary: string;
  confidence: string;
  checklist: string[];
  adopted: boolean;
};

export type AiFeedItem = {
  title: string;
  value: string;
};

export type AiKnowledgeRecommendation = {
  articleId: number;
  ticketNo: string;
  title: string;
  reason: string;
  matchRate: string;
};

export type AiFollowupItem = {
  ticketId: number;
  title: string;
  desc: string;
  chip: string;
  chipClass: string;
};

export type AiReplyDraft = {
  ticketId: number;
  statusKey: string;
  adopted: boolean;
  adoptedByUserId: number | null;
  adoptedByName: string | null;
  adoptedAt: string | null;
  ticketNo: string;
  ticketTitle: string;
  scene: string;
  confidence: string;
  opener: string;
  diagnosis: string;
  nextStep: string;
  customerReply: string;
  operatorNotes: string[];
  relatedKnowledge: AiKnowledgeRecommendation[];
};

export type AiWorkspace = {
  heuristicBased: boolean;
  overview: AiWorkspaceOverview;
  primarySuggestion: AiReplySuggestion | null;
  feed: AiFeedItem[];
  recommendations: AiKnowledgeRecommendation[];
  followups: AiFollowupItem[];
};

export const getAiWorkspace = () => {
  return http.request<ApiResult<AiWorkspace>>("get", "/api/ai/workspace");
};

export const getReplyDraft = (ticketId: number) => {
  return http.request<ApiResult<AiReplyDraft>>(
    "get",
    `/api/ai/workspace/reply-drafts/${ticketId}`
  );
};

export const adoptReplyDraft = (ticketId: number) => {
  return http.request<ApiResult<null>>(
    "post",
    `/api/ai/workspace/reply-drafts/${ticketId}/adopt`
  );
};

export const unadoptReplyDraft = (ticketId: number) => {
  return http.request<ApiResult<null>>(
    "delete",
    `/api/ai/workspace/reply-drafts/${ticketId}/adopt`
  );
};