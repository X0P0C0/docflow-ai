import { flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getKnowledgeDraft, saveKnowledgeDraft } from '../../src/mock/knowledgeDrafts'
import { createLocalTicket } from '../../src/mock/ticketWorkspace'
import type { KnowledgeArticleApiItem } from '../../src/types/dashboard'
import { createTicketDetailFixture } from './helpers/fixtures'
import { mountKnowledgeArticleDetailView } from './helpers/pageMounts'
import { assignRouteState, resetWebStorage } from './helpers/testHarness'

const {
  push,
  fetchKnowledgeArticleDetail,
  fetchKnowledgeArticles,
  restoreKnowledgeArticleVersion,
  archiveKnowledgeArticle,
  deleteKnowledgeArticle,
  fetchTicketDetail,
  canManageState,
} = vi.hoisted(() => ({
  push: vi.fn(),
  fetchKnowledgeArticleDetail: vi.fn(),
  fetchKnowledgeArticles: vi.fn(),
  restoreKnowledgeArticleVersion: vi.fn(),
  archiveKnowledgeArticle: vi.fn(),
  deleteKnowledgeArticle: vi.fn(),
  fetchTicketDetail: vi.fn(),
  canManageState: { value: true },
}))

const route = reactive({
  path: '/knowledge/articles/501',
  params: { id: '501' },
  query: {},
  fullPath: '/knowledge/articles/501',
})

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :data-to="JSON.stringify(to)" :href="typeof to === \'string\' ? to : to.path"><slot /></a>',
  },
  useRoute: () => route,
  useRouter: () => ({
    push,
  }),
}))

vi.mock('../../src/api/knowledge', () => ({
  fetchKnowledgeArticleDetail,
  fetchKnowledgeArticles,
  restoreKnowledgeArticleVersion,
  archiveKnowledgeArticle,
  deleteKnowledgeArticle,
}))

vi.mock('../../src/api/ticket', () => ({
  fetchTicketDetail,
}))

vi.mock('../../src/authz', () => ({
  canManageKnowledgeArticles: () => canManageState.value,
}))

function createKnowledgeArticleDetailFixture(overrides: Partial<KnowledgeArticleApiItem> = {}): KnowledgeArticleApiItem {
  return {
    id: 501,
    title: '支付回调失败排查手册',
    summary: '用于指导支持人员快速定位支付回调失败问题。',
    content: '先检查签名配置。再检查回调地址连通性。最后检查幂等控制和日志链路。',
    categoryId: 3,
    sourceTicketId: null,
    authorUserId: 7,
    status: 1,
    viewCount: 888,
    likeCount: 22,
    collectCount: 13,
    publishTime: '2026-05-14T09:00:00',
    createTime: '2026-05-14T09:00:00',
    updateTime: '2026-05-14T10:00:00',
    versions: [
      {
        id: 9002,
        versionNo: 2,
        title: '支付回调失败排查手册',
        summary: '当前版本',
        remark: '补充幂等检查步骤',
        operatorUserId: 7,
        createTime: '2026-05-14T10:00:00',
      },
      {
        id: 9001,
        versionNo: 1,
        title: '支付回调失败初版',
        summary: '历史版本',
        remark: '初始沉淀版本',
        operatorUserId: 7,
        createTime: '2026-05-14T09:00:00',
      },
    ],
    ...overrides,
  }
}

