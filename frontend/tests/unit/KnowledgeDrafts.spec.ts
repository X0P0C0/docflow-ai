import { beforeEach, describe, expect, it } from 'vitest'
import {
  getKnowledgeDraft,
  listKnowledgeDrafts,
  mergeKnowledgeArticles,
  removeKnowledgeDraft,
  saveKnowledgeDraft,
  updateKnowledgeDraftStatus,
} from '../../src/mock/knowledgeDrafts'

describe('knowledge draft storage helpers', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('saves drafts with local metadata and preserves source ticket links on update', () => {
    const first = saveKnowledgeDraft({
      id: 9101,
      title: '支付回调失败处理复盘',
      summary: '首版摘要',
      content: '首版内容',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
      sourceTicket: {
        id: 701,
        ticketNo: 'INC-701',
        title: '支付回调接口偶发超时',
      },
    })
    const updated = saveKnowledgeDraft({
      id: 9101,
      title: '支付回调失败处理复盘 v2',
      summary: '更新后的摘要',
      content: '更新后的内容',
      categoryId: 3,
      authorUserId: 7,
      status: 0,
      publishTime: null,
      sourceTicket: undefined,
    })

    expect(first.source).toBe('local')
    expect(updated.sourceTicketId).toBe(701)
    expect(updated.sourceTicket).toEqual({
      id: 701,
      ticketNo: 'INC-701',
      title: '支付回调接口偶发超时',
    })
    expect(getKnowledgeDraft(9101)?.title).toBe('支付回调失败处理复盘 v2')
  })

  it('lists drafts by newest update time and can update and remove them', async () => {
    saveKnowledgeDraft({
      id: 9101,
      title: '较早草稿',
      summary: 'a',
      content: 'a',
      categoryId: 1,
      authorUserId: 1,
      status: 0,
      publishTime: null,
      sourceTicket: undefined,
    })
    await new Promise((resolve) => setTimeout(resolve, 5))
    saveKnowledgeDraft({
      id: 9102,
      title: '较新草稿',
      summary: 'b',
      content: 'b',
      categoryId: 2,
      authorUserId: 2,
      status: 0,
      publishTime: null,
      sourceTicket: undefined,
    })

    expect(listKnowledgeDrafts().map((item) => item.id)).toEqual([9102, 9101])
    expect(updateKnowledgeDraftStatus(9101, 1)?.status).toBe(1)
    expect(updateKnowledgeDraftStatus(9999, 1)).toBeNull()

    removeKnowledgeDraft(9102)
    expect(getKnowledgeDraft(9102)).toBeNull()
  })

  it('merges local drafts ahead of remote articles with matching ids and sorts by publish or update time', () => {
    const local = saveKnowledgeDraft({
      id: 9101,
      title: '本地草稿',
      summary: 'local',
      content: 'local',
      categoryId: 3,
      authorUserId: 1,
      status: 0,
      publishTime: null,
      sourceTicket: undefined,
    })
    const merged = mergeKnowledgeArticles([
      {
        id: 9101,
        title: '远程同 ID 文章',
        summary: 'remote-duplicate',
        content: 'remote-duplicate',
        categoryId: 3,
        sourceTicketId: null,
        authorUserId: 1,
        status: 1,
        viewCount: 12,
        likeCount: 3,
        collectCount: 2,
        publishTime: '2026-05-14T09:00:00',
        createTime: '2026-05-14T09:00:00',
        updateTime: '2026-05-14T09:00:00',
      },
      {
        id: 9102,
        title: '远程新文章',
        summary: 'remote',
        content: 'remote',
        categoryId: 2,
        sourceTicketId: null,
        authorUserId: 2,
        status: 1,
        viewCount: 50,
        likeCount: 8,
        collectCount: 5,
        publishTime: '2099-05-14T10:00:00',
        createTime: '2099-05-14T10:00:00',
        updateTime: '2099-05-14T10:00:00',
      },
    ])

    expect(merged[0].id).toBe(9102)
    expect(merged.find((item) => item.id === 9101)?.title).toBe(local.title)
    expect(merged).toHaveLength(2)
  })
})
