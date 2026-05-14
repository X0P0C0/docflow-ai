import type { TicketItem } from '../types/dashboard'

const STORAGE_KEY = 'docflow.ai.knowledgeDraftFromTicket'

export interface KnowledgeDraftSeed {
  title: string
  summary: string
  content: string
  categoryId: number
  sourceTicketId: number
  sourceTicketNo?: string
  sourceTicketTitle?: string
  origin?: 'manual' | 'ticket-close'
  closeRemark?: string
  ticketStatus?: string
}

export function buildKnowledgeDraftFromTicket(
  ticket: TicketItem,
  options?: {
    origin?: 'manual' | 'ticket-close'
    closeRemark?: string
  },
): KnowledgeDraftSeed {
  const categoryId = inferCategoryId(ticket)
  const summary = [
    `来源工单：${ticket.ticketNo || `#${ticket.id}`}`,
    `适用场景：${ticket.status}工单的处理复盘与经验沉淀。`,
    ticket.content ? `问题概述：${ticket.content.slice(0, 48)}${ticket.content.length > 48 ? '...' : ''}` : '',
  ]
    .filter(Boolean)
    .join(' ')

  const timelineSection = (ticket.timeline || [])
    .slice(0, 5)
    .map((item, index) => `${index + 1}. ${item.title}${item.createdAt ? `（${item.createdAt}）` : ''}\n${item.desc}`)
    .join('\n\n')

  const commentSection = (ticket.comments || [])
    .slice(0, 5)
    .map((item, index) => `${index + 1}. ${item.author} · ${item.typeLabel}${item.internal ? ' · 内部可见' : ''}\n${item.content}`)
    .join('\n\n')

  const closeRemarkSection = options?.closeRemark?.trim()
    ? `七、关闭备注\n${options.closeRemark.trim()}`
    : ''

  const content = [
    `一、问题现象\n${ticket.content || '请补充工单现象描述。'}`,
    '二、影响范围\n请补充受影响用户、业务链路和紧急程度。',
    `三、处理过程\n${timelineSection || '请补充关键处理步骤、时间线和定位动作。'}`,
    `四、关键信息\n提交人：${ticket.submitter || '待补充'}\n处理人：${ticket.assignee || '待补充'}\n当前状态：${ticket.status}`,
    `五、协作记录\n${commentSection || '请补充评论区里的关键结论、日志和内部备注。'}`,
    '六、结论与预防\n请补充最终根因、解决方案、预防动作和后续建议。',
    closeRemarkSection,
  ].join('\n\n')

  return {
    title: `${ticket.title}处理复盘`,
    summary,
    content,
    categoryId,
    sourceTicketId: ticket.id,
    sourceTicketNo: ticket.ticketNo,
    sourceTicketTitle: ticket.title,
    origin: options?.origin || 'manual',
    closeRemark: options?.closeRemark?.trim() || undefined,
    ticketStatus: ticket.status,
  }
}

export function saveKnowledgeDraftSeed(seed: KnowledgeDraftSeed) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(seed))
}

export function consumeKnowledgeDraftSeed() {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }

  sessionStorage.removeItem(STORAGE_KEY)
  try {
    return JSON.parse(raw) as KnowledgeDraftSeed
  } catch {
    return null
  }
}

function inferCategoryId(ticket: TicketItem) {
  const content = `${ticket.title} ${ticket.content || ''} ${(ticket.tags || []).join(' ')}`.toLowerCase()
  if (content.includes('支付') || content.includes('订单')) {
    return 3
  }
  if (content.includes('使用') || content.includes('指南') || content.includes('咨询')) {
    return 1
  }
  return 2
}
