import { authState } from '../auth'
import { tickets as fallbackTickets } from './dashboard'
import type { TicketCommentItem, TicketItem, TicketTimelineEntry } from '../types/dashboard'

const STORAGE_KEY = 'docflow.ai.localTickets'

export interface LocalTicketRecord extends TicketItem {
  source: 'local'
}

function readTickets() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return []
  }

  try {
    return JSON.parse(raw) as LocalTicketRecord[]
  } catch {
    localStorage.removeItem(STORAGE_KEY)
    return []
  }
}

function writeTickets(items: LocalTicketRecord[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
}

function nowText() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ')
}

function currentUserName() {
  return authState.user?.nickname || authState.user?.realName || authState.user?.username || '当前用户'
}

function currentUserId() {
  return authState.user?.id
}

function nextTicketId(items: LocalTicketRecord[]) {
  return items.reduce((max, item) => Math.max(max, item.id), 10000) + 1
}

function nextTicketNo(items: LocalTicketRecord[]) {
  return `LOCAL-${nextTicketId(items)}`
}

export function listLocalTickets() {
  return readTickets().sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''))
}

export function mergeTickets(remote: TicketItem[]) {
  const locals = listLocalTickets()
  const merged = new Map<number, TicketItem | LocalTicketRecord>()

  locals.forEach((item) => merged.set(item.id, item))
  remote.forEach((item) => {
    if (!merged.has(item.id)) {
      merged.set(item.id, item)
    }
  })

  return Array.from(merged.values())
}

export function getLocalTicket(id: number) {
  return listLocalTickets().find((item) => item.id === id) || null
}

export function createLocalTicket(payload: {
  title: string
  content: string
  type: string
  categoryId: number
  priority: number
}) {
  const items = listLocalTickets()
  const updatedAt = nowText()
  const priorityMap: Record<number, { level: string; label: string; chip: string }> = {
    1: { level: 'P4', label: '低优先级', chip: 'chip-green' },
    2: { level: 'P3', label: '普通', chip: 'chip-green' },
    3: { level: 'P2', label: '高优先级', chip: 'chip-orange' },
    4: { level: 'P1', label: '紧急', chip: 'chip-red' },
  }

  const priorityMeta = priorityMap[payload.priority] || priorityMap[2]
  const ticketNo = nextTicketNo(items)
  const actor = currentUserName()
  const actorUserId = currentUserId()

  const ticket: LocalTicketRecord = {
    id: nextTicketId(items),
    ticketNo,
    title: payload.title,
    meta: `${ticketNo} · 待分配 · 更新于 ${updatedAt}`,
    submitUserId: actorUserId,
    assigneeUserId: null,
    content: payload.content,
    priorityLevel: priorityMeta.level,
    priority: priorityMeta.label,
    status: '新建',
    priorityClass: priorityMeta.chip,
    assignee: '待分配',
    submitter: actor,
    updatedAt,
    tags: [payload.type, `分类 ${payload.categoryId}`],
    timeline: [
      {
        title: 'Ticket Created',
        desc: '本地工作流已创建工单，等待后端写接口稳定后再同步。',
        operator: actor,
        createdAt: updatedAt,
      },
    ],
    comments: [],
    source: 'local',
  }

  writeTickets([ticket, ...items])
  return ticket
}

export function saveLocalTicket(ticket: LocalTicketRecord) {
  const items = listLocalTickets()
  writeTickets([ticket, ...items.filter((item) => item.id !== ticket.id)])
  return ticket
}

export function appendLocalComment(
  ticket: LocalTicketRecord,
  payload: { content: string; internal: boolean; commentType: number },
) {
  const updatedAt = nowText()
  const typeMap: Record<number, string> = {
    1: '普通评论',
    2: '处理说明',
    3: '内部备注',
  }
  const nextComments: TicketCommentItem[] = [
    {
      author: currentUserName(),
      content: payload.content,
      typeLabel: typeMap[payload.commentType] || '普通评论',
      internal: payload.internal,
      createdAt: updatedAt,
    },
    ...(ticket.comments || []),
  ]

  const nextTimeline: TicketTimelineEntry[] = [
    {
      title: 'Comment Added',
      desc: payload.internal ? '新增了一条内部备注。' : '新增了一条协作评论。',
      operator: currentUserName(),
      createdAt: updatedAt,
    },
    ...(ticket.timeline || []),
  ]

  return saveLocalTicket({
    ...ticket,
    comments: nextComments,
    timeline: nextTimeline,
    updatedAt,
    meta: `${ticket.ticketNo} · ${ticket.assignee} · 更新于 ${updatedAt}`,
  })
}

export function updateLocalTicketStatus(ticket: LocalTicketRecord, status: number, remark: string) {
  const updatedAt = nowText()
  const statusMap: Record<number, string> = {
    1: '新建',
    2: '处理中',
    3: '已解决',
    4: '已关闭',
  }
  return saveLocalTicket({
    ...ticket,
    status: statusMap[status] || '新建',
    updatedAt,
    meta: `${ticket.ticketNo} · ${ticket.assignee} · 更新于 ${updatedAt}`,
    timeline: [
      {
        title: 'Status Updated',
        desc: remark || `工单状态已更新为${statusMap[status] || '新建'}。`,
        operator: currentUserName(),
        createdAt: updatedAt,
      },
      ...(ticket.timeline || []),
    ],
  })
}

export function assignLocalTicket(ticket: LocalTicketRecord, assigneeName: string, remark: string, assigneeUserId?: number) {
  const updatedAt = nowText()
  return saveLocalTicket({
    ...ticket,
    assigneeUserId: assigneeUserId ?? ticket.assigneeUserId ?? null,
    assignee: assigneeName,
    updatedAt,
    meta: `${ticket.ticketNo} · ${assigneeName} · 更新于 ${updatedAt}`,
    timeline: [
      {
        title: 'Ticket Assigned',
        desc: remark || `工单已指派给 ${assigneeName}。`,
        operator: currentUserName(),
        createdAt: updatedAt,
      },
      ...(ticket.timeline || []),
    ],
  })
}

export function seedFallbackLocals() {
  if (listLocalTickets().length > 0) {
    return
  }

  const items = fallbackTickets
    .filter((item) => item.id >= 9000)
    .map((item) => ({ ...item, source: 'local' as const }))
  if (items.length) {
    writeTickets(items)
  }
}
