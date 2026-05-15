import ElementPlus from 'element-plus'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick, reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../src/auth'
import type { TicketApiItem } from '../../src/api/ticket'
import { createLocalTicket } from '../../src/mock/ticketWorkspace'
import TicketListView from '../../src/views/TicketListView.vue'
import { createAuthUser } from './helpers/fixtures'
import { resetWebStorage } from './helpers/testHarness'

const { push, replace, fetchTickets, canViewAllState } = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  fetchTickets: vi.fn(),
  canViewAllState: { value: true },
}))

const route = reactive({
  path: '/tickets',
  query: {},
  fullPath: '/tickets',
})

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : to.path"><slot /></a>',
  },
  useRoute: () => route,
  useRouter: () => ({
    push,
    replace,
  }),
}))

vi.mock('../../src/api/ticket', () => ({
  fetchTickets,
}))

vi.mock('../../src/authz', () => ({
  canViewAllTickets: () => canViewAllState.value,
}))

function createTicketListItemFixture(overrides: Partial<TicketApiItem> = {}): TicketApiItem {
  return {
    id: 201,
    ticketNo: 'TK-201',
    title: '支付接口异常告警',
    content: '生产环境支付接口出现批量失败，需要尽快排查原因。',
    type: 'INCIDENT',
    categoryId: 3,
    priority: 3,
    priorityLabel: 'P2 较高',
    status: 2,
    statusLabel: '处理中',
    submitUserId: 7,
    assigneeUserId: 101,
    submitterName: '张小北',
    assigneeName: '李清川',
    linkedKnowledgeArticleCount: 0,
    latestLinkedKnowledgeArticle: null,
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:00:00',
    ...overrides,
  }
}

async function mountView() {
  const wrapper = mount(TicketListView, {
    global: {
      plugins: [ElementPlus],
      stubs: {
        AppSidebar: true,
        AppTopbar: true,
      },
    },
  })
  await flushPromises()
  mountedWrapper = wrapper
  return wrapper
}

async function setQuickFilter(wrapper: Awaited<ReturnType<typeof mountView>>, value: string) {
  ;(wrapper.vm as any).activeQuickFilter = value
  await nextTick()
  await flushPromises()
}

async function setViewMode(wrapper: Awaited<ReturnType<typeof mountView>>, value: string) {
  ;(wrapper.vm as any).viewMode = value
  await nextTick()
  await flushPromises()
}

let mountedWrapper: Awaited<ReturnType<typeof mountView>> | null = null

