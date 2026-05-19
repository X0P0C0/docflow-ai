import { get, post } from './http'
import type { KnowledgeArticleApiItem } from '../types/dashboard'

// 这里定义的是“前端视角下的工单协议模型”。
// 它尽量贴近后端返回结构，让页面层少做重复转换。
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
  // 详情接口不是再查一次列表字段，而是在列表基础上补齐时间线、评论和知识关联。
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
  // 列表查询统一在这里把筛选条件翻译成 query string，页面层不直接手拼 URL。
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
  // 详情页依赖的是聚合接口，而不是页面自己并发请求评论、时间线和相关文章。
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
  // 新建接口直接返回详情结构，方便创建后立刻跳详情页。
  return post<TicketDetailApiItem>('/api/tickets', payload)
}

export function createTicketKnowledgeDraft(id: number, payload: CreateTicketKnowledgeDraftPayload = {}) {
  // 这是“工单 -> 知识草稿”的跨模块入口，返回值已经切换成知识文章模型。
  return post<KnowledgeArticleApiItem>(`/api/tickets/${id}/knowledge-draft`, payload)
}
