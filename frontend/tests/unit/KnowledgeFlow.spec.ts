import { beforeEach, describe, expect, it } from 'vitest'
import { saveKnowledgeDraft } from '../../src/mock/knowledgeDrafts'
import {
  buildKnowledgeDraftFromTicket,
  consumeKnowledgeDraftSeed,
  saveKnowledgeDraftSeed,
} from '../../src/utils/knowledgeFromTicket'
import {
  attachArticleSourceTicket,
  countArticlesBySourceTicket,
  getArticleSourceTicket,
  listArticleSourceTickets,
  saveArticleSourceTicket,
} from '../../src/utils/knowledgeSourceTicket'

describe('knowledge flow helpers', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('builds a knowledge draft seed from ticket context and infers category from payment/order content', () => {
    const seed = buildKnowledgeDraftFromTicket({
      id: 701,
      ticketNo: 'INC-701',
      title: '支付订单回调异常',
      meta: 'INC-701 · 李晓安 · 更新于 2026-05-14 10:15:00',
      submitUserId: 7,
      assigneeUserId: 8,
      content: '用户支付成功后订单仍显示待支付，需要排查回调链路和重试逻辑。',
      priorityLevel: 'P1',
      priority: '紧急',
      status: '已解决',
      priorityClass: 'chip-red',
      assignee: '李晓安',
      submitter: '王小明',
      updatedAt: '2026-05-14 10:15:00',
      tags: ['故障事件', '分类 3'],
      timeline: [
        {
          title: 'Status Updated',
          desc: '确认回调重试配置错误并完成修复。',
          operator: '李晓安',
          createdAt: '2026-05-14 10:10:00',
        },
      ],
      comments: [
        {
          author: '李晓安',
          content: '已补充内部排查记录。',
          typeLabel: '内部备注',
          internal: true,
          createdAt: '2026-05-14 10:12:00',
        },
      ],
    }, {
      origin: 'ticket-close',
      closeRemark: ' 已通知业务方验证通过。 ',
    })

    expect(seed.title).toBe('支付订单回调异常处理复盘')
    expect(seed.categoryId).toBe(3)
    expect(seed.summary).toContain('来源工单：INC-701')
    expect(seed.content).toContain('三、处理过程')
    expect(seed.content).toContain('1. Status Updated（2026-05-14 10:10:00）')
    expect(seed.content).toContain('内部备注 · 内部可见')
    expect(seed.content).toContain('七、关闭备注')
    expect(seed.closeRemark).toBe('已通知业务方验证通过。')
    expect(seed.origin).toBe('ticket-close')
  })

  it('stores and consumes one-shot draft seeds from session storage', () => {
    const seed = {
      title: '知识草稿种子',
      summary: 'summary',
      content: 'content',
      categoryId: 2,
      sourceTicketId: 701,
      sourceTicketNo: 'INC-701',
      sourceTicketTitle: '工单标题',
      origin: 'manual' as const,
      ticketStatus: '处理中',
    }

    saveKnowledgeDraftSeed(seed)

    expect(consumeKnowledgeDraftSeed()).toEqual(seed)
    expect(consumeKnowledgeDraftSeed()).toBeNull()
  })

  it('falls back cleanly when ticket context is sparse or the saved seed is invalid', () => {
    const seed = buildKnowledgeDraftFromTicket({
      id: 801,
      ticketNo: '',
      title: '使用指南咨询',
      meta: 'meta',
      submitUserId: 1,
      assigneeUserId: null,
      content: '',
      priorityLevel: 'P3',
      priority: '普通',
      status: '处理中',
      priorityClass: 'chip-green',
      assignee: '',
      submitter: '',
      updatedAt: '2026-05-14 11:00:00',
      tags: ['问题咨询'],
      timeline: [],
      comments: [],
    })

    expect(seed.summary).toContain('来源工单：#801')
    expect(seed.categoryId).toBe(1)
    expect(seed.content).toContain('请补充工单现象描述。')
    expect(seed.content).toContain('请补充关键处理步骤、时间线和定位动作。')
    expect(seed.content).toContain('提交人：待补充')
    expect(seed.content).toContain('处理人：待补充')
    expect(seed.content).toContain('请补充评论区里的关键结论、日志和内部备注。')
    expect(seed.origin).toBe('manual')
    expect(seed.closeRemark).toBeUndefined()

    sessionStorage.setItem('docflow.ai.knowledgeDraftFromTicket', '{invalid-json')
    expect(consumeKnowledgeDraftSeed()).toBeNull()

    const genericSeed = buildKnowledgeDraftFromTicket({
      id: 802,
      ticketNo: 'TASK-802',
      title: '服务器巡检记录',
      meta: 'meta',
      submitUserId: 1,
      assigneeUserId: null,
      content: '补充例行巡检情况。',
      priorityLevel: 'P3',
      priority: '普通',
      status: '已关闭',
      priorityClass: 'chip-green',
      assignee: '运维值班',
      submitter: '系统机器人',
      updatedAt: '2026-05-14 11:30:00',
      tags: ['处理任务'],
      timeline: [],
      comments: [],
    })

    expect(genericSeed.categoryId).toBe(2)
  })

  it('persists article source tickets, counts draft and legacy links, and attaches fallback linkage', () => {
    saveArticleSourceTicket(9201, {
      id: 701,
      ticketNo: 'INC-701',
      title: '支付订单回调异常',
    })
    saveKnowledgeDraft({
      id: 9301,
      title: '支付回调排查草稿',
      summary: 'draft',
      content: 'draft',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
      sourceTicket: {
        id: 701,
        ticketNo: 'INC-701',
        title: '支付订单回调异常',
      },
    })

    expect(getArticleSourceTicket(9201)).toEqual({
      id: 701,
      ticketNo: 'INC-701',
      title: '支付订单回调异常',
    })
    expect(listArticleSourceTickets()).toEqual({
      '9201': {
        id: 701,
        ticketNo: 'INC-701',
        title: '支付订单回调异常',
      },
    })
    expect(countArticlesBySourceTicket(701)).toBe(1)
    expect(attachArticleSourceTicket({
      id: 9201,
      title: '远程文章',
      summary: 'summary',
      content: 'content',
      categoryId: 3,
      sourceTicketId: null,
      authorUserId: 7,
      status: 1,
      viewCount: 0,
      likeCount: 0,
      collectCount: 0,
      publishTime: '2026-05-14T10:00:00',
      createTime: '2026-05-14T10:00:00',
      updateTime: '2026-05-14T10:00:00',
    }).sourceTicket).toEqual({
      id: 701,
      ticketNo: 'INC-701',
      title: '支付订单回调异常',
    })
    expect(attachArticleSourceTicket({
      id: 9202,
      title: '带 sourceTicketId 的文章',
      summary: 'summary',
      content: 'content',
      categoryId: 3,
      sourceTicketId: 702,
      authorUserId: 7,
      status: 1,
      viewCount: 0,
      likeCount: 0,
      collectCount: 0,
      publishTime: '2026-05-14T10:00:00',
      createTime: '2026-05-14T10:00:00',
      updateTime: '2026-05-14T10:00:00',
    }).sourceTicket).toEqual({ id: 702 })

    saveArticleSourceTicket(9201, null)
    expect(getArticleSourceTicket(9201)).toBeNull()
  })
})
