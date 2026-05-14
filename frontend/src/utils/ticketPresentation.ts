import type { TicketApiItem, TicketDetailApiItem } from '../api/ticket'
import type { TicketItem } from '../types/dashboard'

export function formatDateText(value: string) {
  return value.replace('T', ' ')
}

export function mapPriorityClass(priority: number) {
  if (priority >= 4) {
    return 'chip-red'
  }
  if (priority === 3) {
    return 'chip-orange'
  }
  return 'chip-green'
}

export function toTicketTypeText(type: string | null | undefined) {
  if (type === 'INCIDENT') {
    return '故障事件'
  }
  if (type === 'QUESTION') {
    return '问题咨询'
  }
  if (type === 'TASK') {
    return '处理任务'
  }
  return type || '工单'
}

export function toPriorityText(priority: number, priorityLabel?: string | null) {
  if (priority === 4) {
    return '紧急'
  }
  if (priority === 3) {
    return '高优先级'
  }
  if (priority === 2) {
    return '普通'
  }
  if (priority === 1) {
    return '低优先级'
  }
  return priorityLabel || '未指定'
}

export function toStatusText(status: number, statusLabel?: string | null) {
  if (status === 1) {
    return '新建'
  }
  if (status === 2) {
    return '处理中'
  }
  if (status === 3) {
    return '已解决'
  }
  if (status === 4) {
    return '已关闭'
  }
  if (statusLabel === 'New') {
    return '新建'
  }
  if (statusLabel === 'In Progress') {
    return '处理中'
  }
  if (statusLabel === 'Resolved') {
    return '已解决'
  }
  if (statusLabel === 'Closed') {
    return '已关闭'
  }
  return statusLabel || '未知状态'
}

export function toKnowledgeArticleStatusText(status: number, statusLabel?: string | null) {
  if (status === 1) {
    return '已发布'
  }
  if (status === 2) {
    return '已归档'
  }
  if (status === 0) {
    return '草稿'
  }
  if (statusLabel === 'Published') {
    return '已发布'
  }
  if (statusLabel === 'Archived') {
    return '已归档'
  }
  if (statusLabel === 'Draft') {
    return '草稿'
  }
  return statusLabel || '未知状态'
}

export function formatTicketListItem(ticket: TicketApiItem): TicketItem {
  const assigneeName = ticket.assigneeName || '待分配'
  return {
    id: ticket.id,
    ticketNo: ticket.ticketNo,
    title: ticket.title,
    meta: `${ticket.ticketNo} · ${assigneeName} · 更新于 ${formatDateText(ticket.updateTime)}`,
    submitUserId: ticket.submitUserId,
    assigneeUserId: ticket.assigneeUserId,
    content: ticket.content,
    priorityLevel: `P${ticket.priority}`,
    priority: toPriorityText(ticket.priority, ticket.priorityLabel),
    status: toStatusText(ticket.status, ticket.statusLabel),
    priorityClass: mapPriorityClass(ticket.priority),
    assignee: assigneeName,
    submitter: ticket.submitterName,
    updatedAt: formatDateText(ticket.updateTime),
    linkedKnowledgeArticleCount: ticket.linkedKnowledgeArticleCount ?? 0,
    latestLinkedKnowledgeArticle: ticket.latestLinkedKnowledgeArticle
      ? {
          id: ticket.latestLinkedKnowledgeArticle.id,
          title: ticket.latestLinkedKnowledgeArticle.title,
          status: toKnowledgeArticleStatusText(
            ticket.latestLinkedKnowledgeArticle.status,
            ticket.latestLinkedKnowledgeArticle.statusLabel,
          ),
          updatedAt: formatDateText(ticket.latestLinkedKnowledgeArticle.publishTime || ticket.latestLinkedKnowledgeArticle.updateTime),
        }
      : null,
    tags: [toTicketTypeText(ticket.type), `分类 ${ticket.categoryId ?? '-'}`],
    timeline: [],
  }
}

export function formatTicketDetailItem(ticket: TicketDetailApiItem): TicketItem {
  const assigneeName = ticket.assigneeName || '待分配'
  return {
    id: ticket.id,
    ticketNo: ticket.ticketNo,
    title: ticket.title,
    meta: `${ticket.ticketNo} · ${assigneeName} · 更新于 ${formatDateText(ticket.updateTime)}`,
    submitUserId: ticket.submitUserId,
    assigneeUserId: ticket.assigneeUserId,
    content: ticket.content,
    priorityLevel: `P${ticket.priority}`,
    priority: toPriorityText(ticket.priority, ticket.priorityLabel),
    status: toStatusText(ticket.status, ticket.statusLabel),
    priorityClass: mapPriorityClass(ticket.priority),
    assignee: assigneeName,
    submitter: ticket.submitterName,
    updatedAt: formatDateText(ticket.updateTime),
    linkedKnowledgeArticleCount: ticket.linkedKnowledgeArticleCount ?? 0,
    latestLinkedKnowledgeArticle: ticket.latestLinkedKnowledgeArticle
      ? {
          id: ticket.latestLinkedKnowledgeArticle.id,
          title: ticket.latestLinkedKnowledgeArticle.title,
          status: toKnowledgeArticleStatusText(
            ticket.latestLinkedKnowledgeArticle.status,
            ticket.latestLinkedKnowledgeArticle.statusLabel,
          ),
          updatedAt: formatDateText(ticket.latestLinkedKnowledgeArticle.publishTime || ticket.latestLinkedKnowledgeArticle.updateTime),
        }
      : null,
    knowledgeContextSummary: {
      status: toStatusText(ticket.status, ticket.statusLabel),
      assignee: assigneeName,
      commentCount: ticket.comments.length,
      timelineCount: ticket.timeline.length,
      latestTimelineTitle: ticket.timeline[0]?.title,
      latestTimelineAt: ticket.timeline[0]?.createTime ? formatDateText(ticket.timeline[0].createTime) : undefined,
    },
    tags: [toTicketTypeText(ticket.type), `分类 ${ticket.categoryId ?? '-'}`],
    timeline: ticket.timeline.map((item) => ({
      title: item.title,
      desc: item.desc,
      operator: item.operatorName,
      createdAt: formatDateText(item.createTime),
    })),
    comments: ticket.comments.map((item) => ({
      author: item.authorName,
      content: item.content,
      typeLabel: item.commentTypeLabel,
      internal: item.internal,
      createdAt: formatDateText(item.createTime),
    })),
    relatedArticles: ticket.relatedArticles.map((item) => ({
      id: item.id,
      title: item.title,
      reason: item.reason,
    })),
  }
}
