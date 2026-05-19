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
  getRuntimeModeText: () => (state.runtimeDemoMode ? 'Demo Mode' : 'Live Mode'),
  getRuntimeModeHeadline: () => (state.runtimeDemoMode ? 'Demo session active' : 'Live backend session active'),
  getRuntimeDataSourceMessage: ({ subject }: { subject: string }) => (
    state.runtimeDemoMode
      ? `Demo data is active for ${subject}.`
      : `Live API data is active for ${subject}.`
  ),
}))

vi.mock('../../src/access-policy', () => ({
  buildNotificationCenterCopy: ({ canManageKnowledge, canAccessAiCenter, canManageSystem }: { canManageKnowledge: boolean; canAccessAiCenter: boolean; canManageSystem: boolean }) => ({
    title: canManageSystem ? 'System and collaboration notifications' : 'Collaboration notifications',
    description: canManageKnowledge ? 'Knowledge and ticket reminders are grouped here.' : 'Reminders are scoped to the current role.',
    unreadCount: canAccessAiCenter ? 4 : 2,
    items: [
      { message: canManageSystem ? 'Role permission changes pending review' : 'Knowledge article pending review', time: 'Just now' },
      { message: 'Ticket activity updated', time: '5 min ago' },
    ],
  }),
  buildSystemManageCopy: ({ canManageSystem }: { canManageSystem: boolean }) => ({
    title: canManageSystem ? 'System governance overview' : 'System information',
    description: canManageSystem ? 'Review roles, permissions, and governance entry points.' : 'Current account is view-only here.',
    chipText: canManageSystem ? 'Admin Control' : 'Read Only',
    hint: canManageSystem ? 'Continue with roles, categories, templates, and governance settings.' : 'This account cannot manage system settings.',
    cards: [
      { label: 'Role templates', value: canManageSystem ? '8' : '0', description: 'Used to constrain navigation and capability scope.' },
      { label: 'Notification templates', value: '12', description: 'Covers ticket and knowledge collaboration reminders.' },
    ],
  }),
  buildProfileCapabilitySummaries: () => ({
    roleSummary: 'This account is focused on knowledge collaboration.',
    ticketScopeSummary: 'This account can review and move ticket workflows forward.',
    knowledgeScopeSummary: 'This account can create and edit knowledge content.',
  }),
  CAPABILITY_PRESENTATION: [
    { code: 'TICKET_OPERATE', label: 'Ticket operations', description: 'Handle comments, statuses, and collaboration actions.' },
    { code: 'KNOWLEDGE_MANAGE', label: 'Knowledge management', description: 'Maintain knowledge articles and their workflow.' },
    { code: 'AI_CENTER_ACCESS', label: 'AI Center', description: 'Access AI summaries and recommendations.' },
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
      nickname: 'Support',
      realName: 'Support',
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

  function appShellStub() {
    return {
      AppShell: {
        template: '<div class="app-shell-stub"><slot /></div>',
      },
    }
  }

  it('renders the AI center hero and runtime banner', () => {
    const wrapper = mount(AiCenterView, {
      global: {
        stubs: appShellStub(),
      },
    })

    expect(wrapper.text()).toContain('AI Center')
    expect(wrapper.text()).toContain('42')
    expect(wrapper.text()).toContain('Demo session active')
    expect(wrapper.text()).toContain('Demo Mode')
    expect(wrapper.text()).toContain('Demo data is active for')
  })

  it('renders notification and system-management copies from access policy', () => {
    const notificationWrapper = mount(NotificationCenterView, {
      global: {
        stubs: appShellStub(),
      },
    })
    const systemWrapper = mount(SystemManageView, {
      global: {
        stubs: appShellStub(),
      },
    })

    expect(notificationWrapper.text()).toContain('System and collaboration notifications')
    expect(notificationWrapper.text()).toContain('4 Unread')
    expect(notificationWrapper.text()).toContain('Demo session active')
    expect(systemWrapper.text()).toContain('System governance overview')
    expect(systemWrapper.text()).toContain('Admin Control')
    expect(systemWrapper.text()).toContain('Role templates')
  })

  it('renders profile identity, runtime banner, and locked quick actions for unavailable capabilities', () => {
    state.canAccessAiCenter = false
    state.canManageSystem = false

    const wrapper = mount(ProfileView, {
      global: {
        stubs: appShellStub(),
      },
    })

    expect(wrapper.text()).toContain('Support')
    expect(wrapper.text()).toContain('@support01')
    expect(wrapper.text()).toContain('Demo Mode')
    expect(wrapper.text()).toContain('Demo data is active for 个人中心.')
    expect(wrapper.text()).toContain('This account is focused on knowledge collaboration.')
    expect(wrapper.text()).toContain('AI Center')
    expect(wrapper.text()).toContain('Locked')
    expect(wrapper.findAll('.quick-action-card.disabled')).toHaveLength(2)

    const quickActionLinks = wrapper.findAll('.quick-action-card')
    expect(quickActionLinks[2].attributes('data-to')).toBe('/profile')
    expect(quickActionLinks[3].attributes('data-to')).toBe('/profile')
  })
})
