import { get, post } from './http'
import type { KnowledgeArticleApiItem } from '../types/dashboard'

export interface TicketApiItem {
  id: number
  ticketNo: string
  title: string
  content: string
  type: string
  categoryId: number | null
  priority: number
  priorityLabel: string
  status: number
  statusLabel: string
  submitUserId: number
  assigneeUserId: number | null
  submitterName: string
  assigneeName: string
  linkedKnowledgeArticleCount: number
  latestLinkedKnowledgeArticle: {
    id: number
    title: string
    status: number
    statusLabel: string
    publishTime: string | null
    updateTime: string
  } | null
  createTime: string
  updateTime: string
}

export interface TicketDetailApiItem extends TicketApiItem {
  sourceKnowledgeArticles: Array<{
    id: number
    title: string
    status: number
    statusLabel: string
    publishTime: string | null
    updateTime: string
  }>
  timeline: Array<{
    id: number
    operatorName: string
    title: string
    desc: string
    createTime: string
  }>
  comments: Array<{
    id: number
    authorName: string
    content: string
    commentTypeLabel: string
    internal: boolean
    createTime: string
  }>
  relatedArticles: Array<{
    id: number
    title: string
    summary: string
    reason: string
  }>
}

export interface TicketAssigneeOption {
  id: number
  username: string
  displayName: string
  roles: string[]
}

export interface AddTicketCommentPayload {
  content: string
  commentType?: number
  internal?: boolean
}

export interface UpdateTicketStatusPayload {
  status: number
  remark?: string
}

export interface CreateTicketKnowledgeDraftPayload {
  origin?: 'manual' | 'ticket-close'
  closeRemark?: string
}

export interface AssignTicketPayload {
  assigneeUserId: number
  remark?: string
}

export interface CreateTicketPayload {
  title: string
  content: string
  type: string
  categoryId: number
  priority: number
}

export interface TicketQueryParams {
  keyword?: string
  status?: number
  priority?: number
  type?: string
  assigneeUserId?: number
}

export function fetchTickets(params?: TicketQueryParams) {
  const query = new URLSearchParams()
  if (params?.keyword) {
    query.set('keyword', params.keyword)
  }
  if (params?.status) {
    query.set('status', String(params.status))
  }
  if (params?.priority) {
    query.set('priority', String(params.priority))
  }
  if (params?.type) {
    query.set('type', params.type)
  }
  if (params?.assigneeUserId) {
    query.set('assigneeUserId', String(params.assigneeUserId))
  }
  const suffix = query.toString() ? `?${query.toString()}` : ''
  return get<TicketApiItem[]>(`/api/tickets${suffix}`)
}

export function fetchTicketDetail(id: number) {
  return get<TicketDetailApiItem>(`/api/tickets/${id}`)
}

export function fetchTicketAssignees() {
  return get<TicketAssigneeOption[]>('/api/tickets/assignees')
}

export function addTicketComment(id: number, payload: AddTicketCommentPayload) {
  return post<TicketDetailApiItem>(`/api/tickets/${id}/comments`, payload)
}

export function updateTicketStatus(id: number, payload: UpdateTicketStatusPayload) {
  return post<TicketDetailApiItem>(`/api/tickets/${id}/status`, payload)
}

export function assignTicket(id: number, payload: AssignTicketPayload) {
  return post<TicketDetailApiItem>(`/api/tickets/${id}/assignee`, payload)
}

export function createTicket(payload: CreateTicketPayload) {
  return post<TicketDetailApiItem>('/api/tickets', payload)
}

export function createTicketKnowledgeDraft(id: number, payload: CreateTicketKnowledgeDraftPayload = {}) {
  return post<KnowledgeArticleApiItem>(`/api/tickets/${id}/knowledge-draft`, payload)
}
