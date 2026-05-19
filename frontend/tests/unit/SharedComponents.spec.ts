import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import HeroPanel from '../../src/components/dashboard/HeroPanel.vue'
import TicketPanel from '../../src/components/dashboard/TicketPanel.vue'
import AppSidebar from '../../src/components/layout/AppSidebar.vue'
import AppTopbar from '../../src/components/layout/AppTopbar.vue'

const state = vi.hoisted(() => ({
  route: { path: '/dashboard' },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
  },
  authState: {
    user: {
      id: 2,
      username: 'support01',
      nickname: 'Support',
      realName: 'Support',
      email: 'support01@docflow.ai',
      phone: '13800000001',
      avatar: null,
      roles: ['SUPPORT'],
      permissions: ['TICKET_VIEW'],
      capabilities: ['TICKET_OPERATE'],
    },
  },
  clearSession: vi.fn(),
  canAccessAiCenter: false,
  canManageKnowledge: false,
  canManageSystem: false,
  routeAccessMap: {
    '/dashboard': true,
    '/tickets': true,
    '/knowledge/articles': true,
    '/ai-center': false,
    '/settings': false,
  } as Record<string, boolean>,
  runtimeDemoMode: true,
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a class="router-link" :data-to="typeof to === \'string\' ? to : JSON.stringify(to)"><slot /></a>',
  },
  useRoute: () => state.route,
  useRouter: () => state.router,
}))

vi.mock('../../src/auth', () => ({
  authState: state.authState,
  clearSession: state.clearSession,
}))

vi.mock('../../src/authz', () => ({
  canAccessAiCenter: () => state.canAccessAiCenter,
  canAccessRoute: (target: string) => state.routeAccessMap[target] ?? true,
  canManageKnowledgeArticles: () => state.canManageKnowledge,
  canManageSystem: () => state.canManageSystem,
}))

vi.mock('../../src/access-policy', () => ({
  buildAiCenterAccessCopy: ({ canAccess, roleText }: { canAccess: boolean; roleText: string }) => ({
    chipText: canAccess ? 'AI Focus' : 'Access Notice',
    title: canAccess ? 'AI access is ready' : `Current account ${roleText} has no AI access yet`,
    description: canAccess ? 'You can enter the AI workspace directly.' : 'This account should continue through tickets and knowledge first.',
    actionText: canAccess ? 'Open AI workspace' : 'Access not enabled',
  }),
  buildSidebarBadgeMap: () => ({
    '/knowledge/articles': state.canManageKnowledge ? 'Edit' : 'Read',
    '/ai-center': state.canAccessAiCenter ? 'AI' : 'Locked',
    '/notifications': 'Follow',
    '/settings': state.canManageSystem ? 'Admin' : 'View',
  }),
  buildTopbarKnowledgeActionCopy: ({ canManageKnowledge }: { canManageKnowledge: boolean }) => ({
    visible: canManageKnowledge,
    label: canManageKnowledge ? 'Publish article' : 'Browse knowledge base',
    hint: canManageKnowledge ? 'Can publish' : 'Read only',
  }),
}))

vi.mock('../../src/utils/runtimeMode', () => ({
  isDemoMode: () => state.runtimeDemoMode,
  getRuntimeModeText: () => (state.runtimeDemoMode ? '演示模式' : '真实模式'),
  getRuntimeModeHeadline: () => (state.runtimeDemoMode ? '当前正在使用演示会话' : '当前正在使用真实后端会话'),
  getRuntimeEntryMessage: (subject = '当前入口') => (
    state.runtimeDemoMode
      ? `当前${subject}默认会进入演示模式，登录后会优先使用本地演示数据来预览页面和流程。`
      : `当前${subject}默认会进入真实模式，登录后会优先读取并写入真实后端数据。`
  ),
}))

