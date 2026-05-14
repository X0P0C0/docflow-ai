import { CAPABILITY_CODES } from './capability-constants'

export const ROUTE_CAPABILITY_REQUIREMENTS: Record<string, string> = {
  '/knowledge/articles/create': CAPABILITY_CODES.KNOWLEDGE_MANAGE,
  '/knowledge/articles/:id/edit': CAPABILITY_CODES.KNOWLEDGE_MANAGE,
  '/ai-center': CAPABILITY_CODES.AI_CENTER_ACCESS,
  '/settings': CAPABILITY_CODES.SYSTEM_MANAGE,
}

export const CAPABILITY_PRESENTATION = [
  {
    code: CAPABILITY_CODES.TICKET_OPERATE,
    label: '工单处理',
    description: '决定你是否能参与工单处理侧动作，而不只是查看自己提交的问题。',
  },
  {
    code: CAPABILITY_CODES.TICKET_VIEW_ALL,
    label: '全量工单视角',
    description: '决定工单列表是工作台视角还是仅展示与你相关的工单。',
  },
  {
    code: CAPABILITY_CODES.KNOWLEDGE_MANAGE,
    label: '知识沉淀管理',
    description: '决定知识草稿生成、编辑、发布和归档这些沉淀动作是否可见。',
  },
  {
    code: CAPABILITY_CODES.AI_CENTER_ACCESS,
    label: 'AI 工作台',
    description: '决定侧边导航里的 AI 工作台入口是否开放。',
  },
  {
    code: CAPABILITY_CODES.SYSTEM_MANAGE,
    label: '系统管理',
    description: '决定系统管理相关入口和后续管理动作是否对当前账号开放。',
  },
] as const

export function getRouteRequiredCapability(target: string) {
  return ROUTE_CAPABILITY_REQUIREMENTS[target] || null
}

export function buildAiCenterAccessCopy(options: { canAccess: boolean; roleText: string }) {
  if (options.canAccess) {
    return {
      chipText: 'AI Focus',
      title: '今天的协作效率提升 18%',
      description: 'AI 已为知识文章生成摘要，并为待处理工单提供回复建议和相关文章推荐。',
      actionText: '查看 AI 面板',
    }
  }

  return {
    chipText: '权限提示',
    title: '当前账号暂未开放 AI 工作台',
    description: `当前登录身份为 ${options.roleText}，你仍然可以先完成工单流转和知识沉淀主线。`,
    actionText: '当前能力未开放',
  }
}

export function buildProfileCapabilitySummaries(options: {
  canOperateTickets: boolean
  canViewAllTickets: boolean
  canManageKnowledge: boolean
}) {
  return {
    roleSummary: options.canOperateTickets
      ? '当前身份偏向处理侧，可以继续验证工单指派、状态流转和知识沉淀。'
      : '当前身份偏向提单侧，更适合查看自己的问题进展并补充业务反馈。',
    ticketScopeSummary: options.canViewAllTickets
      ? '工单列表会展示工作台范围内的可处理工单，详情页也保留完整处理动作。'
      : '工单列表和详情页会自动收口到你提交或当前与你相关的工单。',
    knowledgeScopeSummary: options.canManageKnowledge
      ? '你可以继续从已解决工单生成知识草稿，并进入编辑发布链路。'
      : '你可以查看知识文章和工单沉淀结果，但发布入口会保持隐藏。',
  }
}

export function buildTicketListScopeMessage(options: { canViewAllTickets: boolean }) {
  return options.canViewAllTickets
    ? '当前账号可查看工单工作台全量数据，适合继续验证指派、流转和知识沉淀闭环。'
    : '当前账号处于提单视角，列表已自动收口为你提交或当前由你跟进的工单。'
}

export function buildTicketCommentAccessCopy(options: { canUseInternalComments: boolean }) {
  if (options.canUseInternalComments) {
    return {
      banner: null,
      formHint: null,
    }
  }

  return {
    banner: '当前账号只展示对外可见评论，你仍然可以继续补充问题现象、验证结果和跟进反馈。',
    formHint: '当前将以普通协作评论提交，内部备注与处理流转仅对处理侧账号开放。',
  }
}

export function buildTicketPermissionSummary() {
  return {
    title: '当前权限',
    description: '当前账号更偏向提单和跟进视角，可继续补充评论、查看处理进展和回看知识沉淀结果。',
    items: [
      '可以查看当前工单的处理记录、公开评论和关联知识',
      '可以继续补充问题现象、验证反馈和业务影响信息',
      '处理人指派、内部备注和状态流转由处理侧账号负责',
    ],
  }
}

