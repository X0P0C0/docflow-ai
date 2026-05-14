import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('api wrappers', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function loadApiModules() {
    const get = vi.fn(async (url: string) => ({ url }))
    const post = vi.fn(async (url: string, body?: unknown, options?: unknown) => ({ url, body, options }))
    const put = vi.fn(async (url: string, body?: unknown) => ({ url, body }))
    const del = vi.fn(async (url: string) => ({ url }))

    vi.doMock('../../src/api/http', () => ({
      get,
      post,
      put,
      del,
    }))

    const authApi = await import('../../src/api/auth')
    const knowledgeApi = await import('../../src/api/knowledge')
    const ticketApi = await import('../../src/api/ticket')

    return {
      authApi,
      knowledgeApi,
      ticketApi,
      get,
      post,
      put,
      del,
    }
  }

  it('forwards auth endpoints to the expected HTTP wrappers and resolves demo sessions', async () => {
    const { authApi, get, post } = await loadApiModules()

    await authApi.login({ username: 'support01', password: 'password' })
    await authApi.fetchCurrentUser()

    expect(post).toHaveBeenCalledWith(
      '/api/auth/login',
      { username: 'support01', password: 'password' },
      { skipAuth: true },
    )
    expect(get).toHaveBeenCalledWith('/api/auth/me')
    expect(authApi.createDemoSession('support01', 'password')?.token).toBe('demo-token:support01')
    expect(authApi.createDemoSession('support01', 'wrong')).toBeNull()
    expect(authApi.createDemoSession('missing', 'password')).toBeNull()
    expect(authApi.isDemoToken('demo-token:admin')).toBe(true)
    expect(authApi.isDemoToken('live-token')).toBe(false)
  })

  it('builds knowledge API URLs and mutation endpoints correctly', async () => {
    const { knowledgeApi, get, post, put, del } = await loadApiModules()

    await knowledgeApi.fetchKnowledgeArticles({
      keyword: '支付',
      categoryId: 3,
      status: 1,
      sourceTicketId: 701,
      sourceTicketNo: 'INC-701',
    })
    await knowledgeApi.fetchKnowledgeArticles()
    await knowledgeApi.fetchKnowledgeArticleDetail(88)
    await knowledgeApi.fetchKnowledgeArticleSourceTicketCounts([701, 702])
    await knowledgeApi.fetchKnowledgeArticleSourceTicketCounts([])
    await knowledgeApi.createKnowledgeArticle({
      title: '知识文章',
      summary: 'summary',
      content: 'content',
      categoryId: 3,
      sourceTicketId: 701,
      status: 0,
    })
    await knowledgeApi.updateKnowledgeArticle(88, {
      title: '知识文章',
      summary: 'summary',
      content: 'content',
      categoryId: 3,
      sourceTicketId: 701,
      status: 1,
    })
    await knowledgeApi.restoreKnowledgeArticleVersion(88, 3)
    await knowledgeApi.archiveKnowledgeArticle(88)
    await knowledgeApi.deleteKnowledgeArticle(88)

    expect(get).toHaveBeenCalledWith('/api/knowledge/articles?keyword=%E6%94%AF%E4%BB%98&categoryId=3&status=1&sourceTicketId=701&sourceTicketNo=INC-701')
    expect(get).toHaveBeenCalledWith('/api/knowledge/articles')
    expect(get).toHaveBeenCalledWith('/api/knowledge/articles/88')
    expect(get).toHaveBeenCalledWith('/api/knowledge/articles/source-ticket-counts?ticketIds=701&ticketIds=702')
    expect(get).toHaveBeenCalledWith('/api/knowledge/articles/source-ticket-counts')
    expect(post).toHaveBeenCalledWith('/api/knowledge/articles', expect.any(Object))
    expect(put).toHaveBeenCalledWith('/api/knowledge/articles/88', expect.any(Object))
    expect(post).toHaveBeenCalledWith('/api/knowledge/articles/88/versions/3/restore')
    expect(post).toHaveBeenCalledWith('/api/knowledge/articles/88/archive')
    expect(del).toHaveBeenCalledWith('/api/knowledge/articles/88')
  })

  it('builds ticket API list filters and mutation endpoints correctly', async () => {
    const { ticketApi, get, post } = await loadApiModules()

    await ticketApi.fetchTickets({
      keyword: '支付',
      status: 2,
      priority: 4,
      type: 'INCIDENT',
      assigneeUserId: 8,
    })
    await ticketApi.fetchTickets({
      status: 0,
      priority: 0,
      assigneeUserId: 0,
    })
    await ticketApi.fetchTicketDetail(701)
    await ticketApi.fetchTicketAssignees()
    await ticketApi.addTicketComment(701, { content: '处理说明', commentType: 2, internal: true })
    await ticketApi.updateTicketStatus(701, { status: 3, remark: '已解决' })
    await ticketApi.assignTicket(701, { assigneeUserId: 8, remark: '请跟进' })
    await ticketApi.createTicket({
      title: '新工单',
      content: 'content',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    await ticketApi.createTicketKnowledgeDraft(701, { origin: 'ticket-close', closeRemark: '已完成' })
    await ticketApi.createTicketKnowledgeDraft(702)

    expect(get).toHaveBeenCalledWith('/api/tickets?keyword=%E6%94%AF%E4%BB%98&status=2&priority=4&type=INCIDENT&assigneeUserId=8')
    expect(get).toHaveBeenCalledWith('/api/tickets')
    expect(get).toHaveBeenCalledWith('/api/tickets/701')
    expect(get).toHaveBeenCalledWith('/api/tickets/assignees')
    expect(post).toHaveBeenCalledWith('/api/tickets/701/comments', { content: '处理说明', commentType: 2, internal: true })
    expect(post).toHaveBeenCalledWith('/api/tickets/701/status', { status: 3, remark: '已解决' })
    expect(post).toHaveBeenCalledWith('/api/tickets/701/assignee', { assigneeUserId: 8, remark: '请跟进' })
    expect(post).toHaveBeenCalledWith('/api/tickets', {
      title: '新工单',
      content: 'content',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    expect(post).toHaveBeenCalledWith('/api/tickets/701/knowledge-draft', { origin: 'ticket-close', closeRemark: '已完成' })
    expect(post).toHaveBeenCalledWith('/api/tickets/702/knowledge-draft', {})
  })
})
