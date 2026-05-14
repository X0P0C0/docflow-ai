import { describe, expect, it } from 'vitest'
import { CAPABILITY_CODES } from '../../src/capability-constants'
import {
  buildAiCenterAccessCopy,
  buildDashboardHeroCopy,
  buildDashboardKnowledgePanelCopy,
  buildDashboardMetricsCopy,
  buildDashboardTicketPanelCopy,
  buildKnowledgeHeroCopy,
  buildNotificationCenterCopy,
  buildProfileCapabilitySummaries,
  buildSidebarBadgeMap,
  buildSystemManageCopy,
  buildTicketCommentAccessCopy,
  buildTicketListScopeMessage,
  buildTicketPermissionSummary,
  buildTicketSuggestedActions,
  buildTopbarKnowledgeActionCopy,
  getRouteRequiredCapability,
} from '../../src/access-policy'

describe('access policy helpers', () => {
  it('maps protected routes to their required capabilities', () => {
    expect(getRouteRequiredCapability('/knowledge/articles/create')).toBe(CAPABILITY_CODES.KNOWLEDGE_MANAGE)
    expect(getRouteRequiredCapability('/ai-center')).toBe(CAPABILITY_CODES.AI_CENTER_ACCESS)
    expect(getRouteRequiredCapability('/settings')).toBe(CAPABILITY_CODES.SYSTEM_MANAGE)
    expect(getRouteRequiredCapability('/tickets')).toBeNull()
  })

  it('builds AI center copy differently for enabled and disabled accounts', () => {
    expect(buildAiCenterAccessCopy({
      canAccess: true,
      roleText: '支持',
    })).toEqual({
      chipText: 'AI Focus',
      title: '今天的协作效率提升 18%',
      description: 'AI 已为知识文章生成摘要，并为待处理工单提供回复建议和相关文章推荐。',
      actionText: '查看 AI 面板',
    })

    expect(buildAiCenterAccessCopy({
      canAccess: false,
      roleText: '普通用户',
    })).toEqual({
      chipText: '权限提示',
      title: '当前账号暂未开放 AI 工作台',
      description: '当前登录身份为 普通用户，你仍然可以先完成工单流转和知识沉淀主线。',
      actionText: '当前能力未开放',
    })
  })

  it('builds dashboard hero and metrics copy according to the user perspective', () => {
    const hero = buildDashboardHeroCopy({
      canViewAllTickets: false,
      canManageKnowledge: false,
      canAccessAiCenter: false,
    })
    const metrics = buildDashboardMetricsCopy({
      ticketCount: 6,
      resolvedCount: 2,
      unassignedCount: 1,
      knowledgeCount: 4,
      canViewAllTickets: false,
      canManageKnowledge: false,
    })

    expect(hero.primaryAction).toEqual({ label: '查看我的工单', to: '/tickets' })
    expect(hero.secondaryAction).toEqual({ label: '查看知识库', to: '/knowledge/articles' })
    expect(hero.note).toContain('AI 工作台当前未开放')
    expect(metrics[0].tag).toBe('我的工单')
    expect(metrics[2].value).toBe('4')
    expect(metrics[3].tag).toBe('知识回查')
  })

  it('builds the alternate dashboard and knowledge policy branches for operator-facing accounts', () => {
    const profile = buildProfileCapabilitySummaries({
      canOperateTickets: true,
      canViewAllTickets: true,
      canManageKnowledge: true,
    })
    const ticketScope = buildTicketListScopeMessage({
      canViewAllTickets: true,
    })
    const ticketComments = buildTicketCommentAccessCopy({
      canUseInternalComments: true,
    })
    const suggestedActions = buildTicketSuggestedActions({
      canUseInternalComments: true,
    })
    const topbarAction = buildTopbarKnowledgeActionCopy({
      canManageKnowledge: true,
    })
    const ticketPanel = buildDashboardTicketPanelCopy({
      canViewAllTickets: true,
    })
    const knowledgePanel = buildDashboardKnowledgePanelCopy({
      canManageKnowledge: true,
    })
    const knowledgeHero = buildKnowledgeHeroCopy({
      canManageKnowledge: true,
    })
    const metrics = buildDashboardMetricsCopy({
      ticketCount: 10,
      resolvedCount: 6,
      unassignedCount: 2,
      knowledgeCount: 8,
      canViewAllTickets: true,
      canManageKnowledge: true,
    })

    expect(profile.roleSummary).toContain('处理侧')
    expect(profile.ticketScopeSummary).toContain('工作台范围内的可处理工单')
    expect(profile.knowledgeScopeSummary).toContain('进入编辑发布链路')
    expect(ticketScope).toContain('工单工作台全量数据')
    expect(ticketComments).toEqual({ banner: null, formHint: null })
    expect(buildTicketPermissionSummary().items).toHaveLength(3)
    expect(suggestedActions[1]).toContain('内部备注')
    expect(topbarAction).toEqual({
      visible: true,
      label: '发布文章',
      hint: '你当前可以直接进入知识沉淀与发布链路。',
    })
    expect(ticketPanel).toEqual({
      title: '待处理工单',
      description: '先把最有业务味道的处理队列做出秩序感。',
      actionText: '查看全部',
    })
    expect(knowledgePanel).toEqual({
      title: '热门知识文章',
      description: '内容页会更强调沉淀效率、筛选体验和发布链路。',
      actionText: '进入知识库',
    })
    expect(knowledgeHero.ctaLabel).toBe('新建文章')
    expect(metrics[0].tag).toBe('总工单')
    expect(metrics[2].value).toBe('2')
    expect(metrics[3].tag).toBe('知识文章')
  })

  it('builds notification and system management copy with role-sensitive variants', () => {
    const notifications = buildNotificationCenterCopy({
      canManageKnowledge: false,
      canAccessAiCenter: false,
      canManageSystem: false,
    })
    const systemCopy = buildSystemManageCopy({
      canManageSystem: false,
    })

    expect(notifications.unreadCount).toBe(3)
    expect(notifications.items[1].message).toContain('关联知识')
    expect(systemCopy.chipText).toBe('Read Only')
    expect(systemCopy.hint).toContain('需要切换到具备系统管理能力的账号')
  })

  it('builds admin-facing notification and system-management variants', () => {
    const notifications = buildNotificationCenterCopy({
      canManageKnowledge: true,
      canAccessAiCenter: true,
      canManageSystem: true,
    })
    const systemCopy = buildSystemManageCopy({
      canManageSystem: true,
    })

    expect(notifications.unreadCount).toBe(4)
    expect(notifications.description).toBe('把需要处理、审核和回看的事项集中在一个地方。')
    expect(notifications.items[1].message).toContain('AI 为《支付回调失败排查手册》生成了新摘要')
    expect(notifications.items[2].message).toContain('待你审核的文章草稿')
    expect(notifications.items[3].message).toContain('系统角色配置已更新')
    expect(systemCopy.chipText).toBe('Admin')
    expect(systemCopy.title).toBe('系统管理')
    expect(systemCopy.cards[1].description).toContain('后续可接真实分类管理')
  })

  it('builds sidebar badges from capability state', () => {
    expect(buildSidebarBadgeMap({
      canManageKnowledge: false,
      canAccessAiCenter: false,
      canManageSystem: true,
    })).toEqual({
      '/knowledge/articles': 'Read',
      '/ai-center': 'Locked',
      '/notifications': 'Follow',
      '/settings': 'Admin',
    })
  })

  it('builds the alternate read-only sidebar badge and end-user comment guidance branches', () => {
    expect(buildSidebarBadgeMap({
      canManageKnowledge: true,
      canAccessAiCenter: true,
      canManageSystem: false,
    })).toEqual({
      '/knowledge/articles': 'Edit',
      '/ai-center': 'AI',
      '/notifications': 'Review',
      '/settings': 'View',
    })

    expect(buildTicketCommentAccessCopy({
      canUseInternalComments: false,
    }).banner).toContain('只展示对外可见评论')

    expect(buildTicketSuggestedActions({
      canUseInternalComments: false,
    })[1]).toContain('补充你的验证结果')

    expect(buildKnowledgeHeroCopy({
      canManageKnowledge: false,
    }).ctaLabel).toBe('查看文章')
  })
})
