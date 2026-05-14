import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { saveKnowledgeDraft } from '../../src/mock/knowledgeDrafts'
import { saveArticleSourceTicket } from '../../src/utils/knowledgeSourceTicket'
import type { KnowledgeArticleApiItem } from '../../src/types/dashboard'
import KnowledgeArticleListView from '../../src/views/KnowledgeArticleListView.vue'
import { resetWebStorage } from './helpers/testHarness'

const { push, replace, fetchKnowledgeArticles, canManageState } = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  fetchKnowledgeArticles: vi.fn(),
  canManageState: { value: true },
}))

const route = {
  query: {},
  fullPath: '/knowledge/articles',
}

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

vi.mock('../../src/api/knowledge', () => ({
  fetchKnowledgeArticles,
}))

vi.mock('../../src/authz', () => ({
  canManageKnowledgeArticles: () => canManageState.value,
}))

function createKnowledgeArticleFixture(overrides: Partial<KnowledgeArticleApiItem> = {}): KnowledgeArticleApiItem {
  return {
    id: 801,
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

describe('KnowledgeArticleListView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    push.mockReset()
    replace.mockReset()
    fetchKnowledgeArticles.mockReset()
    canManageState.value = true
    resetWebStorage()
    route.query = {}
    route.fullPath = '/knowledge/articles'
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function mountView() {
    const wrapper = mount(KnowledgeArticleListView, {
      global: {
        stubs: {
          AppSidebar: true,
          AppTopbar: true,
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  it('shows a fallback notice without exposing the trace id when remote loading fails with a network-like error', async () => {
    saveKnowledgeDraft({
      id: 9901,
      title: '本地支付草稿',
      summary: '网络异常时先保存到本地。',
      content: '本地草稿内容',
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
    fetchKnowledgeArticles.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-knowledge-list-503',
    }))

    const wrapper = await mountView()

    expect(fetchKnowledgeArticles).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('知识库接口暂时不可用，当前先回退到本地演示数据。')
    expect(wrapper.text()).not.toContain('trace-knowledge-list-503')
    expect(wrapper.findAll('.knowledge-card').length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('本地支付草稿')
  })

  it('loads with the source-ticket filter from route and clears it from the banner action', async () => {
    route.query = { sourceTicketNo: 'TK-101' }
    route.fullPath = '/knowledge/articles?sourceTicketNo=TK-101'
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture({
        id: 811,
        title: '来源于 TK-101 的文章',
        sourceTicketId: 101,
      }),
    ])

    const wrapper = await mountView()

    expect(fetchKnowledgeArticles).toHaveBeenCalledWith({
      keyword: undefined,
      sourceTicketNo: 'TK-101',
      categoryId: undefined,
      status: undefined,
    })
    expect(wrapper.text()).toContain('当前正在按来源工单筛选：')
    expect(wrapper.text()).toContain('TK-101')

    const clearButton = wrapper.findAll('button').find((item) => item.text() === '清除')
    expect(clearButton).toBeTruthy()
    await clearButton!.trigger('click')
    await flushPromises()

    expect(replace).toHaveBeenCalledWith({
      query: {
        keyword: undefined,
        sourceTicketNo: undefined,
        categoryId: undefined,
        status: undefined,
        quickFilter: undefined,
      },
    })
    expect(fetchKnowledgeArticles).toHaveBeenLastCalledWith({
      keyword: undefined,
      sourceTicketNo: undefined,
      categoryId: undefined,
      status: undefined,
    })
  })

  it('shows the forbidden-access notice when redirected back with reason=forbidden', async () => {
    route.query = { reason: 'forbidden' }
    route.fullPath = '/knowledge/articles?reason=forbidden'
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture(),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('当前账号没有执行该知识管理操作的权限，已为你返回可访问的知识列表。')
  })

  it('shows only source-linked articles when the ticket-derived quick filter is selected', async () => {
    saveArticleSourceTicket(821, {
      id: 201,
      ticketNo: 'TK-201',
      title: '支付回调失败',
    })
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture({
        id: 821,
        title: '来自工单沉淀的支付经验',
      }),
      createKnowledgeArticleFixture({
        id: 822,
        title: '普通系统使用指南',
        categoryId: 1,
        sourceTicketId: null,
        viewCount: 200,
      }),
    ])

    const wrapper = await mountView()

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '工单沉淀')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('来自工单沉淀的支付经验')
    expect(wrapper.text()).toContain('来源工单：TK-201')
    expect(wrapper.text()).not.toContain('普通系统使用指南')
  })

  it('shows local drafts in the local quick filter even when remote articles are also loaded', async () => {
    saveKnowledgeDraft({
      id: 9902,
      title: '本地发布复盘草稿',
      summary: '待补全影响范围和预防动作。',
      content: '本地草稿内容',
      categoryId: 2,
      authorUserId: 9,
      status: 0,
      publishTime: null,
    })
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture({
        id: 831,
        title: '远程真实知识文章',
      }),
    ])

    const wrapper = await mountView()

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '本地内容')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('本地发布复盘草稿')
    expect(wrapper.text()).toContain('本地草稿')
    expect(wrapper.text()).not.toContain('远程真实知识文章')
  })

  it('shows read-only hero copy, hides the create button, and supports list view with draft and archived filters', async () => {
    canManageState.value = false
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture({
        id: 841,
        title: '草稿知识文章',
        status: 0,
        publishTime: null,
        createTime: '2026-05-14T08:00:00',
      }),
      createKnowledgeArticleFixture({
        id: 842,
        title: '已归档知识文章',
        status: 2,
      }),
    ])

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('把排查经验整理成可搜索、可回查的知识资产')
    expect(wrapper.findAll('button').some((item) => item.text().includes('查看文章'))).toBe(false)

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '草稿')!.trigger('click')
    await wrapper.findAll('button').find((item) => item.text() === '列表视图')!.trigger('click')
    await flushPromises()

    expect(wrapper.find('.knowledge-list-view').exists()).toBe(true)
    expect(wrapper.text()).toContain('草稿知识文章')
    expect(wrapper.text()).not.toContain('已归档知识文章')

    await wrapper.findAll('button.quick-filter-chip').find((item) => item.text() === '已归档')!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('已归档知识文章')
    expect(wrapper.text()).not.toContain('草稿知识文章')
  })

  it('syncs filters from route, applies search params, and resets back to defaults', async () => {
    route.query = {
      keyword: '支付',
      sourceTicketNo: 'TK-201',
      categoryId: '3',
      status: '1',
      quickFilter: 'popular',
    }
    route.fullPath = '/knowledge/articles?keyword=%E6%94%AF%E4%BB%98&sourceTicketNo=TK-201&categoryId=3&status=1&quickFilter=popular'
    fetchKnowledgeArticles.mockResolvedValue([
      createKnowledgeArticleFixture({
        id: 851,
        title: '高浏览支付文章',
        sourceTicketId: 201,
      }),
    ])

    const wrapper = await mountView()

    expect(fetchKnowledgeArticles).toHaveBeenCalledWith({
      keyword: '支付',
      sourceTicketNo: 'TK-201',
      categoryId: 3,
      status: 1,
    })
    expect(wrapper.text()).toContain('高浏览支付文章')

    await wrapper.find('input[placeholder="搜索标题或摘要"]').setValue('  回调 ')
    await wrapper.find('input[placeholder="例如 INC-20260511"]').setValue(' TK-202 ')
    const selects = wrapper.findAll('select')
    await selects[0].setValue('2')
    await selects[1].setValue('0')
    await wrapper.find('form.filter-grid').trigger('submit.prevent')
    await flushPromises()

    expect(replace).toHaveBeenCalledWith({
      query: {
        keyword: '回调',
        sourceTicketNo: 'TK-202',
        categoryId: '2',
        status: '0',
        quickFilter: 'popular',
      },
    })
    expect(fetchKnowledgeArticles).toHaveBeenLastCalledWith({
      keyword: '回调',
      sourceTicketNo: 'TK-202',
      categoryId: 2,
      status: 0,
    })

    await wrapper.findAll('button').find((item) => item.text() === '重置')!.trigger('click')
    await flushPromises()

    expect(fetchKnowledgeArticles).toHaveBeenLastCalledWith({
      keyword: undefined,
      sourceTicketNo: undefined,
      categoryId: undefined,
      status: undefined,
    })
  })

  it('shows trace details for business errors and empty-state copy when no articles match', async () => {
    saveKnowledgeDraft({
      id: 9904,
      title: '本地知识草稿',
      summary: '业务错误时仍可保留本地草稿。',
      content: '本地知识内容',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
    })
    fetchKnowledgeArticles.mockRejectedValue(Object.assign(new Error('知识库查询条件不合法'), {
      status: 422,
      traceId: 'trace-knowledge-list-422',
    }))

    const errorWrapper = await mountView()

    expect(errorWrapper.text()).toContain('知识库查询条件不合法')
    expect(errorWrapper.text()).toContain('trace-knowledge-list-422')
    expect(errorWrapper.text()).not.toContain('当前先回退到本地演示数据')
    expect(errorWrapper.text()).toContain('本地知识草稿')

    fetchKnowledgeArticles.mockReset()
    fetchKnowledgeArticles.mockResolvedValue([])
    route.query = { quickFilter: 'published' }
    route.fullPath = '/knowledge/articles?quickFilter=published'

    const emptyWrapper = await mountView()

    expect(emptyWrapper.text()).toContain('当前筛选条件下还没有知识文章，可以继续新建一篇沉淀当前经验。')
  })
})