export function buildTicketSuggestedActions(options: { canUseInternalComments: boolean }) {
  return [
    '先确认最新处理人和当前状态是否一致',
    options.canUseInternalComments
      ? '优先检查最新评论和内部备注里的排查线索'
      : '优先检查最新评论里的排查线索，并补充你的验证结果',
    '必要时补充时间线记录，保证流转过程可追踪',
  ]
}

export function buildTopbarKnowledgeActionCopy(options: { canManageKnowledge: boolean }) {
  if (options.canManageKnowledge) {
    return {
      visible: true,
      label: '发布文章',
      hint: '你当前可以直接进入知识沉淀与发布链路。',
    }
  }

  return {
    visible: false,
    label: '查看知识库',
    hint: '当前账号以浏览和协作为主，知识发布入口会保持隐藏。',
  }
}

export function buildDashboardHeroCopy(options: {
  canViewAllTickets: boolean
  canManageKnowledge: boolean
  canAccessAiCenter: boolean
}) {
  const headline = options.canViewAllTickets
    ? '把工单工作台、知识沉淀和 AI 协作放进同一个处理视角。'
    : '把问题跟进、知识回查和后续协作放进同一个个人工作台。'

  const description = options.canViewAllTickets
    ? '当前账号偏向处理侧，适合继续验证指派、状态流转、知识沉淀和跨页面协作闭环。'
    : '当前账号偏向提单与跟进侧，适合回看处理进展、补充业务反馈，并追踪知识沉淀结果。'

  const primaryAction = options.canViewAllTickets
    ? { label: '进入工单中心', to: '/tickets' }
    : { label: '查看我的工单', to: '/tickets' }

  const secondaryAction = options.canManageKnowledge
    ? { label: '新建知识文章', to: '/knowledge/articles/create' }
    : { label: '查看知识库', to: '/knowledge/articles' }

  const note = options.canAccessAiCenter
    ? 'AI 工作台已开放，可以继续验证摘要、推荐和回复建议。'
    : 'AI 工作台当前未开放，仍可先完成工单与知识主线验证。'

  return {
    headline,
    description,
    primaryAction,
    secondaryAction,
    note,
  }
}

export function buildDashboardTicketPanelCopy(options: { canViewAllTickets: boolean }) {
  return {
    title: options.canViewAllTickets ? '待处理工单' : '我的相关工单',
    description: options.canViewAllTickets
      ? '先把最有业务味道的处理队列做出秩序感。'
      : '先把与你相关的问题、反馈和处理进展集中到同一处查看。',
    actionText: options.canViewAllTickets ? '查看全部' : '查看我的工单',
  }
}

export function buildDashboardKnowledgePanelCopy(options: { canManageKnowledge: boolean }) {
  return {
    title: options.canManageKnowledge ? '热门知识文章' : '推荐知识文章',
    description: options.canManageKnowledge
      ? '内容页会更强调沉淀效率、筛选体验和发布链路。'
      : '内容页会更强调阅读体验、回查效率和知识复用价值。',
    actionText: options.canManageKnowledge ? '进入知识库' : '浏览知识库',
  }
}

export function buildKnowledgeHeroCopy(options: { canManageKnowledge: boolean }) {
  if (options.canManageKnowledge) {
    return {
      title: '把排查经验沉淀成可搜索、可复用的知识资产',
      description: '这一页会继续承接从工单生成草稿、编辑补完到发布归档的完整知识链路。',
      ctaLabel: '新建文章',
      emptyHint: '当前筛选条件下还没有知识文章，可以继续新建一篇沉淀当前经验。',
    }
  }

  return {
    title: '把排查经验整理成可搜索、可回查的知识资产',
    description: '这一页更偏向知识回查和结果浏览，方便你从工单现场快速找到已有处理经验。',
    ctaLabel: '查看文章',
    emptyHint: '当前筛选条件下还没有知识文章，可以换个条件继续查找。',
  }
}

