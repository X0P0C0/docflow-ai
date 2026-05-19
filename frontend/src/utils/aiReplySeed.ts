import type { AiReplyDraftResponse } from '../api/ai'
import { getSafeSessionStorage } from './safeStorage'

const STORAGE_KEY = 'docflow.ai.replyDraftSeed'
const storage = getSafeSessionStorage()

export interface AiReplyDraftSeed {
  ticketId: number
  content: string
  commentType: number
  internal: boolean
}

export function saveAiReplyDraftSeed(draft: AiReplyDraftResponse) {
  const seed: AiReplyDraftSeed = {
    ticketId: draft.ticketId,
    content: draft.customerReply,
    commentType: 2,
    internal: false,
  }
  storage.setItem(STORAGE_KEY, JSON.stringify(seed))
}

export function consumeAiReplyDraftSeed() {
  const raw = storage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }

  storage.removeItem(STORAGE_KEY)
  try {
    return JSON.parse(raw) as AiReplyDraftSeed
  } catch {
    return null
  }
}
