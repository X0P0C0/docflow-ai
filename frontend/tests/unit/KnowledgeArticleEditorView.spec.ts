import { flushPromises } from '@vue/test-utils'
import { reactive } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../src/auth'
import { consumeKnowledgeDraftSeed, saveKnowledgeDraftSeed } from '../../src/utils/knowledgeFromTicket'
import { listKnowledgeDrafts, saveKnowledgeDraft } from '../../src/mock/knowledgeDrafts'
import { createAuthUser } from './helpers/fixtures'
import { mountKnowledgeArticleEditorView } from './helpers/pageMounts'
import { assignRouteState, resetWebStorage } from './helpers/testHarness'

const { push, createKnowledgeArticle, updateKnowledgeArticle, fetchKnowledgeArticleDetail, canManageState, isDemoMode } = vi.hoisted(() => ({
  push: vi.fn(),
  createKnowledgeArticle: vi.fn(),
  updateKnowledgeArticle: vi.fn(),
  fetchKnowledgeArticleDetail: vi.fn(),
  canManageState: { value: true },
  isDemoMode: vi.fn(),
}))

const route = reactive({
  path: '/knowledge/articles/create',
  params: {},
  query: {},
})

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({
    push,
  }),
}))

vi.mock('../../src/api/knowledge', () => ({
  createKnowledgeArticle,
  updateKnowledgeArticle,
  fetchKnowledgeArticleDetail,
}))

vi.mock('../../src/authz', () => ({
  canManageKnowledgeArticles: () => canManageState.value,
}))

vi.mock('../../src/utils/runtimeMode', () => ({
  isDemoMode,
}))