describe('shared components', () => {
  beforeEach(() => {
    state.route.path = '/dashboard'
    state.router.push.mockReset()
    state.router.replace.mockReset()
    state.clearSession.mockReset()
    state.authState.user = {
      id: 2,
      username: 'support01',
      nickname: 'Support',
      realName: 'Support',
      email: 'support01@docflow.ai',
      phone: '13800000001',
      avatar: null,
      roles: ['SUPPORT'],
      permissions: ['TICKET_VIEW'],
      capabilities: ['TICKET_OPERATE'],
    }
    state.canAccessAiCenter = false
    state.canManageKnowledge = false
    state.canManageSystem = false
    state.routeAccessMap = {
      '/dashboard': true,
      '/tickets': true,
      '/knowledge/articles': true,
      '/ai-center': false,
      '/settings': false,
    }
    state.runtimeDemoMode = true
  })

  it('filters sidebar navigation by route access, renders mapped badges, and blocks locked AI actions', async () => {
    const wrapper = mount(AppSidebar, {
      props: {
        workspaceNav: [
          { name: 'Workspace', to: '/dashboard', badge: 'Hot' },
          { name: 'AI Center', to: '/ai-center', badge: 'Soon' },
        ],
        manageNav: [
          { name: 'Knowledge Base', to: '/knowledge/articles', badge: 'Base' },
          { name: 'System Settings', to: '/settings', badge: 'Base' },
        ],
      },
    })

    const links = wrapper.findAll('.router-link')
    expect(links).toHaveLength(2)
    expect(wrapper.text()).toContain('Workspace')
    expect(wrapper.text()).not.toContain('AI Center')
    expect(wrapper.text()).toContain('Read')
    expect(wrapper.find('.nav-item').classes()).toContain('active')
    expect(wrapper.text()).toContain('Access Notice')
    expect(wrapper.text()).toContain('Current account SUPPORT has no AI access yet')

    await wrapper.find('.primary-button').trigger('click')
    expect(state.router.push).not.toHaveBeenCalled()
  })

  it('shows enabled AI access in the sidebar and navigates when the AI action is available', async () => {
    state.canAccessAiCenter = true
    state.canManageSystem = true
    state.routeAccessMap['/ai-center'] = true
    state.routeAccessMap['/settings'] = true
    state.route.path = '/settings'

    const wrapper = mount(AppSidebar, {
      props: {
        workspaceNav: [
          { name: 'Workspace', to: '/dashboard', badge: 'Hot' },
          { name: 'AI Center', to: '/ai-center', badge: 'Soon' },
        ],
        manageNav: [
          { name: 'System Settings', to: '/settings', badge: 'Base' },
        ],
      },
    })

    expect(wrapper.text()).toContain('AI')
    expect(wrapper.text()).toContain('Admin')
    expect(wrapper.text()).toContain('AI access is ready')
    expect(wrapper.findAll('.nav-item')[2].classes()).toContain('active')

    await wrapper.find('.primary-button').trigger('click')
    expect(state.router.push).toHaveBeenCalledWith('/ai-center')
  })

  it('suppresses redundant shared-component navigation when the current route already matches the target', async () => {
    state.route.path = '/knowledge/articles'

    const topbar = mount(AppTopbar)
    await topbar.findAll('button').find((item) => item.text().includes('搜索知识文章'))!.trigger('click')

    const hero = mount(HeroPanel, {
      props: {
        pendingCount: 6,
        knowledgeCoverage: 87,
        localCount: 2,
        runtimeMode: '真实模式',
        heroHeadline: 'Keep tickets and knowledge in one operating lane.',
        heroDescription: 'This account is currently focused on triage work.',
        capabilityNote: 'AI workspace is already enabled.',
        primaryAction: { label: 'Open knowledge base', to: '/knowledge/articles' },
        secondaryAction: { label: 'Create article', to: '/knowledge/articles/create' },
      },
    })
    await hero.findAll('button')[0].trigger('click')

    state.route.path = '/ai-center'
    state.canAccessAiCenter = true
    state.routeAccessMap['/ai-center'] = true
    const sidebar = mount(AppSidebar, {
      props: {
        workspaceNav: [
          { name: 'Workspace', to: '/dashboard', badge: 'Hot' },
          { name: 'AI Center', to: '/ai-center', badge: 'Soon' },
        ],
        manageNav: [],
      },
    })
    await sidebar.find('.primary-button').trigger('click')

    expect(state.router.push).not.toHaveBeenCalled()
  })

  it('renders topbar mode, gated knowledge actions, and logs out through clearSession', async () => {
    state.canManageKnowledge = true

    const wrapper = mount(AppTopbar)

    expect(wrapper.text()).toContain('演示模式')
    expect(wrapper.text()).toContain('SUPPORT')
    expect(wrapper.text()).toContain('Publish article')
    expect(wrapper.text()).toContain('当前正在使用演示会话')
    expect(wrapper.text()).toContain('当前顶部导航默认会进入演示模式')

    const searchButton = wrapper.findAll('button').find((item) => item.text().includes('搜索知识文章'))
    const createTicketButton = wrapper.findAll('button').find((item) => item.text() === '新建工单')
    const publishButton = wrapper.findAll('button').find((item) => item.text() === 'Publish article')
    const profileButton = wrapper.findAll('button').find((item) => item.text().includes('个人中心'))
    const logoutButton = wrapper.findAll('button').find((item) => item.text() === '退出')

    await searchButton!.trigger('click')
    await createTicketButton!.trigger('click')
    await publishButton!.trigger('click')
    await profileButton!.trigger('click')
    await logoutButton!.trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/knowledge/articles')
    expect(state.router.push).toHaveBeenCalledWith('/tickets/create')
    expect(state.router.push).toHaveBeenCalledWith('/knowledge/articles/create')
    expect(state.router.push).toHaveBeenCalledWith('/profile')
    expect(state.clearSession).toHaveBeenCalledTimes(1)
    expect(state.router.replace).toHaveBeenCalledWith('/login')
  })

  it('renders hero actions and runtime copy from props and current user identity', async () => {
    const wrapper = mount(HeroPanel, {
      props: {
        pendingCount: 6,
        knowledgeCoverage: 87,
        localCount: 2,
        runtimeMode: '真实模式',
        heroHeadline: 'Keep tickets and knowledge in one operating lane.',
        heroDescription: 'This account is currently focused on triage work.',
        capabilityNote: 'AI workspace is already enabled.',
        primaryAction: { label: 'Open ticket center', to: '/tickets' },
        secondaryAction: { label: 'Create article', to: '/knowledge/articles/create' },
      },
    })

    expect(wrapper.text()).toContain('Keep tickets and knowledge in one operating lane.')
    expect(wrapper.text()).toContain('真实模式')
    expect(wrapper.text()).toContain('Support')
    expect(wrapper.text()).toContain('87%')
    expect(wrapper.text()).toContain('本地可继续记录')

    const buttons = wrapper.findAll('button')
    await buttons[0].trigger('click')
    await buttons[1].trigger('click')

    expect(state.router.push).toHaveBeenCalledWith('/tickets')
    expect(state.router.push).toHaveBeenCalledWith('/knowledge/articles/create')
  })

  it('renders ticket panel links, status chips, and local fallback error copy', () => {
    const wrapper = mount(TicketPanel, {
      props: {
        title: 'Pending tickets',
        description: 'Review the current handling queue first.',
        actionText: 'View all',
        errorMessage: '工单接口暂时不可用',
        errorTraceId: '',
        items: [
          {
            id: 1,
            ticketNo: 'INC-1',
            title: 'Payment callback issue',
            meta: 'INC-1 · Unassigned · Updated at 2026-05-14 10:00:00',
            submitUserId: 1,
            assigneeUserId: null,
            content: 'content',
            priorityLevel: 'P1',
            priority: 'High',
            status: '处理中',
            priorityClass: 'chip-red',
            assignee: 'Unassigned',
            submitter: 'Alice',
            updatedAt: '2026-05-14 10:00:00',
            tags: ['Incident', 'Category 3'],
          },
          {
            id: 2,
            ticketNo: 'INC-2',
            title: 'Historical issue',
            meta: 'INC-2 · Support · Updated at 2026-05-14 09:00:00',
            submitUserId: 2,
            assigneeUserId: 2,
            content: 'content',
            priorityLevel: 'P3',
            priority: 'Normal',
            status: '已关闭',
            priorityClass: 'chip-green',
            assignee: 'Support',
            submitter: 'Alice',
            updatedAt: '2026-05-14 09:00:00',
            tags: ['Question', 'Category 1'],
          },
        ],
      },
      global: {
        stubs: {
          ErrorTraceNotice: {
            props: ['message', 'traceId'],
            template: '<div class="error-trace">{{ message }}|{{ traceId }}</div>',
          },
        },
      },
    })

    expect(wrapper.text()).toContain('工单接口暂时不可用，当前先展示本地工作台数据。')
    expect(wrapper.find('.error-trace').text()).toContain('|')
    expect(wrapper.findAll('.router-link')[0].attributes('data-to')).toBe('/tickets')
    expect(wrapper.findAll('.router-link')[1].attributes('data-to')).toBe('/tickets/1')
    expect(wrapper.findAll('.router-link')[2].attributes('data-to')).toBe('/tickets/2')
    expect(wrapper.text()).toContain('处理中')
    expect(wrapper.text()).toContain('已关闭')
  })
})
