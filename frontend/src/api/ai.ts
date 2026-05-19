import { get } from './http'
import { del } from './http'
import { post } from './http'

export interface AiWorkspaceOverview {
  pendingSuggestions: number
  adoptedSuggestions: number
  knowledgeRecommendations: number
}

export interface AiReplySuggestion {
  ticketId?: number | null
  statusKey?: string
  adopted?: boolean
  adoptedByUserId?: number | null
  adoptedByName?: string
  adoptedAt?: string
  lastActivityAt?: string
  claimFreshness?: string
  ticketNo?: string
  title: string
  summary: string
  scene: string
  confidence: string
  checklist: string[]
}

export interface AiKnowledgeRecommendation {
  articleId?: number | null
  title: string
  reason: string
  matchRate: string
}

export interface AiFeedItem {
  title: string
  value: string
}

export interface AiFollowupItem {
  ticketId?: number | null
  queueKey?: string
  statusKey?: string
  adopted?: boolean
  adoptedByUserId?: number | null
  adoptedByName?: string
  adoptedAt?: string
  lastActivityAt?: string
  claimFreshness?: string
  title: string
  desc: string
  chip: string
  chipClass: string
}

export interface AiWorkspaceResponse {
  generatedAt: string
  heuristicBased: boolean
  overview: AiWorkspaceOverview
  primarySuggestion: AiReplySuggestion
  recommendations: AiKnowledgeRecommendation[]
  feed: AiFeedItem[]
  followups: AiFollowupItem[]
  adoptedTicketIds?: number[]
}

export interface AiReplyDraftResponse {
  ticketId: number
  statusKey?: string
  adopted?: boolean
  adoptedByUserId?: number | null
  adoptedByName?: string
  adoptedAt?: string
  lastActivityAt?: string
  claimFreshness?: string
  ticketNo: string
  ticketTitle: string
  scene: string
  confidence: string
  opener: string
  diagnosis: string
  nextStep: string
  customerReply: string
  operatorNotes: string[]
  relatedKnowledge: AiKnowledgeRecommendation[]
}

export function fetchAiWorkspace() {
  return get<AiWorkspaceResponse>('/api/ai/workspace')
}

export function fetchAiReplyDraft(ticketId: number) {
  return get<AiReplyDraftResponse>(`/api/ai/workspace/reply-drafts/${ticketId}`)
}

export interface AiWorkspaceAdoptionResponse {
  ticketId: number
  adopted: boolean
  adoptedByUserId?: number | null
  adoptedByName?: string
  adoptedAt?: string
  lastActivityAt?: string
  claimFreshness?: string
}

export function adoptAiReplyDraft(ticketId: number) {
  return post<AiWorkspaceAdoptionResponse>(`/api/ai/workspace/reply-drafts/${ticketId}/adopt`)
}

export function unadoptAiReplyDraft(ticketId: number) {
  return del<AiWorkspaceAdoptionResponse>(`/api/ai/workspace/reply-drafts/${ticketId}/adopt`)
}
