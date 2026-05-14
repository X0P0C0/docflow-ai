import type { KnowledgeArticleApiItem, KnowledgeArticleDraft } from '../types/dashboard'

const STORAGE_KEY = 'docflow.ai.knowledgeDrafts'

function readDrafts() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return []
  }

  try {
    return JSON.parse(raw) as KnowledgeArticleDraft[]
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return []
  }
}

function writeDrafts(drafts: KnowledgeArticleDraft[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(drafts))
}

export function listKnowledgeDrafts() {
  return readDrafts().sort((a, b) => new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime())
}

export function getKnowledgeDraft(id: number) {
  return listKnowledgeDrafts().find((item) => item.id === id) || null
}

export function saveKnowledgeDraft(
  payload: Pick<
    KnowledgeArticleDraft,
    'id' | 'title' | 'summary' | 'content' | 'categoryId' | 'authorUserId' | 'status' | 'publishTime' | 'sourceTicket'
  >,
) {
  const drafts = listKnowledgeDrafts()
  const now = new Date().toISOString().slice(0, 19)
  const nextId = payload.id || Date.now()
  const existing = drafts.find((item) => item.id === nextId)

  const draft: KnowledgeArticleDraft = {
    id: nextId,
    title: payload.title,
    summary: payload.summary,
    content: payload.content,
    categoryId: payload.categoryId,
    sourceTicketId: payload.sourceTicket?.id ?? existing?.sourceTicketId ?? existing?.sourceTicket?.id ?? null,
    authorUserId: payload.authorUserId,
    status: payload.status,
    viewCount: existing?.viewCount ?? 0,
    likeCount: existing?.likeCount ?? 0,
    collectCount: existing?.collectCount ?? 0,
    publishTime: payload.publishTime,
    createTime: existing?.createTime ?? now,
    updateTime: now,
    sourceTicket: payload.sourceTicket ?? existing?.sourceTicket,
    source: 'local',
  }

  const nextDrafts = [draft, ...drafts.filter((item) => item.id !== nextId)]
  writeDrafts(nextDrafts)
  return draft
}

export function removeKnowledgeDraft(id: number) {
  const drafts = listKnowledgeDrafts().filter((item) => item.id !== id)
  writeDrafts(drafts)
}

export function updateKnowledgeDraftStatus(id: number, status: number) {
  const drafts = listKnowledgeDrafts()
  const target = drafts.find((item) => item.id === id)
  if (!target) {
    return null
  }

  target.status = status
  target.updateTime = new Date().toISOString().slice(0, 19)
  writeDrafts(drafts)
  return target
}

export function mergeKnowledgeArticles(
  articles: KnowledgeArticleApiItem[],
  drafts: KnowledgeArticleDraft[] = listKnowledgeDrafts(),
) {
  const merged = new Map<number, KnowledgeArticleApiItem | KnowledgeArticleDraft>()

  drafts.forEach((item) => {
    merged.set(item.id, item)
  })

  articles.forEach((item) => {
    if (!merged.has(item.id)) {
      merged.set(item.id, item)
    }
  })

  return Array.from(merged.values()).sort(
    (a, b) => new Date(b.publishTime || b.updateTime).getTime() - new Date(a.publishTime || a.updateTime).getTime(),
  )
}
