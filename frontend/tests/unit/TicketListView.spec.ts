import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../src/auth'
import { createLocalTicket } from '../../src/mock/ticketWorkspace'
import type { TicketApiItem } from '../../src/api/ticket'
import { createAuthUser } from './helpers/fixtures'
import { resetWebStorage } from './helpers/testHarness'
import TicketListView from '../../src/views/TicketListView.vue'

const { push, fetchTickets, canViewAllState } = vi.hoisted(() => ({
  push: vi.fn(),
  fetchTickets: vi.fn(),
  canViewAllState: { value: true },
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : to.path"><slot /></a>',
  },
  useRouter: () => ({
    push,
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
    title: '支付回调超时',
    content: '用户反馈支付成功后页面没有及时刷新。',
    type: 'INCIDENT',
    categoryId: 3,
    priority: 3,
    priorityLabel: '高优先级',
    status: 2,
    statusLabel: '处理中',
    submitUserId: 7,
    assigneeUserId: 101,
    submitterName: '提交人A',
    assigneeName: '李晓安',
    linkedKnowledgeArticleCount: 0,
    latestLinkedKnowledgeArticle: null,
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:00:00',
    ...overrides,
  }
}

describe('TicketListView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    push.mockReset()
    fetchTickets.mockReset()
    canViewAllState.value = true
    resetWebStorage()
    authState.token = 'prod-token'
    authState.restored = true
    authState.user = createAuthUser()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function mountView() {
    const wrapper = mount(TicketListView)
    await flushPromises()
    return wrapper
  }

  it('shows a fallback notice and keeps rendering demo tickets when remote loading fails with a network-like error', async () => {
    fetchTickets.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-ticket-list-503',
    }))

    const wrapper = await mountView()

    expect(fetchTickets).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('筛选接口暂时不可用，已回退到本地演示数据')
    expect(wrapper.text()).not.toContain('trace-ticket-list-503')
    expect(wrapper.findAll('.ticket-board-item').length).toBeGreaterThan(0)
  })

  it('shows the backend business error without falling back to demo tickets', async () => {
    createLocalTicket({
      title: '仅本地保留的工单',
      content: '业务错误时不应继续显示演示工单。',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    fetchTickets.mockRejectedValue(Object.assign(new Error('没有查看工单的权限'), {
      status: 403,
      traceId: 'trace-ticket-list-403',
    }))

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('没有查看工单的权限')
    expect(wrapper.text()).toContain('trace-ticket-list-403')
    expect(wrapper.text()).not.toContain('已回退到本地演示数据')
    expect(wrapper.text()).not.toContain('支付回调接口偶发超时')
  })

  it('filters the board down to knowledge-gap tickets with the quick filter', async () => {
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 301,
        ticketNo: 'TK-301',
        title: '已解决但未沉淀的支付回调问题',
        status: 3,
        statusLabel: '已解决',
        linkedKnowledgeArticleCount: 0,
      }),
      createTicketListItemFixture({
        id: 302,
        ticketNo: 'TK-302',
        title: '处理中的账单同步任务',
        status: 2,
        statusLabel: '处理中',
        linkedKnowledgeArticleCount: 0,
      }),
      createTicketListItemFixture({
        id: 303,
        ticketNo: 'TK-303',
        title: '已沉淀知识的退款工单',
        status: 4,
        statusLabel: '已关闭',
        linkedKnowledgeArticleCount: 2,
      }),
    ])

    const wrapper = await mountView()

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '待沉淀知识')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('已解决但未沉淀的支付回调问题')
    expect(wrapper.text()).not.toContain('处理中的账单同步任务')
    expect(wrapper.text()).not.toContain('已沉淀知识的退款工单')
  })

  it('shows only local workflow tickets when the local quick filter is selected', async () => {
    createLocalTicket({
      title: '本地创建的对账工单',
      content: '后端接口不可用时先在本地工作流排查。',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 401,
        ticketNo: 'TK-401',
        title: '远程加载的支付补单任务',
        status: 2,
        statusLabel: '处理中',
      }),
    ])

    const wrapper = await mountView()

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '本地工单')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('本地创建的对账工单')
    expect(wrapper.text()).toContain('本地工作流')
    expect(wrapper.text()).not.toContain('远程加载的支付补单任务')
  })

  it('toggles filters, applies converted filter params, and resets back to the default query', async () => {
    fetchTickets
      .mockResolvedValueOnce([createTicketListItemFixture()])
      .mockResolvedValue([createTicketListItemFixture({
        id: 402,
        ticketNo: 'TK-402',
        title: '筛选后的高优工单',
        priority: 4,
        priorityLabel: '紧急',
        type: 'TASK',
      })])

    const wrapper = await mountView()
    const topButtons = wrapper.findAll('button')

    await topButtons.find((item) => item.text() === '收起筛选')!.trigger('click')
    expect(wrapper.text()).toContain('筛选工单')

    await topButtons.find((item) => item.text() === '筛选工单')!.trigger('click')
    await wrapper.find('input.field-control').setValue(' 支付回调 ')
    const selects = wrapper.findAll('select.field-control')
    await selects[0].setValue('2')
    await selects[1].setValue('4')
    await selects[2].setValue('TASK')
    await wrapper.find('form.ticket-filter-bar').trigger('submit.prevent')
    await flushPromises()

    expect(fetchTickets).toHaveBeenLastCalledWith({
      keyword: '支付回调',
      status: 2,
      priority: 4,
      type: 'TASK',
    })

    await wrapper.findAll('button').find((item) => item.text() === '重置')!.trigger('click')
    await flushPromises()

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
        priority: 2,
        priorityLabel: '普通',
      }),
      createTicketListItemFixture({
        id: 502,
        ticketNo: 'TK-502',
        title: '普通咨询工单',
        priority: 4,
        priorityLabel: '紧急',
        type: 'QUESTION',
      }),
    ])

    const wrapper = await mountView()

    await wrapper.findAll('button').find((item) => item.text() === '紧凑视图')!.trigger('click')
    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '高优先级')!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.ticket-compact-list').exists()).toBe(true)
    expect(wrapper.text()).toContain('高优先级支付事故')
    expect(wrapper.text()).not.toContain('普通咨询工单')
  })

  it('scopes the mine filter to the current user when all-ticket access is unavailable', async () => {
    canViewAllState.value = false
    authState.user = createAuthUser({
      id: 2,
      username: 'user01',
      nickname: '业务小王',
      realName: '王晨',
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 601,
        ticketNo: 'TK-601',
        title: '我提交的工单',
        submitUserId: 2,
        submitterName: '业务小王',
        assigneeUserId: 101,
        assigneeName: '李晓安',
      }),
      createTicketListItemFixture({
        id: 602,
        ticketNo: 'TK-602',
        title: '与我无关的工单',
        submitUserId: 8,
        submitterName: '其他人',
        assigneeUserId: 9,
        assigneeName: '其他处理人',
      }),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('我的工单')
    expect(wrapper.text()).toContain('我提交的工单')
    expect(wrapper.text()).not.toContain('与我无关的工单')

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '我的工单')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('我提交的工单')
    expect(wrapper.text()).not.toContain('与我无关的工单')
  })

  it('uses assignee matching for the mine filter when all-ticket access is available', async () => {
    authState.user = createAuthUser({
      id: 101,
      username: 'support01',
      nickname: '李晓安',
      realName: '李晓安',
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 701,
        ticketNo: 'TK-701',
        title: '指派给我的工单',
        assigneeUserId: 101,
        assigneeName: '李晓安',
      }),
      createTicketListItemFixture({
        id: 702,
        ticketNo: 'TK-702',
        title: '其他人负责的工单',
        assigneeUserId: 102,
        assigneeName: '林哲',
      }),
    ])

    const wrapper = await mountView()

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '待我处理')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('指派给我的工单')
    expect(wrapper.text()).not.toContain('其他人负责的工单')
  })

  it('shows an empty state when role scoping removes every fetched ticket', async () => {
    canViewAllState.value = false
    authState.user = createAuthUser({
      id: 3,
      username: 'user03',
      nickname: '业务小陈',
      realName: '陈一鸣',
    })
    fetchTickets.mockResolvedValue([
      createTicketListItemFixture({
        id: 801,
        ticketNo: 'TK-801',
        title: '不属于当前用户的工单',
        submitUserId: 7,
        submitterName: '提交人A',
        assigneeUserId: 8,
        assigneeName: '其他处理人',
      }),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('当前筛选条件下没有匹配的工单。')
    expect(wrapper.findAll('.ticket-board-item')).toHaveLength(0)
  })
})