export function buildDashboardMetricsCopy(options: {
  ticketCount: number
  resolvedCount: number
  unassignedCount: number
  knowledgeCount: number
  canViewAllTickets: boolean
  canManageKnowledge: boolean
}) {
  const ticketMetric = options.canViewAllTickets
    ? {
        tag: '总工单',
        value: String(options.ticketCount),
        title: '当前工作台里的工单数量',
        desc: '包含真实接口结果和本地可续跑工单',
        chipClass: 'chip-blue',
      }
    : {
        tag: '我的工单',
        value: String(options.ticketCount),
        title: '当前与你相关的工单数量',
        desc: '方便你快速跟进提交中的问题与处理进展',
        chipClass: 'chip-blue',
      }

  const resolvedMetric = options.canViewAllTickets
    ? {
        tag: '已解决',
        value: String(options.resolvedCount),
        title: '当前已推进到解决态的工单',
        desc: '便于你快速验证状态流转是否生效',
        chipClass: 'chip-green',
      }
    : {
        tag: '已解决',
        value: String(options.resolvedCount),
        title: '与你相关并已进入解决态的问题',
        desc: '可以继续回看处理结果和后续知识沉淀',
        chipClass: 'chip-green',
      }

  const queueMetric = options.canViewAllTickets
    ? {
        tag: '待分配',
        value: String(options.unassignedCount),
        title: '还没有明确处理人的工单',
        desc: '适合重点测试指派和协作流转',
        chipClass: 'chip-orange',
      }
    : {
        tag: '待跟进',
        value: String(Math.max(options.ticketCount - options.resolvedCount, 0)),
        title: '当前还需要你继续关注的工单',
        desc: '优先补充信息、查看最新评论和确认处理状态',
        chipClass: 'chip-orange',
      }

  const knowledgeMetric = options.canManageKnowledge
    ? {
        tag: '知识文章',
        value: String(options.knowledgeCount),
        title: '当前可浏览的知识内容总数',
        desc: '已合并正式文章与本地草稿',
        chipClass: 'chip-blue',
      }
    : {
        tag: '知识回查',
        value: String(options.knowledgeCount),
        title: '当前可回查的知识内容总数',
        desc: '方便你从工单现场快速找到已有处理经验',
        chipClass: 'chip-blue',
      }

  return [ticketMetric, resolvedMetric, queueMetric, knowledgeMetric]
}

export function buildNotificationCenterCopy(options: {
  canManageKnowledge: boolean
  canAccessAiCenter: boolean
  canManageSystem: boolean
}) {
  const items = [
    { message: '工单 INC-2031 状态已更新为处理中', time: '10 分钟前', tone: 'chip-orange' },
  ]

  if (options.canAccessAiCenter) {
    items.push({ message: 'AI 为《支付回调失败排查手册》生成了新摘要', time: '25 分钟前', tone: 'chip-blue' })
  } else {
    items.push({ message: '关联知识《支付回调失败排查手册》已有新的排查补充', time: '25 分钟前', tone: 'chip-blue' })
  }

  if (options.canManageKnowledge) {
    items.push({ message: '知识库新增一篇待你审核的文章草稿', time: '今天 09:12', tone: 'chip-green' })
  } else {
    items.push({ message: '你关注的知识文章新增了处理经验补充', time: '今天 09:12', tone: 'chip-green' })
  }

  if (options.canManageSystem) {
    items.push({ message: '系统角色配置已更新，建议回看最新权限边界', time: '今天 08:40', tone: 'chip-default' })
  }

  return {
    title: '通知中心',
    description: options.canManageSystem
      ? '把需要处理、审核和回看的事项集中在一个地方。'
      : '把需要你跟进、关注和回看的事项集中在一个地方。',
    unreadCount: items.length,
    items,
  }
}

export function buildSystemManageCopy(options: { canManageSystem: boolean }) {
  if (options.canManageSystem) {
    return {
      chipText: 'Admin',
      title: '系统管理',
      description: '先把管理台入口接起来，后面可以继续细分角色、分类、通知和系统参数。',
      cards: [
        { label: '角色数量', value: '3', description: '管理员、支持、普通用户' },
        { label: '知识分类', value: '8', description: '后续可接真实分类管理' },
        { label: '系统通知模板', value: '5', description: '用于站内信与告警' },
      ],
      hint: '当前账号可以继续向角色管理、通知模板和系统参数配置扩展。',
    }
  }

  return {
    chipText: 'Read Only',
    title: '系统概览',
    description: '当前账号暂未开放系统管理动作，这里先展示系统结构与治理范围。',
    cards: [
      { label: '角色体系', value: '3', description: '已划分管理员、支持、普通用户' },
      { label: '知识分类', value: '8', description: '现有知识目录结构可继续扩展' },
      { label: '通知模板', value: '5', description: '系统已具备站内信与告警模板基础' },
    ],
    hint: '如需修改角色、分类或模板，需要切换到具备系统管理能力的账号。',
  }
}

export function buildSidebarBadgeMap(options: {
  canManageKnowledge: boolean
  canAccessAiCenter: boolean
  canManageSystem: boolean
}) {
  return {
    '/knowledge/articles': options.canManageKnowledge ? 'Edit' : 'Read',
    '/ai-center': options.canAccessAiCenter ? 'AI' : 'Locked',
    '/notifications': options.canManageKnowledge ? 'Review' : 'Follow',
    '/settings': options.canManageSystem ? 'Admin' : 'View',
  }
}