describe('KnowledgeArticleEditorView', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    push.mockReset()
    createKnowledgeArticle.mockReset()
    updateKnowledgeArticle.mockReset()
    fetchKnowledgeArticleDetail.mockReset()
    canManageState.value = true
    isDemoMode.mockReset()
    isDemoMode.mockReturnValue(false)
    resetWebStorage()
    assignRouteState(route, {
      path: '/knowledge/articles/create',
      params: {},
      query: {},
    })
    authState.token = 'prod-token'
    authState.restored = true
    authState.user = createAuthUser({
      username: 'editor',
      nickname: '知识管理员',
      realName: '知识管理员',
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function fillRequiredFields(wrapper: Awaited<ReturnType<typeof mountKnowledgeArticleEditorView>>) {
    const titleInput = wrapper.find('input[type="text"]')
    const contentInput = wrapper.find('.editor-textarea')

    await titleInput.setValue('支付回调失败排查手册')
    await contentInput.setValue('先检查签名配置，再检查回调地址连通性和幂等控制键，最后对照日志链路逐步排查。')
  }

  it('shows backend business errors and traceId for 4xx save failures', async () => {
    createKnowledgeArticle.mockRejectedValue(Object.assign(new Error('没有保存权限'), {
      status: 403,
      traceId: 'trace-403-editor',
    }))

    const wrapper = await mountKnowledgeArticleEditorView()
    await fillRequiredFields(wrapper)

    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).toHaveBeenCalledTimes(1)
    expect(push).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('没有保存权限')
    expect(wrapper.text()).toContain('trace-403-editor')
    expect(listKnowledgeDrafts()).toHaveLength(0)
  })

  it('loads a remote article into the form in edit mode when no local draft exists', async () => {
    assignRouteState(route, {
      path: '/knowledge/articles/66/edit',
      params: { id: '66' },
      query: {},
    })
    fetchKnowledgeArticleDetail.mockResolvedValue({
      id: 66,
      title: '远程知识文章标题',
      summary: '远程知识文章摘要',
      content: '远程知识文章正文，包含处理步骤和结论。',
      categoryId: 3,
      status: 0,
      sourceTicketId: null,
      createTime: '2026-05-14T09:00:00',
      updateTime: '2026-05-14T10:00:00',
      publishTime: null,
      authorName: '知识管理员',
      statusLabel: '草稿',
      viewCount: 0,
      likeCount: 0,
      collectCount: 0,
    })

    const wrapper = await mountKnowledgeArticleEditorView()
    const textareas = wrapper.findAll('textarea')

    expect(fetchKnowledgeArticleDetail).toHaveBeenCalledWith(66)
    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('远程知识文章标题')
    expect((textareas[0].element as HTMLTextAreaElement).value).toBe('远程知识文章摘要')
    expect((wrapper.find('.editor-textarea').element as HTMLTextAreaElement).value).toContain('远程知识文章正文')
  })

  it('shows backend business errors and traceId for 4xx load failures in edit mode', async () => {
    assignRouteState(route, {
      path: '/knowledge/articles/67/edit',
      params: { id: '67' },
      query: {},
    })
    fetchKnowledgeArticleDetail.mockRejectedValue(Object.assign(new Error('文章不存在或你没有访问权限'), {
      status: 404,
      traceId: 'trace-load-404',
    }))

    const wrapper = await mountKnowledgeArticleEditorView()

    expect(fetchKnowledgeArticleDetail).toHaveBeenCalledWith(67)
    expect(wrapper.text()).toContain('文章不存在或你没有访问权限')
    expect(wrapper.text()).toContain('trace-load-404')
    expect(listKnowledgeDrafts()).toHaveLength(0)
  })

  it('shows the local-create fallback message for network-like load failures in edit mode', async () => {
    assignRouteState(route, {
      path: '/knowledge/articles/68/edit',
      params: { id: '68' },
      query: {},
    })
    fetchKnowledgeArticleDetail.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-load-503',
    }))

    const wrapper = await mountKnowledgeArticleEditorView()

    expect(fetchKnowledgeArticleDetail).toHaveBeenCalledWith(68)
    expect(wrapper.text()).toContain('远程文章加载失败，当前可以继续新建本地草稿。')
    expect(wrapper.text()).not.toContain('trace-load-503')
    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('')
    expect(listKnowledgeDrafts()).toHaveLength(0)
  })

  it('shows an inline error instead of treating an invalid edit id as create mode', async () => {
    assignRouteState(route, {
      path: '/knowledge/articles/not-a-number/edit',
      params: { id: 'not-a-number' },
      query: {},
    })

    const wrapper = await mountKnowledgeArticleEditorView()

    expect(fetchKnowledgeArticleDetail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('文章 ID 不合法，无法进入编辑模式。')
    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('')
  })

  it('reuses the ticket seed in edit mode when the remote draft cannot be loaded', async () => {
    saveKnowledgeDraftSeed({
      title: '支付回调失败处理复盘',
      summary: '从工单沉淀入口带过来的摘要。',
      content: '一、问题现象\n支付回调失败。\n\n六、结论与预防\n待补充。',
      categoryId: 3,
      sourceTicketId: 701,
      sourceTicketNo: 'TK-701',
      sourceTicketTitle: '支付回调失败',
      origin: 'ticket-close',
      closeRemark: '业务已确认恢复。',
      ticketStatus: '已关闭',
    })
    assignRouteState(route, {
      path: '/knowledge/articles/68/edit',
      params: { id: '68' },
      query: { from: 'ticket-close' },
    })
    fetchKnowledgeArticleDetail.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-load-503',
    }))

    const wrapper = await mountKnowledgeArticleEditorView()

    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('支付回调失败处理复盘')
    expect(wrapper.text()).toContain('工单 TK-701 已关闭，已经帮你带出沉淀草稿和关闭备注。')
    expect(wrapper.text()).toContain('来源工单：TK-701 · 支付回调失败')
    expect(consumeKnowledgeDraftSeed()).toBeNull()
  })

  it('resets the form when navigating from edit mode back to create mode without a seed', async () => {
    assignRouteState(route, {
      path: '/knowledge/articles/66/edit',
      params: { id: '66' },
      query: {},
    })
    fetchKnowledgeArticleDetail.mockResolvedValue({
      id: 66,
      title: '远程知识文章标题',
      summary: '远程知识文章摘要',
      content: '远程知识文章正文，包含处理步骤和结论。',
      categoryId: 3,
      status: 0,
      sourceTicketId: null,
      createTime: '2026-05-14T09:00:00',
      updateTime: '2026-05-14T10:00:00',
      publishTime: null,
      authorName: '知识管理员',
      statusLabel: '草稿',
      viewCount: 0,
      likeCount: 0,
      collectCount: 0,
    })

    const wrapper = await mountKnowledgeArticleEditorView()

    assignRouteState(route, {
      path: '/knowledge/articles/create',
      params: {},
      query: {},
    })
    await flushPromises()

    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('')
    expect((wrapper.findAll('textarea')[0].element as HTMLTextAreaElement).value).toBe('')
    expect((wrapper.find('.editor-textarea').element as HTMLTextAreaElement).value).toBe('')
  })

  it('clears stale editor content before showing the no-permission message', async () => {
    fetchKnowledgeArticleDetail.mockResolvedValue({
      id: 66,
      title: '远程知识文章标题',
      summary: '远程知识文章摘要',
      content: '远程知识文章正文，包含处理步骤和结论。',
      categoryId: 3,
      status: 0,
      sourceTicketId: null,
      createTime: '2026-05-14T09:00:00',
      updateTime: '2026-05-14T10:00:00',
      publishTime: null,
      authorName: '知识管理员',
      statusLabel: '草稿',
      viewCount: 0,
      likeCount: 0,
      collectCount: 0,
    })

    const wrapper = await mountKnowledgeArticleEditorView()

    canManageState.value = false
    assignRouteState(route, {
      path: '/knowledge/articles/77/edit',
      params: { id: '77' },
      query: {},
    })
    await flushPromises()

    expect(wrapper.text()).toContain('当前账号只有阅读权限，不能新建或编辑知识文章。')
    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('')
    expect((wrapper.find('.editor-textarea').element as HTMLTextAreaElement).value).toBe('')
  })

  it('falls back to a local draft for 5xx save failures', async () => {
    vi.spyOn(Date, 'now').mockReturnValue(24680)
    createKnowledgeArticle.mockRejectedValue(Object.assign(new Error('服务暂时不可用'), {
      status: 503,
      traceId: 'trace-503-editor',
    }))

    const wrapper = await mountKnowledgeArticleEditorView()
    await fillRequiredFields(wrapper)

    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    const drafts = listKnowledgeDrafts()
    expect(createKnowledgeArticle).toHaveBeenCalledTimes(1)
    expect(drafts).toHaveLength(1)
    expect(drafts[0].id).toBe(24680)
    expect(drafts[0].title).toBe('支付回调失败排查手册')
    expect(drafts[0].status).toBe(0)
    expect(wrapper.text()).toContain('后端不可用，草稿已保存到本地。')
    expect(push).toHaveBeenCalledWith('/knowledge/articles/24680')
  })

  it('creates a real article from a local draft and removes the local draft after a successful save', async () => {
    saveKnowledgeDraft({
      id: 88,
      title: '历史草稿标题',
      summary: '历史草稿摘要',
      content: '历史草稿正文',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
    })
    assignRouteState(route, {
      path: '/knowledge/articles/88/edit',
      params: { id: '88' },
      query: {},
    })
    createKnowledgeArticle.mockResolvedValue({
      id: 188,
      title: '支付回调失败排查手册',
      summary: '更新后的摘要',
      content: '更新后的正文',
      categoryId: 3,
      status: 0,
    })

    const wrapper = await mountKnowledgeArticleEditorView()

    expect(wrapper.find('input[type="text"]').element).toHaveProperty('value', '历史草稿标题')

    await wrapper.find('input[type="text"]').setValue('支付回调失败排查手册')
    await wrapper.find('textarea').setValue('更新后的摘要')
    await wrapper.find('.editor-textarea').setValue('更新后的正文，已经同步到真实知识库。')
    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).toHaveBeenCalledTimes(1)
    expect(createKnowledgeArticle).toHaveBeenCalledWith({
      title: '支付回调失败排查手册',
      summary: '更新后的摘要',
      content: '更新后的正文，已经同步到真实知识库。',
      categoryId: 3,
      sourceTicketId: null,
      status: 0,
    })
    expect(updateKnowledgeArticle).not.toHaveBeenCalled()
    expect(listKnowledgeDrafts()).toHaveLength(0)
    expect(push).toHaveBeenCalledWith('/knowledge/articles/188')
  })

  it('blocks loading and saving for read-only users', async () => {
    canManageState.value = false

    const wrapper = await mountKnowledgeArticleEditorView()

    expect(fetchKnowledgeArticleDetail).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前账号只有阅读权限，不能新建或编辑知识文章。')

    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('当前账号只有阅读权限，不能保存知识文章。')
  })

  it('requires title and content before saving', async () => {
    const wrapper = await mountKnowledgeArticleEditorView()

    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('标题和正文至少要先写一点内容。')
  })

  it('applies ticket-close draft seeds into the form and preview context', async () => {
    saveKnowledgeDraftSeed({
      title: '支付回调失败处理复盘',
      summary: '从关单流程带出的摘要。',
      content: '一、问题现象\n支付回调失败。\n\n七、关闭备注\n业务已确认恢复。',
      categoryId: 3,
      sourceTicketId: 701,
      sourceTicketNo: 'TK-701',
      sourceTicketTitle: '支付回调失败',
      origin: 'ticket-close',
      closeRemark: '业务已确认恢复。',
      ticketStatus: '已关闭',
    })
    assignRouteState(route, {
      path: '/knowledge/articles/create',
      params: {},
      query: { from: 'ticket-close' },
    })

    const wrapper = await mountKnowledgeArticleEditorView()

    expect((wrapper.find('input[type="text"]').element as HTMLInputElement).value).toBe('支付回调失败处理复盘')
    expect(wrapper.text()).toContain('工单 TK-701 已关闭，已经帮你带出沉淀草稿和关闭备注。')
    expect(wrapper.text()).toContain('来源工单：TK-701 · 支付回调失败')
    expect(consumeKnowledgeDraftSeed()).toBeNull()
  })

  it('creates and publishes a real article when the backend is available', async () => {
    createKnowledgeArticle.mockResolvedValue({
      id: 501,
      title: '支付回调失败排查手册',
      summary: '用于指导支持人员快速定位支付回调失败问题。',
      content: '先检查签名配置，再检查回调地址连通性和幂等控制键，最后对照日志链路逐步排查。',
      categoryId: 3,
      status: 1,
    })

    const wrapper = await mountKnowledgeArticleEditorView()
    await fillRequiredFields(wrapper)
    await wrapper.findAll('button').find((item) => item.text().includes('发布文章'))!.trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).toHaveBeenCalledWith({
      title: '支付回调失败排查手册',
      summary: '',
      content: '先检查签名配置，再检查回调地址连通性和幂等控制键，最后对照日志链路逐步排查。',
      categoryId: 2,
      sourceTicketId: null,
      status: 1,
    })
    expect(push).toHaveBeenCalledWith('/knowledge/articles/501')
  })

  it('uses demo-mode local fallback immediately without calling remote create', async () => {
    isDemoMode.mockReturnValue(true)
    const wrapper = await mountKnowledgeArticleEditorView()
    await fillRequiredFields(wrapper)

    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).not.toHaveBeenCalled()
    expect(listKnowledgeDrafts()).toHaveLength(1)
    expect(wrapper.text()).toContain('后端不可用，草稿已保存到本地。')
  })

  it('prevents duplicate saves while the first knowledge save is still in flight', async () => {
    let resolveCreate: ((value: { id: number; title: string; summary: string; content: string; categoryId: number; status: number }) => void) | null = null
    createKnowledgeArticle.mockImplementation(() => new Promise((resolve) => {
      resolveCreate = resolve as typeof resolveCreate
    }))

    const wrapper = await mountKnowledgeArticleEditorView()
    await fillRequiredFields(wrapper)

    await wrapper.find('button.ghost-button').trigger('click')
    await wrapper.find('button.ghost-button').trigger('click')
    await flushPromises()

    expect(createKnowledgeArticle).toHaveBeenCalledTimes(1)

    resolveCreate?.({
      id: 902,
      title: '支付回调失败排查手册',
      summary: '',
      content: '先检查签名配置，再检查回调地址连通性和幂等控制键，最后对照日志链路逐步排查。',
      categoryId: 2,
      status: 0,
    })
    await flushPromises()

    expect(push).toHaveBeenCalledWith('/knowledge/articles/902')
  })
})
