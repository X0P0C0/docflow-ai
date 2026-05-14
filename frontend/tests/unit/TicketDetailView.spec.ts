import { flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../src/auth'
import { saveKnowledgeDraft } from '../../src/mock/knowledgeDrafts'
import { createLocalTicket } from '../../src/mock/ticketWorkspace'
import { consumeKnowledgeDraftSeed } from '../../src/utils/knowledgeFromTicket'
import { createAuthUser, createDefaultAssigneeOptions, createTicketDetailFixture } from './helpers/fixtures'
import { mountTicketDetailView } from './helpers/pageMounts'
import { assignRouteState, resetWebStorage } from './helpers/testHarness'

const {
  push,
  replace,
  fetchTicketDetail,
  fetchTicketAssignees,
  addTicketComment,
  assignTicket,
  createTicketKnowledgeDraft,
  updateTicketStatus,
  authzState,
} = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  fetchTicketDetail: vi.fn(),
  fetchTicketAssignees: vi.fn(),
  addTicketComment: vi.fn(),
  assignTicket: vi.fn(),
  createTicketKnowledgeDraft: vi.fn(),
  updateTicketStatus: vi.fn(),
  authzState: {
    canAssignTickets: true,
    canManageKnowledgeArticles: true,
    canTransitionTickets: true,
    canUseInternalTicketComments: true,
    canViewAllTickets: true,
  },
}))

const route = reactive({
  path: '/tickets/101',
  params: { id: '101' },
  query: {},
  fullPath: '/tickets/101',
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
  fetchTicketDetail,
  fetchTicketAssignees,
  addTicketComment,
  assignTicket,
  createTicketKnowledgeDraft,
  updateTicketStatus,
}))

vi.mock('../../src/authz', () => ({
  canAssignTickets: () => authzState.canAssignTickets,
  canManageKnowledgeArticles: () => authzState.canManageKnowledgeArticles,
  canTransitionTickets: () => authzState.canTransitionTickets,
  canUseInternalTicketComments: () => authzState.canUseInternalTicketComments,
  canViewAllTickets: () => authzState.canViewAllTickets,
}))

