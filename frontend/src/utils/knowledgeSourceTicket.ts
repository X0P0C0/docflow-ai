import { listKnowledgeDrafts } from '../mock/knowledgeDrafts'
import type { KnowledgeArticleApiItem, KnowledgeArticleDraft } from '../types/dashboard'

const STORAGE_KEY = 'docflow.ai.knowledgeArticleSourceTickets'

export interface KnowledgeSourceTicketLink {
  id: number
  ticketNo?: string
  title?: string
}

type SourceTicketMap = Record<string, KnowledgeSourceTicketLink>

function readMap(): SourceTicketMap {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return {}
  }

  try {
    return JSON.parse(raw) as SourceTicketMap
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return {}
  }
}

function writeMap(data: SourceTicketMap) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
}

export function saveArticleSourceTicket(articleId: number, sourceTicket: KnowledgeSourceTicketLink | null | undefined) {
  const data = readMap()
  if (!sourceTicket) {
    delete data[String(articleId)]
  } else {
    data[String(articleId)] = sourceTicket
  }
  writeMap(data)
}

export function getArticleSourceTicket(articleId: number) {
  return readMap()[String(articleId)] || null
}

export function listArticleSourceTickets() {
  return readMap()
}

export function countArticlesBySourceTicket(ticketId: number) {
  const legacyCount = Object.values(readMap()).filter((item) => item.id === ticketId).length
  const draftCount = listKnowledgeDrafts().filter((item) => (item.sourceTicket?.id ?? item.sourceTicketId) === ticketId).length
  return Math.max(legacyCount, draftCount)
}

export function attachArticleSourceTicket<T extends KnowledgeArticleApiItem | KnowledgeArticleDraft>(article: T): T {
  if (article.sourceTicket) {
    return article
  }

  if (article.sourceTicketId) {
    return {
      ...article,
      sourceTicket: {
        id: article.sourceTicketId,
      },
    }
  }

  const sourceTicket = getArticleSourceTicket(article.id)
  if (!sourceTicket) {
    return article
  }

  return {
    ...article,
    sourceTicket,
  }
}
