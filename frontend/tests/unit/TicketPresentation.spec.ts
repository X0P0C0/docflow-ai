import { describe, expect, it } from 'vitest'
import type { TicketDetailApiItem } from '../../src/api/ticket'
import {
  formatDateText,
  formatTicketDetailItem,
  formatTicketListItem,
  mapPriorityClass,
  toKnowledgeArticleStatusText,
  toPriorityText,
  toStatusText,
  toTicketTypeText,
} from '../../src/utils/ticketPresentation'

function createTicketDetailFixture(overrides: Partial<TicketDetailApiItem> = {}): TicketDetailApiItem {
  return {
    id: 701,
    ticketNo: 'INC-701',
    title: '支付回调接口偶发超时',
    content: '用户支付成功后订单仍停留在待支付状态。',
    type: 'INCIDENT',
    categoryId: 3,
    priority: 4,
    priorityLabel: '紧急',
    status: 2,
    statusLabel: '处理中',
    submitUserId: 7,
    assigneeUserId: null,
    submitterName: '王小明',
    assigneeName: '',
    linkedKnowledgeArticleCount: 1,
    latestLinkedKnowledgeArticle: {
      id: 88,
      title: '支付回调失败排查手册',
      status: 1,
      statusLabel: 'Published',
      publishTime: null,
      updateTime: '2026-05-14T09:05:00',
    },
    sourceKnowledgeArticles: [],
    timeline: [
      {
        id: 1,
        operatorName: '李晓安',
        title: 'Status Updated',
        desc: '工单已进入处理中。',
        createTime: '2026-05-14T10:10:00',
      },
    ],
    comments: [
      {
        id: 1,
        authorName: '李晓安',
        content: '已定位到回调重试异常。',
        commentTypeLabel: '处理说明',
        internal: true,
        createTime: '2026-05-14T10:12:00',
      },
    ],
    relatedArticles: [
      {
        id: 12,
        title: '发布异常应急处理流程',
        summary: '...',
        reason: '与支付回调链路存在相似排查路径。',
      },
    ],
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:15:00',
    ...overrides,
  }
}

describe('ticket presentation helpers', () => {
  it('normalizes primitive date, priority, type, and status helpers', () => {
    expect(formatDateText('2026-05-14T10:15:00')).toBe('2026-05-14 10:15:00')
    expect(mapPriorityClass(4)).toBe('chip-red')
    expect(mapPriorityClass(3)).toBe('chip-orange')
    expect(mapPriorityClass(1)).toBe('chip-green')
    expect(toTicketTypeText('QUESTION')).toBe('问题咨询')
    expect(toTicketTypeText(undefined)).toBe('工单')
    expect(toPriorityText(99, '自定义')).toBe('自定义')
    expect(toStatusText(0, 'Resolved')).toBe('已解决')
    expect(toStatusText(0, 'New')).toBe('新建')
    expect(toStatusText(0, 'In Progress')).toBe('处理中')
    expect(toStatusText(0, 'Closed')).toBe('已关闭')
    expect(toStatusText(0, null)).toBe('未知状态')
    expect(toKnowledgeArticleStatusText(0, null)).toBe('草稿')
    expect(toKnowledgeArticleStatusText(99, 'Published')).toBe('已发布')
    expect(toKnowledgeArticleStatusText(99, 'Archived')).toBe('已归档')
    expect(toKnowledgeArticleStatusText(99, 'Draft')).toBe('草稿')
  })

  it('formats ticket list items with fallback assignee names and linked article context', () => {
    const result = formatTicketListItem(createTicketDetailFixture())

    expect(result.meta).toBe('INC-701 · 待分配 · 更新于 2026-05-14 10:15:00')
    expect(result.priorityLevel).toBe('P4')
    expect(result.priority).toBe('紧急')
    expect(result.status).toBe('处理中')
    expect(result.priorityClass).toBe('chip-red')
    expect(result.assignee).toBe('待分配')
    expect(result.linkedKnowledgeArticleCount).toBe(1)
    expect(result.latestLinkedKnowledgeArticle).toEqual({
      id: 88,
      title: '支付回调失败排查手册',
      status: '已发布',
      updatedAt: '2026-05-14 09:05:00',
    })
    expect(result.tags).toEqual(['故障事件', '分类 3'])
  })

  it('formats ticket detail items with knowledge context, timeline, comments, and related articles', () => {
    const result = formatTicketDetailItem(createTicketDetailFixture())

    expect(result.knowledgeContextSummary).toEqual({
      status: '处理中',
      assignee: '待分配',
      commentCount: 1,
      timelineCount: 1,
      latestTimelineTitle: 'Status Updated',
      latestTimelineAt: '2026-05-14 10:10:00',
    })
    expect(result.timeline).toEqual([
      {
        title: 'Status Updated',
        desc: '工单已进入处理中。',
        operator: '李晓安',
        createdAt: '2026-05-14 10:10:00',
      },
    ])
    expect(result.comments).toEqual([
      {
        author: '李晓安',
        content: '已定位到回调重试异常。',
        typeLabel: '处理说明',
        internal: true,
        createdAt: '2026-05-14 10:12:00',
      },
    ])
    expect(result.relatedArticles).toEqual([
      {
        id: 12,
        title: '发布异常应急处理流程',
        reason: '与支付回调链路存在相似排查路径。',
      },
    ])
  })

  it('falls back to category and status placeholders when backend values are missing or unknown', () => {
    const result = formatTicketListItem(createTicketDetailFixture({
      type: 'CUSTOM',
      categoryId: null,
      priority: 0,
      priorityLabel: null,
      status: 99,
      statusLabel: null,
      latestLinkedKnowledgeArticle: null,
    }))

    expect(result.priority).toBe('未指定')
    expect(result.status).toBe('未知状态')
    expect(result.tags).toEqual(['CUSTOM', '分类 -'])
    expect(result.latestLinkedKnowledgeArticle).toBeNull()
  })

  it('falls back to update times and empty-detail summaries when optional backend detail fields are absent', () => {
    const result = formatTicketDetailItem(createTicketDetailFixture({
      assigneeName: '处理人甲',
      latestLinkedKnowledgeArticle: {
        id: 89,
        title: '只存在更新时间的文章',
        status: 2,
        statusLabel: 'Archived',
        publishTime: null,
        updateTime: '2026-05-14T09:08:00',
      },
      timeline: [],
      comments: [],
      relatedArticles: [],
    }))

    expect(result.assignee).toBe('处理人甲')
    expect(result.latestLinkedKnowledgeArticle).toEqual({
      id: 89,
      title: '只存在更新时间的文章',
      status: '已归档',
      updatedAt: '2026-05-14 09:08:00',
    })
    expect(result.knowledgeContextSummary).toEqual({
      status: '处理中',
      assignee: '处理人甲',
      commentCount: 0,
      timelineCount: 0,
      latestTimelineTitle: undefined,
      latestTimelineAt: undefined,
    })
    expect(result.timeline).toEqual([])
    expect(result.comments).toEqual([])
    expect(result.relatedArticles).toEqual([])
  })
})