describe('TicketDetailView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    push.mockReset()
    replace.mockReset()
    fetchTicketDetail.mockReset()
    fetchTicketAssignees.mockReset()
    addTicketComment.mockReset()
    assignTicket.mockReset()
    createTicketKnowledgeDraft.mockReset()
    updateTicketStatus.mockReset()
    authzState.canAssignTickets = true
    authzState.canManageKnowledgeArticles = true
    authzState.canTransitionTickets = true
    authzState.canUseInternalTicketComments = true
    authzState.canViewAllTickets = true
    resetWebStorage()
    assignRouteState(route, {
      path: '/tickets/101',
      params: { id: '101' },
      query: {},
      fullPath: '/tickets/101',
    })
    authState.token = 'prod-token'
    authState.restored = true
    authState.user = createAuthUser()
    window.HTMLElement.prototype.scrollIntoView = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows inline trace details when comment submission fails with a business error', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])
    addTicketComment.mockRejectedValue(Object.assign(new Error('评论内容涉嫌敏感信息，无法提交'), {
      status: 403,
      traceId: 'trace-comment-403',
    }))

    const wrapper = await mountTicketDetailView()

    await wrapper.find('form.ticket-form textarea').setValue('请帮我同步用户手机号和支付订单号。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(addTicketComment).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('评论内容涉嫌敏感信息，无法提交')
    expect(wrapper.text()).toContain('trace-comment-403')
  })

  it('clears the previous ticket when navigating to an invalid ticket id', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())

    const wrapper = await mountTicketDetailView()

    assignRouteState(route, {
      path: '/tickets/not-a-number',
      params: { id: 'not-a-number' },
      query: {},
      fullPath: '/tickets/not-a-number',
    })
    await flushPromises()

    expect(wrapper.text()).toContain('工单 ID 不合法')
    expect(wrapper.text()).not.toContain('支付回调接口偶发超时')
  })

  it('shows the new remote comment and resets the form after a successful submission', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])
    addTicketComment.mockResolvedValue(createTicketDetailFixture({
      comments: [
        {
          id: 2,
          authorName: '李晓安',
          content: '已联系支付网关同学确认回调白名单配置。',
          commentTypeLabel: '处理说明',
          internal: true,
          createTime: '2026-05-14T10:20:00',
        },
        {
          id: 1,
          authorName: '王小明',
          content: '用户反馈支付成功但页面未刷新。',
          commentTypeLabel: '普通评论',
          internal: false,
          createTime: '2026-05-14T09:30:00',
        },
      ],
    }))

    const wrapper = await mountTicketDetailView()
    const commentForm = wrapper.find('form.ticket-form')
    const commentTypeSelect = commentForm.find('select')
    const commentInternal = commentForm.find('input[type="checkbox"]')
    const commentTextarea = commentForm.find('textarea')

    await commentTypeSelect.setValue('2')
    await commentInternal.setValue(true)
    await commentTextarea.setValue('已联系支付网关同学确认回调白名单配置。')
    await commentForm.trigger('submit.prevent')
    await flushPromises()

    expect(addTicketComment).toHaveBeenCalledTimes(1)
    expect(addTicketComment).toHaveBeenCalledWith(101, {
      content: '已联系支付网关同学确认回调白名单配置。',
      commentType: 2,
      internal: true,
    })
    expect(wrapper.text()).toContain('评论已提交。')
    expect(wrapper.text()).toContain('已联系支付网关同学确认回调白名单配置。')
    expect((commentTextarea.element as HTMLTextAreaElement).value).toBe('')
    expect((commentTypeSelect.element as HTMLSelectElement).value).toBe('1')
    expect((commentInternal.element as HTMLInputElement).checked).toBe(false)
  })

  it('falls back to built-in assignee options when assignee loading hits a network-like failure', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-assignee-503',
    }))

    const wrapper = await mountTicketDetailView()

    const options = wrapper.findAll('select.field-control option').map((item) => item.text())

    expect(fetchTicketAssignees).toHaveBeenCalledTimes(1)
    expect(options).toContain('李晓安 · SUPPORT')
    expect(options).toContain('林哲 · SUPPORT')
    expect(options).toContain('系统管理员 · ADMIN')
    expect(wrapper.text()).not.toContain('处理人列表加载失败，请稍后重试。')
  })

  it('writes comments into the local workflow without calling the remote api', async () => {
    const localTicket = createLocalTicket({
      title: '本地支付回调排查',
      content: '后端接口暂不可用，先在本地工作流记录处理过程。',
      type: 'INCIDENT',
      categoryId: 3,
      priority: 3,
    })
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())
    assignRouteState(route, {
      path: `/tickets/${localTicket.id}`,
      params: { id: String(localTicket.id) },
      query: {},
      fullPath: `/tickets/${localTicket.id}`,
    })

    const wrapper = await mountTicketDetailView()
    const commentForm = wrapper.find('form.ticket-form')
    const commentTextarea = commentForm.find('textarea')

    await commentTextarea.setValue('先记录在本地，待远程接口恢复后再同步。')
    await commentForm.trigger('submit.prevent')
    await flushPromises()

    expect(fetchTicketDetail).not.toHaveBeenCalled()
    expect(addTicketComment).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('评论已写入本地工作流。')
    expect(wrapper.text()).toContain('先记录在本地，待远程接口恢复后再同步。')
    expect((commentTextarea.element as HTMLTextAreaElement).value).toBe('')
  })

  it('keeps the ticket context and entered remark visible when status update fails', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])
    updateTicketStatus.mockRejectedValue(Object.assign(new Error('状态已被其他人更新，请刷新后重试'), {
      status: 409,
      traceId: 'trace-status-409',
    }))

    const wrapper = await mountTicketDetailView()

    const statusForm = wrapper.findAll('form.ticket-form')[2]
    const statusSelect = statusForm.find('select')
    const remarkInput = statusForm.find('textarea')

    await statusSelect.setValue('3')
    await remarkInput.setValue('已补充支付网关日志，准备改为已解决。')
    await statusForm.trigger('submit.prevent')
    await flushPromises()

    expect(updateTicketStatus).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('支付回调失败')
    expect(wrapper.text()).toContain('状态已被其他人更新，请刷新后重试')
    expect(wrapper.text()).toContain('trace-status-409')
    expect((remarkInput.element as HTMLTextAreaElement).value).toBe('已补充支付网关日志，准备改为已解决。')
  })

  it('updates the assignee on screen and shows the success message after a successful assignment', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())
    assignTicket.mockResolvedValue(createTicketDetailFixture({
      assigneeUserId: 102,
      assigneeName: '林哲',
    }))

    const wrapper = await mountTicketDetailView()
    const assignForm = wrapper.findAll('form.ticket-form')[1]
    const assignSelect = assignForm.find('select')
    const assignRemark = assignForm.find('textarea')

    await assignSelect.setValue('102')
    await assignRemark.setValue('转给林哲继续跟进回调配置。')
    await assignForm.trigger('submit.prevent')
    await flushPromises()

    expect(assignTicket).toHaveBeenCalledTimes(1)
    expect(assignTicket).toHaveBeenCalledWith(101, {
      assigneeUserId: 102,
      remark: '转给林哲继续跟进回调配置。',
    })
    expect(wrapper.text()).toContain('当前处理人：林哲')
    expect(wrapper.text()).toContain('处理人已更新。')
    expect((assignRemark.element as HTMLTextAreaElement).value).toBe('')
  })

  it('closes the ticket and redirects into real knowledge draft editing when capture is confirmed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())
    updateTicketStatus.mockResolvedValue(createTicketDetailFixture({
      status: 4,
      statusLabel: '已关闭',
    }))
    createTicketKnowledgeDraft.mockResolvedValue({ id: 501 })

    const wrapper = await mountTicketDetailView()
    const statusForm = wrapper.findAll('form.ticket-form')[2]
    const statusSelect = statusForm.find('select')
    const remarkInput = statusForm.find('textarea')

    await statusSelect.setValue('4')
    await remarkInput.setValue('已确认回调恢复，关闭工单并沉淀处理经验。')
    await statusForm.trigger('submit.prevent')
    await flushPromises()
    const seed = consumeKnowledgeDraftSeed()

    expect(updateTicketStatus).toHaveBeenCalledTimes(1)
    expect(updateTicketStatus).toHaveBeenCalledWith(101, {
      status: 4,
      remark: '已确认回调恢复，关闭工单并沉淀处理经验。',
    })
    expect(createTicketKnowledgeDraft).toHaveBeenCalledTimes(1)
    expect(createTicketKnowledgeDraft).toHaveBeenCalledWith(101, {
      origin: 'ticket-close',
      closeRemark: '已确认回调恢复，关闭工单并沉淀处理经验。',
    })
    expect(seed?.sourceTicketId).toBe(101)
    expect(seed?.origin).toBe('ticket-close')
    expect(seed?.closeRemark).toBe('已确认回调恢复，关闭工单并沉淀处理经验。')
    expect(wrapper.text()).toContain('已基于当前工单生成真实知识草稿，正在进入编辑页。')
    expect(push).toHaveBeenCalledWith('/knowledge/articles/501/edit?from=ticket-close')
  })

  it('reuses the existing local knowledge draft instead of creating a new one', async () => {
    saveKnowledgeDraft({
      id: 701,
      title: '支付回调失败处理复盘',
      summary: '已有本地知识草稿',
      content: '先看本地草稿，再继续补齐处理过程。',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
      sourceTicket: {
        id: 101,
        ticketNo: 'TK-101',
        title: '支付回调失败',
      },
    })
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      status: 3,
      statusLabel: '已解决',
    }))
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])

    const wrapper = await mountTicketDetailView()

    const knowledgeButton = wrapper.findAll('button').find((item) => item.text().includes('沉淀为知识文章'))
    expect(knowledgeButton).toBeTruthy()

    await knowledgeButton!.trigger('click')
    await flushPromises()

    expect(createTicketKnowledgeDraft).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('这张工单已经有本地草稿，正在继续编辑。')
    expect(push).toHaveBeenCalledWith('/knowledge/articles/701/edit?from=ticket')
  })

  it('closes the ticket without creating knowledge when capture is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())
    updateTicketStatus.mockResolvedValue(createTicketDetailFixture({
      status: 4,
      statusLabel: '已关闭',
    }))

    const wrapper = await mountTicketDetailView()
    const statusForm = wrapper.findAll('form.ticket-form')[2]
    const statusSelect = statusForm.find('select')
    const remarkInput = statusForm.find('textarea')

    await statusSelect.setValue('4')
    await remarkInput.setValue('问题已处理完成，用户已确认恢复。')
    await statusForm.trigger('submit.prevent')
    await flushPromises()

    expect(updateTicketStatus).toHaveBeenCalledTimes(1)
    expect(createTicketKnowledgeDraft).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('状态已更新，这张工单已经适合继续沉淀知识文章。')
    expect(push).not.toHaveBeenCalled()
    expect((remarkInput.element as HTMLTextAreaElement).value).toBe('')
  })

  it('shows top error details when knowledge draft creation fails with a business error', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      status: 3,
      statusLabel: '已解决',
    }))
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])
    createTicketKnowledgeDraft.mockRejectedValue(Object.assign(new Error('当前工单不允许继续沉淀知识草稿'), {
      status: 409,
      traceId: 'trace-kb-409',
    }))

    const wrapper = await mountTicketDetailView()

    const knowledgeButton = wrapper.findAll('button').find((item) => item.text().includes('沉淀为知识文章'))
    expect(knowledgeButton).toBeTruthy()

    await knowledgeButton!.trigger('click')
    await flushPromises()

    expect(createTicketKnowledgeDraft).toHaveBeenCalledTimes(1)
    expect(push).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前工单不允许继续沉淀知识草稿')
    expect(wrapper.text()).toContain('trace-kb-409')
  })

  it('falls back to the local knowledge draft route when knowledge draft creation hits a network-like failure', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      status: 4,
      statusLabel: '已关闭',
    }))
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])
    createTicketKnowledgeDraft.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-kb-503',
    }))

    const wrapper = await mountTicketDetailView()

    const knowledgeButton = wrapper.findAll('button').find((item) => item.text().includes('沉淀为知识文章'))
    expect(knowledgeButton).toBeTruthy()

    await knowledgeButton!.trigger('click')
    await flushPromises()

    const seed = consumeKnowledgeDraftSeed()

    expect(createTicketKnowledgeDraft).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('/knowledge/articles/create?from=ticket')
    expect(seed?.sourceTicketId).toBe(101)
    expect(seed?.sourceTicketNo).toBe('TK-101')
    expect(seed?.title).toContain('支付回调失败')
    expect(seed?.origin).toBe('manual')
  })

  it('validates empty comment submission before calling the api', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])

    const wrapper = await mountTicketDetailView()

    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(addTicketComment).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请先输入评论内容')
  })

  it('shows public-only comment guidance when internal comments are unavailable and still submits as a normal public comment', async () => {
    authzState.canUseInternalTicketComments = false
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue([createDefaultAssigneeOptions()[0]])
    addTicketComment.mockResolvedValue(createTicketDetailFixture({
      comments: [
        {
          id: 2,
          authorName: '王小明',
          content: '补充外部可见反馈。',
          commentTypeLabel: '普通评论',
          internal: false,
          createTime: '2026-05-14T10:40:00',
        },
      ],
    }))

    const wrapper = await mountTicketDetailView()
    const commentForm = wrapper.find('form.ticket-form')
    const commentTextarea = commentForm.find('textarea')

    expect(wrapper.text()).toContain('当前将以普通协作评论提交')
    expect(commentForm.find('select').exists()).toBe(false)

    await commentTextarea.setValue('补充外部可见反馈。')
    await commentForm.trigger('submit.prevent')
    await flushPromises()

    expect(addTicketComment).toHaveBeenCalledWith(101, {
      content: '补充外部可见反馈。',
      commentType: 1,
      internal: false,
    })
  })

  it('validates assignment input before calling the assignment api', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())

    const wrapper = await mountTicketDetailView()
    const assignForm = wrapper.findAll('form.ticket-form')[1]
    await assignForm.find('select').setValue('0')

    await assignForm.trigger('submit.prevent')
    await flushPromises()

    expect(assignTicket).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请先选择处理人')
  })

  it('redirects back to the ticket list when the current user cannot access the ticket scope', async () => {
    authzState.canViewAllTickets = false
    authState.user = createAuthUser({
      id: 2,
      username: 'user01',
      nickname: '业务小王',
      realName: '王晨',
    })
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      submitUserId: 7,
      submitterName: '提交人A',
      assigneeUserId: 8,
      assigneeName: '其他处理人',
    }))
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())

    await mountTicketDetailView()

    expect(replace).toHaveBeenCalledWith('/tickets')
  })

  it('shows a top-level business error when ticket detail loading fails directly', async () => {
    fetchTicketDetail.mockRejectedValue(Object.assign(new Error('该工单当前暂不允许查看详情'), {
      status: 409,
      traceId: 'trace-ticket-detail-409',
    }))
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())

    const wrapper = await mountTicketDetailView()

    expect(wrapper.text()).toContain('没有找到对应工单。')
  })

  it('shows the knowledge return banner, focuses the requested context, and builds knowledge list links from ticket context', async () => {
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      status: 3,
      statusLabel: '已解决',
      relatedArticles: [
        {
          id: 66,
          title: '支付回调失败排查手册',
          reason: '相似场景',
        },
      ],
    }))
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())
    assignRouteState(route, {
      path: '/tickets/101',
      params: { id: '101' },
      query: {
        fromKnowledge: '1',
        focus: 'knowledge',
      },
      fullPath: '/tickets/101?fromKnowledge=1&focus=knowledge',
    })

    const wrapper = await mountTicketDetailView()

    expect(wrapper.text()).toContain('你正在回看这篇知识文章对应的工单上下文。')
    expect(wrapper.find('.section-highlight').exists()).toBe(true)
    expect(window.HTMLElement.prototype.scrollIntoView).toHaveBeenCalled()
    const knowledgeListLink = wrapper.findAll('a').find((item) => item.text().includes('查看同工单文章'))
    expect(knowledgeListLink?.attributes('href')).toBe('/knowledge/articles')
  })

  it('filters comments and hides operation panels when assignment and transition permissions are both unavailable', async () => {
    authzState.canAssignTickets = false
    authzState.canTransitionTickets = false
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      comments: [
        {
          id: 1,
          authorName: '王小明',
          content: '外部评论',
          commentTypeLabel: '普通评论',
          internal: false,
          createTime: '2026-05-14T09:00:00',
        },
        {
          id: 2,
          authorName: '李晓安',
          content: '内部备注',
          commentTypeLabel: '内部备注',
          internal: true,
          createTime: '2026-05-14T10:00:00',
        },
      ],
    }))
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())

    const wrapper = await mountTicketDetailView()

    expect(wrapper.text()).toContain('当前权限')
    expect(wrapper.text()).not.toContain('指派处理人')
    expect(wrapper.findAll('form.ticket-form')).toHaveLength(1)

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '内部备注')!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('内部备注')
    expect(wrapper.text()).not.toContain('外部评论')
    expect(wrapper.findAll('.comment-card')).toHaveLength(1)

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '对外可见')!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('外部评论')
    expect(wrapper.text()).not.toContain('内部评论')
    expect(wrapper.findAll('.comment-card')).toHaveLength(1)
  })

  it('uses local assignment and local status transitions without remote apis and updates knowledge prompt state', async () => {
    const localTicket = createLocalTicket({
      title: '本地待沉淀工单',
      content: '先在本地闭环。',
      type: 'INCIDENT',
      categoryId: 3,
      priority: 3,
    })
    fetchTicketAssignees.mockResolvedValue(createDefaultAssigneeOptions())
    assignRouteState(route, {
      path: `/tickets/${localTicket.id}`,
      params: { id: String(localTicket.id) },
      query: {},
      fullPath: `/tickets/${localTicket.id}`,
    })

    const wrapper = await mountTicketDetailView()
    const assignForm = wrapper.findAll('form.ticket-form')[1]
    await assignForm.find('select').setValue('102')
    await assignForm.find('textarea').setValue('先转给林哲')
    await assignForm.trigger('submit.prevent')
    await flushPromises()

    expect(assignTicket).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('处理人已写入本地工作流。')
    expect(wrapper.text()).toContain('当前处理人：林哲')

    const statusForm = wrapper.findAll('form.ticket-form')[2]
    await statusForm.find('select').setValue('3')
    await statusForm.find('textarea').setValue('本地已解决')
    await statusForm.trigger('submit.prevent')
    await flushPromises()

    expect(updateTicketStatus).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('状态已更新，本地工单已经适合沉淀知识文章。')
    expect(wrapper.text()).toContain('这张工单已经处理完成，适合继续沉淀知识文章。')
  })

  it('blocks status updates when transition permission is unavailable and skips assignee loading when assignment is disabled', async () => {
    authzState.canAssignTickets = false
    authzState.canTransitionTickets = false
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture())

    const wrapper = await mountTicketDetailView()

    expect(fetchTicketAssignees).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前权限')
  })
})
