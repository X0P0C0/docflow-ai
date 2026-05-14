export function createAuthUser(overrides: Partial<{
  id: number
  username: string
  nickname: string
  realName: string
  roles: string[]
  permissions: string[]
  capabilities: string[]
}> = {}) {
  return {
    id: 7,
    username: 'support01',
    nickname: '李晓安',
    realName: '李晓安',
    roles: [],
    permissions: [],
    capabilities: [],
    ...overrides,
  }
}

export function createTicketDetailFixture(overrides: Partial<ReturnType<typeof createTicketDetailBase>> = {}) {
  return {
    ...createTicketDetailBase(),
    ...overrides,
  }
}

export function createDefaultAssigneeOptions() {
  return [
    { id: 101, username: 'support01', displayName: '李晓安', roles: ['SUPPORT'] },
    { id: 102, username: 'support02', displayName: '林哲', roles: ['SUPPORT'] },
    { id: 103, username: 'admin', displayName: '系统管理员', roles: ['ADMIN'] },
  ]
}

function createTicketDetailBase() {
  return {
    id: 101,
    ticketNo: 'TK-101',
    title: '支付回调失败',
    content: '用户支付成功后订单仍显示待支付，需要排查回调链路。',
    type: 'INCIDENT',
    categoryId: 3,
    priority: 3,
    priorityLabel: '高优先级',
    status: 2,
    statusLabel: '处理中',
    submitUserId: 7,
    assigneeUserId: 101,
    submitterName: '王小明',
    assigneeName: '李晓安',
    linkedKnowledgeArticleCount: 0,
    latestLinkedKnowledgeArticle: null,
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:00:00',
    sourceKnowledgeArticles: [],
    timeline: [
      {
        id: 1,
        operatorName: '李晓安',
        title: '已开始排查',
        desc: '先确认支付网关回调日志。',
        createTime: '2026-05-14T10:00:00',
      },
    ],
    comments: [
      {
        id: 1,
        authorName: '王小明',
        content: '用户反馈支付成功但页面未刷新。',
        commentTypeLabel: '普通评论',
        internal: false,
        createTime: '2026-05-14T09:30:00',
      },
    ],
    relatedArticles: [],
  }
}
