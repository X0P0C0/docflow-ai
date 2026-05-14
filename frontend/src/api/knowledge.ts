import { del, get, post, put } from './http'
import type { KnowledgeArticleApiItem } from '../types/dashboard'

export interface KnowledgeArticleListQuery {
  keyword?: string
  categoryId?: number
  status?: number
  sourceTicketId?: number
  sourceTicketNo?: string
}

export function fetchKnowledgeArticles(query: KnowledgeArticleListQuery = {}) {
  const params = new URLSearchParams()

  if (query.keyword) {
    params.set('keyword', query.keyword)
  }
  if (query.categoryId !== undefined) {
    params.set('categoryId', String(query.categoryId))
  }
  if (query.status !== undefined) {
    params.set('status', String(query.status))
  }
  if (query.sourceTicketId !== undefined) {
    params.set('sourceTicketId', String(query.sourceTicketId))
  }
  if (query.sourceTicketNo) {
    params.set('sourceTicketNo', query.sourceTicketNo)
  }

  const search = params.toString()
  const url = search ? `/api/knowledge/articles?${search}` : '/api/knowledge/articles'
  return get<KnowledgeArticleApiItem[]>(url)
}

export function fetchKnowledgeArticleDetail(id: number) {
  return get<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}`)
}

export function fetchKnowledgeArticleSourceTicketCounts(ticketIds: number[]) {
  const params = new URLSearchParams()
  ticketIds.forEach((ticketId) => {
    params.append('ticketIds', String(ticketId))
  })
  const search = params.toString()
  return get<Record<string, number>>(`/api/knowledge/articles/source-ticket-counts${search ? `?${search}` : ''}`)
}

export interface SaveKnowledgeArticlePayload {
  title: string
  summary: string
  content: string
  categoryId: number | null
  sourceTicketId?: number | null
  status: number
}

export function createKnowledgeArticle(payload: SaveKnowledgeArticlePayload) {
  return post<KnowledgeArticleApiItem>('/api/knowledge/articles', payload)
}

export function updateKnowledgeArticle(id: number, payload: SaveKnowledgeArticlePayload) {
  return put<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}`, payload)
}

export function restoreKnowledgeArticleVersion(id: number, versionId: number) {
  return post<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}/versions/${versionId}/restore`)
}

export function archiveKnowledgeArticle(id: number) {
  return post<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}/archive`)
}

export function deleteKnowledgeArticle(id: number) {
  return del<void>(`/api/knowledge/articles/${id}`)
}
