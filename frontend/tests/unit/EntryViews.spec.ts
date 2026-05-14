import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../src/auth'
import AiCenterView from '../../src/views/AiCenterView.vue'
import NotificationCenterView from '../../src/views/NotificationCenterView.vue'
import ProfileView from '../../src/views/ProfileView.vue'
import SystemManageView from '../../src/views/SystemManageView.vue'

const state = vi.hoisted(() => ({
  canAccessAiCenter: true,
  canManageKnowledge: true,
  canManageSystem: true,
  canOperateTickets: true,
  canViewAllTickets: true,
  runtimeDemoMode: true,
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :data-to="typeof to === \'string\' ? to : JSON.stringify(to)" :href="typeof to === \'string\' ? to : to.path"><slot /></a>',
  },
}))

vi.mock('../../src/authz', () => ({
  canAccessAiCenter: () => state.canAccessAiCenter,
  canManageKnowledgeArticles: () => state.canManageKnowledge,
  canManageSystem: () => state.canManageSystem,
  canOperateTickets: () => state.canOperateTickets,
  canViewAllTickets: () => state.canViewAllTickets,
  canAccessCapability: (code: string) => (authState.user?.capabilities || []).includes(code),
}))

vi.mock('../../src/utils/runtimeMode', () => ({
  isDemoMode: () => state.runtimeDemoMode,
}))

vi.mock('../../src/access-policy', () => ({
  buildNotificationCenterCopy: ({ canManageKnowledge, canAccessAiCenter, canManageSystem }: { canManageKnowledge: boolean; canAccessAiCenter: boolean; canManageSystem: boolean }) => ({
    title: canManageSystem ? '系统与协作通知' : '协作通知',
    description: canManageKnowledge ? '已按知识与工单协作范围整理提醒。' : '已按当前角色整理提醒。',
    unreadCount: canAccessAiCenter ? 4 : 2,
    items: [
      { message: canManageSystem ? '系统角色权限变更待确认' : '知识文章待复核', time: '刚刚' },
      { message: '工单处理记录已更新', time: '5 分钟前' },
    ],
  }),
  buildSystemManageCopy: ({ canManageSystem }: { canManageSystem: boolean }) => ({
    title: canManageSystem ? '系统治理总览' : '系统信息',
    description: canManageSystem ? '查看角色、权限与配置治理入口。' : '当前账号以查看为主。',
    chipText: canManageSystem ? 'Admin Control' : 'Read Only',
    hint: canManageSystem ? '继续补角色、分类、通知模板和治理配置。' : '当前账号没有系统治理能力。',
    cards: [
      { label: '角色模板', value: canManageSystem ? '8' : '0', description: '用于约束菜单和能力范围。' },
      { label: '通知模板', value: '12', description: '覆盖工单与知识协作提醒。' },
    ],
  }),
  buildProfileCapabilitySummaries: () => ({
    roleSummary: '当前账号以知识沉淀和协作为主。',
    ticketScopeSummary: '可以查看并推进工单处理链路。',
    knowledgeScopeSummary: '可以新建、编辑并沉淀知识内容。',
  }),
  CAPABILITY_PRESENTATION: [
    { code: 'TICKET_OPERATE', label: '工单处理', description: '处理评论、状态和协作动作。' },
    { code: 'KNOWLEDGE_MANAGE', label: '知识管理', description: '维护知识文章与沉淀流程。' },
    { code: 'AI_CENTER_ACCESS', label: 'AI 工作台', description: '访问 AI 摘要和推荐能力。' },
  ],
}))

describe('entry views smoke coverage', () => {
  beforeEach(() => {
    state.canAccessAiCenter = true
    state.canManageKnowledge = true
    state.canManageSystem = true
    state.canOperateTickets = true
    state.canViewAllTickets = true
    state.runtimeDemoMode = true
    authState.token = ''
    authState.restored = true
    authState.user = {
      id: 2,
      username: 'support01',
      nickname: '李晓安',
      realName: '李晓安',
      email: 'support01@docflow.ai',
      phone: '13800000001',
      avatar: null,
      roles: ['SUPPORT'],
      permissions: ['KNOWLEDGE_VIEW', 'TICKET_VIEW'],
      capabilities: ['TICKET_OPERATE', 'KNOWLEDGE_MANAGE'],
    }
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the AI center hero and recommendation cards', () => {
    const wrapper = mount(AiCenterView, {
      global: {
        stubs: {
          AppSidebar: true,
          AppTopbar: true,
        },
      },
    })

    expect(wrapper.text()).toContain('AI Center')
    expect(wrapper.text()).toContain('今日建议')
    expect(wrapper.text()).toContain('42')
    expect(wrapper.text()).toContain('知识推荐')
    expect(wrapper.text()).toContain('支付回调失败排查手册')
    expect(wrapper.text()).toContain('查看全部')
  })

  it('renders notification and system-management copies from access policy', () => {
    const notificationWrapper = mount(NotificationCenterView, {
      global: {
        stubs: {
          AppSidebar: true,
          AppTopbar: true,
        },
      },
    })
    const systemWrapper = mount(SystemManageView, {
      global: {
        stubs: {
          AppSidebar: true,
          AppTopbar: true,
        },
      },
    })

    expect(notificationWrapper.text()).toContain('系统与协作通知')
    expect(notificationWrapper.text()).toContain('4 Unread')
    expect(notificationWrapper.text()).toContain('系统角色权限变更待确认')
    expect(systemWrapper.text()).toContain('系统治理总览')
    expect(systemWrapper.text()).toContain('Admin Control')
    expect(systemWrapper.text()).toContain('角色模板')
    expect(systemWrapper.text()).toContain('继续补角色、分类、通知模板和治理配置。')
  })

  it('renders profile identity, demo mode, and locked quick actions for unavailable capabilities', () => {
    state.canAccessAiCenter = false
    state.canManageSystem = false

    const wrapper = mount(ProfileView, {
      global: {
        stubs: {
          AppSidebar: true,
          AppTopbar: true,
        },
      },
    })

    expect(wrapper.text()).toContain('李晓安')
    expect(wrapper.text()).toContain('@support01 · support01@docflow.ai')
    expect(wrapper.text()).toContain('演示模式')
    expect(wrapper.text()).toContain('知识沉淀')
    expect(wrapper.text()).toContain('当前账号以知识沉淀和协作为主。')
    expect(wrapper.text()).toContain('AI 工作台')
    expect(wrapper.text()).toContain('Locked')
    expect(wrapper.text()).toContain('系统管理')
    expect(wrapper.text()).toContain('当前账号以协作和业务处理为主，系统治理入口保持隐藏。')

    const quickActionLinks = wrapper.findAll('.quick-action-card')
    expect(quickActionLinks[2].attributes('data-to')).toBe('/profile')
    expect(quickActionLinks[3].attributes('data-to')).toBe('/profile')
  })
})
