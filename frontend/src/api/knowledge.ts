import { del, get, post, put } from './http'
import type { KnowledgeArticleApiItem } from '../types/dashboard'

// knowledge.ts 把知识模块的所有远程读写动作收口起来，
// 页面层只表达“我要列、查、存、恢复”，不关心具体 URL 细节。
export interface KnowledgeArticleListQuery {
  keyword?: string
  categoryId?: number
  status?: number
  sourceTicketId?: number
  sourceTicketNo?: string
}

export function fetchKnowledgeArticles(query: KnowledgeArticleListQuery = {}) {
  // 知识列表筛选条件较多，统一在这里做 query 参数拼装，避免各页面各自实现。
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
  // 这个批量统计接口主要给工单列表 / 工作台做“知识沉淀数量”补充展示。
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
  // create / update 共用同一份 payload，保持编辑器保存逻辑简单一致。
  return post<KnowledgeArticleApiItem>('/api/knowledge/articles', payload)
}

export function updateKnowledgeArticle(id: number, payload: SaveKnowledgeArticlePayload) {
  return put<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}`, payload)
}

export function restoreKnowledgeArticleVersion(id: number, versionId: number) {
  // 恢复版本在前端看来仍是一种写操作，所以继续走显式 action 接口。
  return post<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}/versions/${versionId}/restore`)
}

export function archiveKnowledgeArticle(id: number) {
  return post<KnowledgeArticleApiItem>(`/api/knowledge/articles/${id}/archive`)
}

export function deleteKnowledgeArticle(id: number) {
  return del<void>(`/api/knowledge/articles/${id}`)
}