describe('KnowledgeArticleDetailView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    push.mockReset()
    fetchKnowledgeArticleDetail.mockReset()
    fetchKnowledgeArticles.mockReset()
    restoreKnowledgeArticleVersion.mockReset()
    archiveKnowledgeArticle.mockReset()
    deleteKnowledgeArticle.mockReset()
    fetchTicketDetail.mockReset()
    canManageState.value = true
    resetWebStorage()
    assignRouteState(route, {
      path: '/knowledge/articles/501',
      params: { id: '501' },
      query: {},
      fullPath: '/knowledge/articles/501',
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads the source ticket summary and exposes ticket-context links from the article banner', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture({
      sourceTicket: {
        id: 101,
        ticketNo: 'TK-101',
        title: '支付回调失败',
      },
    }))
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      status: 3,
      statusLabel: '已解决',
      assigneeName: '林哲',
      timeline: [
        {
          id: 2,
          operatorName: '林哲',
          title: '已完成回调补偿',
          desc: '补偿执行完成，等待业务确认。',
          createTime: '2026-05-14T11:00:00',
        },
      ],
      comments: [
        {
          id: 2,
          authorName: '林哲',
          content: '已确认支付回调恢复。',
          commentTypeLabel: '处理说明',
          internal: false,
          createTime: '2026-05-14T11:05:00',
        },
      ],
    }))
    fetchKnowledgeArticles.mockResolvedValue([])

    const wrapper = await mountKnowledgeArticleDetailView()
    const sourceLinks = wrapper.findAll('a.source-ticket-link')

    expect(fetchKnowledgeArticleDetail).toHaveBeenCalledWith(501)
    expect(fetchTicketDetail).toHaveBeenCalledWith(101)
    expect(wrapper.text()).toContain('来源工单：TK-101')
    expect(wrapper.text()).toContain('当前状态已解决')
    expect(wrapper.text()).toContain('当前处理人林哲')
    expect(sourceLinks).toHaveLength(2)
    expect(sourceLinks[0].attributes('data-to')).toContain('"path":"/tickets/101"')
    expect(sourceLinks[0].attributes('data-to')).toContain('"focus":"timeline"')
    expect(sourceLinks[0].attributes('data-to')).toContain('"fromKnowledge":"1"')
    expect(sourceLinks[1].attributes('data-to')).toContain('"focus":"comments"')
  })

  it('restores an older version and refreshes the visible article content', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture())
    fetchKnowledgeArticles.mockResolvedValue([])
    restoreKnowledgeArticleVersion.mockResolvedValue(createKnowledgeArticleDetailFixture({
      title: '支付回调失败初版',
      summary: '恢复到历史版本后的摘要',
      content: '先检查支付配置。再查看历史补偿记录。',
      updateTime: '2026-05-14T12:00:00',
    }))

    const wrapper = await mountKnowledgeArticleDetailView()
    const restoreButtons = wrapper.findAll('button.version-action')

    expect(restoreButtons).toHaveLength(1)
    await restoreButtons[0].trigger('click')
    await flushPromises()

    expect(restoreKnowledgeArticleVersion).toHaveBeenCalledWith(501, 9001)
    expect(wrapper.text()).toContain('支付回调失败初版')
    expect(wrapper.text()).toContain('恢复到历史版本后的摘要')
  })

  it('shows backend business errors and traceId when version restore fails', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture())
    fetchKnowledgeArticles.mockResolvedValue([])
    restoreKnowledgeArticleVersion.mockRejectedValue(Object.assign(new Error('该版本已被归档，不能恢复'), {
      status: 409,
      traceId: 'trace-restore-409',
    }))

    const wrapper = await mountKnowledgeArticleDetailView()

    await wrapper.find('button.version-action').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('该版本已被归档，不能恢复')
    expect(wrapper.text()).toContain('trace-restore-409')
  })

  it('archives a remote article successfully and updates the status on screen', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture({
      status: 1,
    }))
    fetchKnowledgeArticles.mockResolvedValue([])
    archiveKnowledgeArticle.mockResolvedValue(createKnowledgeArticleDetailFixture({
      status: 2,
      publishTime: '2026-05-14T09:00:00',
    }))

    const wrapper = await mountKnowledgeArticleDetailView()

    const archiveButton = wrapper.findAll('button').find((item) => item.text().includes('归档文章'))
    expect(archiveButton).toBeTruthy()
    await archiveButton!.trigger('click')
    await flushPromises()

    expect(archiveKnowledgeArticle).toHaveBeenCalledWith(501)
    expect(wrapper.text()).toContain('已归档')
  })

  it('archives a local draft without calling the remote archive api', async () => {
    saveKnowledgeDraft({
      id: 8801,
      title: '本地知识草稿',
      summary: '先在本地沉淀。',
      content: '本地排查步骤。本地结论。',
      categoryId: 2,
      authorUserId: 7,
      status: 0,
      publishTime: null,
    })
    fetchKnowledgeArticles.mockResolvedValue([])
    assignRouteState(route, {
      path: '/knowledge/articles/8801',
      params: { id: '8801' },
      query: {},
      fullPath: '/knowledge/articles/8801',
    })

    const wrapper = await mountKnowledgeArticleDetailView()

    const archiveButton = wrapper.findAll('button').find((item) => item.text().includes('归档文章'))
    expect(archiveButton).toBeTruthy()
    await archiveButton!.trigger('click')
    await flushPromises()

    expect(fetchKnowledgeArticleDetail).not.toHaveBeenCalled()
    expect(archiveKnowledgeArticle).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('本地知识草稿')
    expect(wrapper.text()).toContain('已归档')
  })

  it('shows backend business errors and traceId when archiving fails', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture())
    fetchKnowledgeArticles.mockResolvedValue([])
    archiveKnowledgeArticle.mockRejectedValue(Object.assign(new Error('当前文章状态不允许归档'), {
      status: 409,
      traceId: 'trace-archive-409',
    }))

    const wrapper = await mountKnowledgeArticleDetailView()

    await wrapper.findAll('button').find((item) => item.text().includes('归档文章'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('当前文章状态不允许归档')
    expect(wrapper.text()).toContain('trace-archive-409')
  })

  it('deletes a remote article after confirmation and navigates back to the knowledge list', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture())
    fetchKnowledgeArticles.mockResolvedValue([])

    const wrapper = await mountKnowledgeArticleDetailView()

    const deleteButton = wrapper.findAll('button').find((item) => item.text().includes('删除文章'))
    expect(deleteButton).toBeTruthy()
    await deleteButton!.trigger('click')
    await flushPromises()

    expect(deleteKnowledgeArticle).toHaveBeenCalledWith(501)
    expect(push).toHaveBeenCalledWith('/knowledge/articles')
  })

  it('deletes a local draft without calling the remote delete api', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    saveKnowledgeDraft({
      id: 8802,
      title: '待删除的本地草稿',
      summary: '本地删除测试',
      content: '本地草稿内容',
      categoryId: 2,
      authorUserId: 7,
      status: 0,
      publishTime: null,
    })
    fetchKnowledgeArticles.mockResolvedValue([])
    assignRouteState(route, {
      path: '/knowledge/articles/8802',
      params: { id: '8802' },
      query: {},
      fullPath: '/knowledge/articles/8802',
    })

    const wrapper = await mountKnowledgeArticleDetailView()

    const deleteButton = wrapper.findAll('button').find((item) => item.text().includes('删除文章'))
    expect(deleteButton).toBeTruthy()
    await deleteButton!.trigger('click')
    await flushPromises()

    expect(deleteKnowledgeArticle).not.toHaveBeenCalled()
    expect(getKnowledgeDraft(8802)).toBeNull()
    expect(push).toHaveBeenCalledWith('/knowledge/articles')
  })

  it('shows backend business errors and traceId when deletion fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture())
    fetchKnowledgeArticles.mockResolvedValue([])
    deleteKnowledgeArticle.mockRejectedValue(Object.assign(new Error('文章已被其他管理员处理，请刷新后重试'), {
      status: 409,
      traceId: 'trace-delete-409',
    }))

    const wrapper = await mountKnowledgeArticleDetailView()

    await wrapper.findAll('button').find((item) => item.text().includes('删除文章'))!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('文章已被其他管理员处理，请刷新后重试')
    expect(wrapper.text()).toContain('trace-delete-409')
  })

  it('shows an inline error immediately when the route article id is invalid', async () => {
    assignRouteState(route, {
      path: '/knowledge/articles/not-a-number',
      params: { id: 'not-a-number' },
      query: {},
      fullPath: '/knowledge/articles/not-a-number',
    })

    const wrapper = await mountKnowledgeArticleDetailView()

    expect(fetchKnowledgeArticleDetail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('文章 ID 不合法')
  })

  it('toggles AI summary, related filters, sort mode, and falls back to generic source-ticket routes when no ticket is linked', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture({
      sourceTicket: null,
      content: '支付失败排查。回调补偿方案。日志核对。',
    }))
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleDetailFixture({
        id: 601,
        title: '支付失败 FAQ',
        sourceTicket: {
          id: 101,
          ticketNo: 'TK-101',
          title: '支付回调失败',
        },
      }),
      createKnowledgeArticleDetailFixture({
        id: 602,
        title: '订单回调常见问题',
        categoryId: 3,
        sourceTicket: null,
      }),
      createKnowledgeArticleDetailFixture({
        id: 603,
        title: '系统使用指南',
        categoryId: 1,
        sourceTicket: null,
      }),
    ])

    const wrapper = await mountKnowledgeArticleDetailView()

    await wrapper.findAll('button').find((item) => item.text().includes('隐藏 AI 摘要'))!.trigger('click')
    expect(wrapper.text()).toContain('AI 摘要已收起')

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '同分类')!.trigger('click')
    await wrapper.find('select.knowledge-sort-select').setValue('title')
    await flushPromises()

    const relatedLinks = wrapper.findAll('a.related-link')
    const relatedText = relatedLinks.map((item) => item.text()).join(' ')
    expect(relatedText).toContain('支付失败 FAQ')
    expect(relatedText).toContain('订单回调常见问题')
    expect(relatedText).not.toContain('系统使用指南')
    expect(wrapper.findAll('a.source-ticket-link')).toHaveLength(0)
  })

  it('does not delete when the confirmation dialog is cancelled and hides management actions for read-only users', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    canManageState.value = false
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture())
    fetchKnowledgeArticles.mockResolvedValue([])

    const wrapper = await mountKnowledgeArticleDetailView()

    expect(wrapper.findAll('button').some((item) => item.text().includes('编辑文章'))).toBe(false)
    expect(wrapper.findAll('button').some((item) => item.text().includes('删除文章'))).toBe(false)
    expect(wrapper.findAll('button').some((item) => item.text().includes('归档文章'))).toBe(false)
    expect(deleteKnowledgeArticle).not.toHaveBeenCalled()
  })

  it('navigates into edit mode and supports source-preview pinning and preview toggles', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture({
      sourceTicket: {
        id: 101,
        ticketNo: 'TK-101',
        title: '支付回调失败',
      },
    }))
    fetchTicketDetail.mockResolvedValue(createTicketDetailFixture({
      status: 2,
      statusLabel: '处理中',
      assigneeName: '李晓安',
      timeline: [
        {
          id: 11,
          operatorName: '李晓安',
          title: '开始排查',
          desc: '确认问题范围。',
          createTime: '2026-05-14T11:00:00',
        },
      ],
      comments: [
        {
          id: 21,
          authorName: '李晓安',
          content: '先看最近一次发布变更。',
          commentTypeLabel: '处理说明',
          internal: false,
          createTime: '2026-05-14T11:05:00',
        },
      ],
    }))
    fetchKnowledgeArticles.mockResolvedValue([])

    const wrapper = await mountKnowledgeArticleDetailView()

    await wrapper.findAll('button').find((item) => item.text().includes('编辑文章'))!.trigger('click')
    expect(push).toHaveBeenCalledWith('/knowledge/articles/501/edit')

    await wrapper.findAll('button').find((item) => item.text() === '展开最新评论')!.trigger('click')
    await wrapper.findAll('button').find((item) => item.text() === '展开最新记录')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('最新评论')
    expect(wrapper.text()).toContain('先看最近一次发布变更。')
    expect(wrapper.text()).toContain('最新处理记录')
    expect(wrapper.text()).toContain('开始排查')

    await wrapper.findAll('button').find((item) => item.text() === '固定到顶部')!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.panel-pinned').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('先看最近一次发布变更。')
    expect(wrapper.text()).toContain('取消固定')
  })

  it('handles source-summary failures and related-article fallback sorting without crashing', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue(createKnowledgeArticleDetailFixture({
      sourceTicket: {
        id: 101,
        ticketNo: 'TK-101',
        title: '支付回调失败',
      },
      categoryId: 3,
    }))
    fetchTicketDetail.mockRejectedValue(Object.assign(new Error('ticket fetch failed'), { status: 500 }))
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleDetailFixture({
        id: 610,
        title: 'B 文章',
        status: 2,
        sourceTicket: {
          id: 101,
          ticketNo: 'TK-101',
          title: '支付回调失败',
        },
      }),
      createKnowledgeArticleDetailFixture({
        id: 611,
        title: 'A 文章',
        status: 1,
        sourceTicket: null,
      }),
    ])

    const wrapper = await mountKnowledgeArticleDetailView()

    expect(wrapper.findAll('.source-ticket-summary').length).toBe(0)

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '同来源工单')!.trigger('click')
    await wrapper.find('select.knowledge-sort-select').setValue('status')
    await flushPromises()

    expect(wrapper.text()).toContain('B 文章')
    expect(wrapper.text()).not.toContain('A 文章')
  })

  it('uses the local ticket workspace for source-ticket summary when the linked ticket is local', async () => {
    const localTicket = createLocalTicket({
      title: '本地支付回调排查',
      content: '先在本地记录排查过程，待后端恢复后再同步。',
      type: 'INCIDENT',
      categoryId: 3,
      priority: 3,
    })
    saveKnowledgeDraft({
      id: 8804,
      title: '来自本地工单的知识草稿',
      summary: '沉淀本地工单里的处理经验。',
      content: '先记录排查过程。再补充最终结论。',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
      sourceTicket: {
        id: localTicket.id,
        ticketNo: localTicket.ticketNo,
        title: localTicket.title,
      },
    })
    fetchKnowledgeArticles.mockResolvedValue([])
    assignRouteState(route, {
      path: '/knowledge/articles/8804',
      params: { id: '8804' },
      query: {},
      fullPath: '/knowledge/articles/8804',
    })

    const wrapper = await mountKnowledgeArticleDetailView()

    expect(fetchKnowledgeArticleDetail).not.toHaveBeenCalled()
    expect(fetchTicketDetail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('来源工单摘要')
    expect(wrapper.text()).toContain('当前状态')
    expect(wrapper.text()).toContain(localTicket.status)
    expect(wrapper.text()).toContain(localTicket.assignee || '待分配')
  })

  it('shows local-draft version history without restore actions and falls back when related loading fails', async () => {
    saveKnowledgeDraft({
      id: 8803,
      title: '本地历史草稿',
      summary: '本地摘要',
      content: '本地内容。补充说明。',
      categoryId: 2,
      authorUserId: 7,
      status: 0,
      publishTime: null,
      versions: [],
    } as never)
    fetchKnowledgeArticles.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-related-503',
    }))
    assignRouteState(route, {
      path: '/knowledge/articles/8803',
      params: { id: '8803' },
      query: {},
      fullPath: '/knowledge/articles/8803',
    })

    const wrapper = await mountKnowledgeArticleDetailView()

    expect(wrapper.text()).toContain('当前还没有版本记录。')
    expect(wrapper.findAll('button.version-action')).toHaveLength(0)
    expect(wrapper.text()).toContain('当前筛选下还没有可展示的相关文章')
  })

  it('reloads article content when navigating to another knowledge detail route in the same component', async () => {
    fetchKnowledgeArticleDetail
      .mockResolvedValueOnce(createKnowledgeArticleDetailFixture({
        id: 501,
        title: '支付回调失败排查手册',
      }))
      .mockResolvedValueOnce(createKnowledgeArticleDetailFixture({
        id: 602,
        title: '订单回调常见问题',
        summary: '切换路由后应重新加载新的文章内容。',
      }))
    fetchKnowledgeArticles.mockResolvedValue([])

    const wrapper = await mountKnowledgeArticleDetailView()

    assignRouteState(route, {
      path: '/knowledge/articles/602',
      params: { id: '602' },
      query: {},
      fullPath: '/knowledge/articles/602',
    })
    await flushPromises()

    expect(fetchKnowledgeArticleDetail).toHaveBeenNthCalledWith(2, 602)
    expect(wrapper.text()).toContain('订单回调常见问题')
    expect(wrapper.text()).toContain('切换路由后应重新加载新的文章内容。')
  })
})