describe('TicketListView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    push.mockReset()
    replace.mockReset()
    fetchTickets.mockReset()
    canViewAllState.value = true
    resetWebStorage()
    route.query = {}
    route.path = '/tickets'
    route.fullPath = '/tickets'
    authState.token = 'prod-token'
    authState.restored = true
    authState.user = createAuthUser()
  })

  afterEach(() => {
    mountedWrapper?.unmount()
    mountedWrapper = null
    vi.restoreAllMocks()
  })

  it('shows a fallback notice and keeps rendering demo tickets when remote loading fails with a network-like error', async () => {
    fetchTickets.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-ticket-list-503',
    }))

    const wrapper = await mountView()

    expect(fetchTickets).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('工单列表暂时不可用')
    expect(wrapper.text()).toContain('当前展示的是可用兜底数据')
    expect(wrapper.text()).not.toContain('trace-ticket-list-503')
    expect(wrapper.findAll('.ticket-board-card').length).toBeGreaterThan(0)
  })

  it('shows the backend business error without falling back to demo tickets', async () => {
    createLocalTicket({
      title: '仅本地存在的工单草稿',
      content: '这条工单只在本地工作流中存在。',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    fetchTickets.mockRejectedValue(Object.assign(new Error('你没有权限查看全部工单'), {
      status: 403,
      traceId: 'trace-ticket-list-403',
    }))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('你没有权限查看全部工单')
    expect(wrapper.text()).toContain('trace-ticket-list-403')
    expect(wrapper.text()).not.toContain('当前展示的是可用兜底数据')
    expect(wrapper.text()).not.toContain('支付接口异常告警')
  })

  it('filters the board down to knowledge-gap tickets with the quick filter', async () => {
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 301,
        ticketNo: 'TK-301',
        title: '故障已解决但还没有知识沉淀',
        status: 3,
        statusLabel: '已解决',
        linkedKnowledgeArticleCount: 0,
      }),
      createTicketListItemFixture({
        id: 302,
        ticketNo: 'TK-302',
        title: '处理中任务工单',
        status: 2,
        statusLabel: '处理中',
        linkedKnowledgeArticleCount: 0,
      }),
      createTicketListItemFixture({
        id: 303,
        ticketNo: 'TK-303',
        title: '已关联知识的关闭工单',
        status: 4,
        statusLabel: '已关闭',
        linkedKnowledgeArticleCount: 2,
      }),
    ])

    const wrapper = await mountView()
    await setQuickFilter(wrapper, 'knowledge-gap')

    expect(wrapper.text()).toContain('故障已解决但还没有知识沉淀')
    expect(wrapper.text()).not.toContain('处理中任务工单')
    expect(wrapper.text()).not.toContain('已关联知识的关闭工单')
  })

  it('shows only local workflow tickets when the local quick filter is selected', async () => {
    createLocalTicket({
      title: '本地创建的联调工单',
      content: '前端联调过程中临时记录的工单草稿。',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 401,
        ticketNo: 'TK-401',
        title: '远程返回的处理中工单',
        status: 2,
        statusLabel: '处理中',
      }),
    ])

    const wrapper = await mountView()
    await setQuickFilter(wrapper, 'local')

    expect(wrapper.text()).toContain('本地创建的联调工单')
    expect(wrapper.text()).toContain('本地草稿')
    expect(wrapper.text()).not.toContain('远程返回的处理中工单')
  })

  it('applies converted filter params and resets back to the default query', async () => {
    fetchTickets
      .mockResolvedValueOnce([createTicketListItemFixture()])
      .mockResolvedValue([createTicketListItemFixture({
        id: 402,
        ticketNo: 'TK-402',
        title: '按条件筛选后的工单',
        priority: 4,
        priorityLabel: 'P1 紧急',
        type: 'TASK',
      })])

    const wrapper = await mountView()
    const vm = wrapper.vm as any

    vm.filters.keyword = ' 支付异常 '
    vm.filters.status = '2'
    vm.filters.priority = '4'
    vm.filters.type = 'TASK'
    vm.handleSearch()
    await flushPromises()

    expect(replace).toHaveBeenCalledWith({
      query: {
        keyword: '支付异常',
        status: '2',
        priority: '4',
        type: 'TASK',
        quickFilter: undefined,
        viewMode: undefined,
      },
    })
    expect(fetchTickets).toHaveBeenLastCalledWith({
      keyword: '支付异常',
      status: 2,
      priority: 4,
      type: 'TASK',
    })

    vm.resetFilters()
    await flushPromises()

    expect(replace).toHaveBeenLastCalledWith({
      query: {
        keyword: undefined,
        status: undefined,
        priority: undefined,
        type: undefined,
        quickFilter: undefined,
        viewMode: undefined,
      },
    })
    expect(fetchTickets).toHaveBeenLastCalledWith({
      keyword: undefined,
      status: undefined,
      priority: undefined,
      type: undefined,
    })
  })

  it('switches to compact view and filters urgent tickets only', async () => {
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 501,
        ticketNo: 'TK-501',
        title: '高优先级支付事故',
        priority: 1,
        priorityLabel: 'P1 紧急',
      }),
      createTicketListItemFixture({
        id: 502,
        ticketNo: 'TK-502',
        title: '普通咨询工单',
        priority: 4,
        priorityLabel: 'P4 低优先级',
        type: 'QUESTION',
      }),
    ])

    const wrapper = await mountView()
    await setViewMode(wrapper, 'compact')
    await setQuickFilter(wrapper, 'urgent')

    expect(wrapper.find('.ticket-table').exists()).toBe(true)
    expect(wrapper.text()).toContain('高优先级支付事故')
    expect(wrapper.text()).not.toContain('普通咨询工单')
  })

  it('scopes the mine filter to the current user when all-ticket access is unavailable', async () => {
    canViewAllState.value = false
    authState.user = createAuthUser({
      id: 2,
      username: 'user01',
      nickname: '王一鸣',
      realName: '王一鸣',
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 601,
        ticketNo: 'TK-601',
        title: '当前用户提交的工单',
        submitUserId: 2,
        submitterName: '王一鸣',
        assigneeUserId: 101,
        assigneeName: '李清川',
      }),
      createTicketListItemFixture({
        id: 602,
        ticketNo: 'TK-602',
        title: '其他人完全无关的工单',
        submitUserId: 8,
        submitterName: '赵晴',
        assigneeUserId: 9,
        assigneeName: '陈凯',
      }),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('我提交的工单')
    expect(wrapper.text()).toContain('当前用户提交的工单')
    expect(wrapper.text()).not.toContain('其他人完全无关的工单')

    await setQuickFilter(wrapper, 'mine')

    expect(wrapper.text()).toContain('当前用户提交的工单')
    expect(wrapper.text()).not.toContain('其他人完全无关的工单')
  })

  it('uses assignee matching for the mine filter when all-ticket access is available', async () => {
    authState.user = createAuthUser({
      id: 101,
      username: 'support01',
      nickname: '李清川',
      realName: '李清川',
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 701,
        ticketNo: 'TK-701',
        title: '当前坐席负责的工单',
        assigneeUserId: 101,
        assigneeName: '李清川',
      }),
      createTicketListItemFixture({
        id: 702,
        ticketNo: 'TK-702',
        title: '其他坐席负责的工单',
        assigneeUserId: 102,
        assigneeName: '周辰',
      }),
    ])

    const wrapper = await mountView()
    await setQuickFilter(wrapper, 'mine')

    expect(wrapper.text()).toContain('当前坐席负责的工单')
    expect(wrapper.text()).not.toContain('其他坐席负责的工单')
  })

  it('shows an empty state when role scoping removes every fetched ticket', async () => {
    canViewAllState.value = false
    authState.user = createAuthUser({
      id: 3,
      username: 'user03',
      nickname: '沈可',
      realName: '沈可',
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 801,
        ticketNo: 'TK-801',
        title: '与当前用户无关的工单',
        submitUserId: 7,
        submitterName: '张小北',
        assigneeUserId: 8,
        assigneeName: '陈凯',
      }),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('暂无符合条件的工单')
    expect(wrapper.findAll('.ticket-board-card')).toHaveLength(0)
  })

  it('syncs filter and view state from the route on first load without duplicate requests', async () => {
    route.query = {
      keyword: '支付',
      status: '2',
      priority: '2',
      type: 'TASK',
      quickFilter: 'urgent',
      viewMode: 'compact',
    }
    route.fullPath = '/tickets?keyword=%E6%94%AF%E4%BB%98&status=2&priority=2&type=TASK&quickFilter=urgent&viewMode=compact'
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 901,
        ticketNo: 'TK-901',
        title: '来自路由参数的工单',
        priority: 2,
        priorityLabel: 'P2 较高',
        type: 'TASK',
      }),
    ])

    const wrapper = await mountView()

    expect(fetchTickets).toHaveBeenCalledTimes(1)
    expect(fetchTickets).toHaveBeenCalledWith({
      keyword: '支付',
      status: 2,
      priority: 2,
      type: 'TASK',
    })
    expect(wrapper.find('.ticket-table').exists()).toBe(true)
    expect(wrapper.text()).toContain('来自路由参数的工单')
  })

  it('suppresses redundant navigation when already on the ticket-create route', async () => {
    fetchTickets.mockResolvedValue([createTicketListItemFixture()])
    route.path = '/tickets/create'
    route.fullPath = '/tickets/create'

    const wrapper = await mountView()
    ;(wrapper.vm as any).navigateTo('/tickets/create')

    expect(push).not.toHaveBeenCalled()
  })

  it('keeps the latest ticket list result when an older request resolves later', async () => {
    let resolveFirstLoad: ((value: TicketApiItem[]) => void) | null = null
    fetchTickets
      .mockImplementationOnce(() => new Promise((resolve) => {
        resolveFirstLoad = resolve as (value: TicketApiItem[]) => void
      }))
      .mockResolvedValueOnce([
        createTicketListItemFixture({
          id: 902,
          ticketNo: 'TK-902',
          title: '第二次请求返回的最新工单',
          priority: 2,
        }),
      ])

    const wrapper = await mountView()

    route.query = { quickFilter: 'urgent' }
    route.fullPath = '/tickets?quickFilter=urgent'
    await flushPromises()

    resolveFirstLoad?.([
      createTicketListItemFixture({
        id: 903,
        ticketNo: 'TK-903',
        title: '第一次请求迟到返回的旧工单',
      }),
    ])
    await flushPromises()

    expect(fetchTickets).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('第二次请求返回的最新工单')
    expect(wrapper.text()).not.toContain('第一次请求迟到返回的旧工单')
  })
})
