export interface NavItem {
  name: string
  badge: string
  to: string
}

export interface MetricItem {
  tag: string
  value: string
  title: string
  desc: string
  chipClass: string
}

export interface TicketCommentItem {
  author: string
  content: string
  typeLabel: string
  internal: boolean
  createdAt: string
}

export interface TicketTimelineEntry {
  title: string
  desc: string
  operator?: string
  createdAt?: string
}

export interface TicketItem {
  id: number
  ticketNo?: string
  title: string
  meta: string
  submitUserId?: number
  assigneeUserId?: number | null
  content?: string
  priorityLevel?: string
  priority: string
  status: string
  priorityClass: string
  assignee?: string
  submitter?: string
  updatedAt?: string
  tags?: Array<string>
  timeline?: Array<TicketTimelineEntry>
  comments?: Array<TicketCommentItem>
  relatedArticles?: Array<{
    id: number
    title: string
    reason: string
  }>
  linkedKnowledgeArticleCount?: number
  latestLinkedKnowledgeArticle?: {
    id: number
    title: string
    status: string
    updatedAt: string
  } | null
  knowledgeContextSummary?: {
    status: string
    assignee: string
    commentCount: number
    timelineCount: number
    latestTimelineTitle?: string
    latestTimelineAt?: string
  } | null
  source?: 'local'
}

export interface KnowledgeArticleItem {
  id: number
  tag: string
  title: string
  summary: string
  author: string
  time: string
  metric: string
  tagClass: string
}

export interface ActivityItem {
  title: string
  desc: string
}

export interface KnowledgeArticleApiItem {
  id: number
  title: string
  summary: string
  content: string
  categoryId: number | null
  sourceTicketId?: number | null
  authorUserId: number
  status: number
  viewCount: number
  likeCount: number
  collectCount: number
  publishTime: string | null
  createTime: string
  updateTime: string
  versions?: KnowledgeArticleVersionItem[]
  sourceTicket?: {
    id: number
    ticketNo?: string
    title?: string
  }
}

export interface KnowledgeArticleVersionItem {
  id: number
  versionNo: number
  title: string
  summary: string
  remark: string
  operatorUserId: number
  createTime: string
}

export interface KnowledgeArticleDraft {
  id: number
  title: string
  summary: string
  content: string
  categoryId: number | null
  sourceTicketId?: number | null
  authorUserId: number
  status: number
  viewCount: number
  likeCount: number
  collectCount: number
  publishTime: string | null
  createTime: string
  updateTime: string
  versions?: KnowledgeArticleVersionItem[]
  sourceTicket?: {
    id: number
    ticketNo?: string
    title?: string
  }
  source: 'local'
}
