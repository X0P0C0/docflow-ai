import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { saveKnowledgeDraft } from '../../src/mock/knowledgeDrafts'
import { createLocalTicket } from '../../src/mock/ticketWorkspace'
import type { KnowledgeArticleApiItem } from '../../src/types/dashboard'
import type { TicketApiItem } from '../../src/api/ticket'
import DashboardView from '../../src/views/DashboardView.vue'
import { resetWebStorage } from './helpers/testHarness'

const { fetchKnowledgeArticles, fetchTickets, getRuntimeModeText } = vi.hoisted(() => ({
  fetchKnowledgeArticles: vi.fn(),
  fetchTickets: vi.fn(),
  getRuntimeModeText: vi.fn(),
}))

const route = {
  query: {},
  fullPath: '/dashboard',
}

vi.mock('vue-router', () => ({
  useRoute: () => route,
}))

vi.mock('../../src/api/knowledge', () => ({
  fetchKnowledgeArticles,
}))

vi.mock('../../src/api/ticket', () => ({
  fetchTickets,
}))

vi.mock('../../src/utils/runtimeMode', () => ({
  getRuntimeModeText,
}))

vi.mock('../../src/authz', () => ({
  canAccessAiCenter: () => true,
  canManageKnowledgeArticles: () => true,
  canViewAllTickets: () => true,
}))

function createKnowledgeArticleFixture(overrides: Partial<KnowledgeArticleApiItem> = {}): KnowledgeArticleApiItem {
  return {
    id: 1201,
    title: '支付回调失败排查手册',
    summary: '用于指导支持人员快速定位支付回调失败问题。',
    content: '先检查签名配置，再检查回调地址连通性，最后核对日志链路。',
    categoryId: 3,
    sourceTicketId: null,
    authorUserId: 7,
    status: 1,
    viewCount: 800,
    likeCount: 30,
    collectCount: 12,
    publishTime: '2026-05-14T09:00:00',
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:00:00',
    ...overrides,
  }
}

function createTicketFixture(overrides: Partial<TicketApiItem> = {}): TicketApiItem {
  return {
    id: 601,
    ticketNo: 'TK-601',
    title: '支付回调接口偶发超时',
    content: '用户支付成功后订单仍显示待支付。',
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
    linkedKnowledgeArticleCount: 1,
    latestLinkedKnowledgeArticle: null,
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:00:00',
    ...overrides,
  }
}

