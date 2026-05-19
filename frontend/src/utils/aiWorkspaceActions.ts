import { getSafeLocalStorage } from './safeStorage'

const STORAGE_KEY = 'docflow.ai.workspaceActions'
const storage = getSafeLocalStorage()

export interface AiWorkspaceActionState {
  adoptedTicketIds: number[]
}

function readState(): AiWorkspaceActionState {
  const raw = storage.getItem(STORAGE_KEY)
  if (!raw) {
    return { adoptedTicketIds: [] }
  }

  try {
    const parsed = JSON.parse(raw) as Partial<AiWorkspaceActionState>
    return {
      adoptedTicketIds: Array.isArray(parsed.adoptedTicketIds)
        ? parsed.adoptedTicketIds.filter((item): item is number => typeof item === 'number')
        : [],
    }
  } catch {
    return { adoptedTicketIds: [] }
  }
}

function writeState(state: AiWorkspaceActionState) {
  storage.setItem(STORAGE_KEY, JSON.stringify(state))
}

export function listAdoptedAiReplyTicketIds() {
  return readState().adoptedTicketIds
}

export function markAiReplyDraftAdopted(ticketId: number) {
  const state = readState()
  if (state.adoptedTicketIds.includes(ticketId)) {
    return state.adoptedTicketIds
  }
  const nextIds = [...state.adoptedTicketIds, ticketId]
  writeState({
    adoptedTicketIds: nextIds,
  })
  return nextIds
}

export function unmarkAiReplyDraftAdopted(ticketId: number) {
  const state = readState()
  const nextIds = state.adoptedTicketIds.filter((item) => item !== ticketId)
  writeState({
    adoptedTicketIds: nextIds,
  })
  return nextIds
}