describe('DashboardView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    fetchKnowledgeArticles.mockReset()
    fetchTickets.mockReset()
    getRuntimeModeText.mockReset()
    resetWebStorage()
    route.query = {}
    route.fullPath = '/dashboard'
    getRuntimeModeText.mockReturnValue('演示模式')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function mountView() {
    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          AppSidebar: true,
          AppTopbar: true,
          HeroPanel: {
            props: ['pendingCount', 'knowledgeCoverage', 'localCount', 'runtimeMode'],
            template: '<div class="hero-panel-props">pending={{ pendingCount }}|coverage={{ knowledgeCoverage }}|local={{ localCount }}|runtime={{ runtimeMode }}</div>',
          },
          MetricsGrid: {
            props: ['items'],
            template: '<div class="metrics-props">metrics={{ items.length }}</div>',
          },
          TicketPanel: {
            props: ['items', 'errorMessage', 'errorTraceId'],
            template: '<div class="ticket-panel-props">items={{ items.length }}|error={{ errorMessage }}|trace={{ errorTraceId }}</div>',
          },
          KnowledgePanel: {
            props: ['items', 'loading', 'errorMessage', 'errorTraceId'],
            template: '<div class="knowledge-panel-props">items={{ items.length }}|loading={{ loading }}|error={{ errorMessage }}|trace={{ errorTraceId }}</div>',
          },
          SidePanels: true,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  it('shows the forbidden notice and feeds merged local workspace stats into the hero panel', async () => {
    route.query = { reason: 'forbidden' }
    route.fullPath = '/dashboard?reason=forbidden'
    saveKnowledgeDraft({
      id: 9903,
      title: '本地知识草稿',
      summary: '本地沉淀中的知识。',
      content: '本地知识内容',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
    })
    createLocalTicket({
      title: '本地排查工单',
      content: '后端未就绪时的本地工单。',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture({ id: 1201 }),
      createKnowledgeArticleFixture({ id: 1202, title: '发布异常应急处理流程', categoryId: 2 }),
    ])
    fetchTickets.mockResolvedValue([
      createTicketFixture({ id: 601, status: 2, statusLabel: '处理中' }),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('当前账号没有访问刚才那个页面的权限，已为你返回可访问的工作台。')
    expect(wrapper.find('.hero-panel-props').text()).toContain('pending=2')
    expect(wrapper.find('.hero-panel-props').text()).toContain('coverage=78')
    expect(wrapper.find('.hero-panel-props').text()).toContain('local=2')
    expect(wrapper.find('.hero-panel-props').text()).toContain('runtime=演示模式')
    expect(wrapper.find('.ticket-panel-props').text()).toContain('items=2')
    expect(wrapper.find('.knowledge-panel-props').text()).toContain('items=3')
  })

  it('passes network fallback messages to both panels without exposing trace ids', async () => {
    saveKnowledgeDraft({
      id: 9905,
      title: '本地知识草稿',
      summary: '后端不可用时保留在本地的知识内容。',
      content: '本地知识内容',
      categoryId: 2,
      authorUserId: 9,
      status: 0,
      publishTime: null,
    })
    fetchKnowledgeArticles.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-dashboard-knowledge-503',
    }))
    fetchTickets.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-dashboard-ticket-503',
    }))

    const wrapper = await mountView()

    expect(wrapper.find('.knowledge-panel-props').text()).toContain('items=3')
    expect(wrapper.find('.knowledge-panel-props').text()).toContain('error=后端接口暂时不可用')
    expect(wrapper.find('.knowledge-panel-props').text()).toContain('trace=')
    expect(wrapper.find('.knowledge-panel-props').text()).not.toContain('trace-dashboard-knowledge-503')
    expect(wrapper.find('.ticket-panel-props').text()).toContain('error=工单接口暂时不可用')
    expect(wrapper.find('.ticket-panel-props').text()).not.toContain('trace-dashboard-ticket-503')
  })

  it('preserves backend business errors and trace ids when dashboard list calls fail with 4xx errors', async () => {
    saveKnowledgeDraft({
      id: 9906,
      title: '仅本地保留的知识草稿',
      summary: '业务错误时不应注入演示知识。',
      content: '本地知识内容',
      categoryId: 2,
      authorUserId: 9,
      status: 0,
      publishTime: null,
    })
    createLocalTicket({
      title: '仅本地保留的工单',
      content: '业务错误时不应注入演示工单。',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    fetchKnowledgeArticles.mockRejectedValue(Object.assign(new Error('没有查看知识文章的权限'), {
      status: 403,
      traceId: 'trace-dashboard-knowledge-403',
    }))
    fetchTickets.mockRejectedValue(Object.assign(new Error('没有查看工单的权限'), {
      status: 403,
      traceId: 'trace-dashboard-ticket-403',
    }))

    const wrapper = await mountView()

    expect(wrapper.find('.knowledge-panel-props').text()).toContain('items=1')
    expect(wrapper.find('.knowledge-panel-props').text()).toContain('error=没有查看知识文章的权限（追踪号：trace-dashboard-knowledge-403）')
    expect(wrapper.find('.knowledge-panel-props').text()).toContain('trace=trace-dashboard-knowledge-403')
    expect(wrapper.find('.ticket-panel-props').text()).toContain('items=1')
    expect(wrapper.find('.ticket-panel-props').text()).toContain('error=没有查看工单的权限（追踪号：trace-dashboard-ticket-403）')
    expect(wrapper.find('.ticket-panel-props').text()).toContain('trace=trace-dashboard-ticket-403')
  })
})
