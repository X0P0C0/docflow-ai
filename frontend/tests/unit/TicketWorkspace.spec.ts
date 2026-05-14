import { beforeEach, describe, expect, it } from 'vitest'
import { authState } from '../../src/auth'
import {
  appendLocalComment,
  assignLocalTicket,
  createLocalTicket,
  getLocalTicket,
  listLocalTickets,
  mergeTickets,
  seedFallbackLocals,
  updateLocalTicketStatus,
} from '../../src/mock/ticketWorkspace'

describe('local ticket workspace helpers', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    authState.restored = true
    authState.token = 'demo-token:support01'
    authState.user = {
      id: 2,
      username: 'support01',
      nickname: '李晓安',
      realName: '李晓安',
      email: 'support01@docflow.ai',
      phone: '13800000001',
      avatar: null,
      roles: ['SUPPORT'],
      permissions: ['TICKET_VIEW'],
      capabilities: ['TICKET_OPERATE'],
    }
  })

  it('creates local tickets with actor identity, local numbering, and default workflow metadata', () => {
    const created = createLocalTicket({
      title: '本地排查工单',
      content: '后端未就绪时继续记录排查动作。',
      type: 'TASK',
      categoryId: 2,
      priority: 4,
    })

    expect(created.id).toBeGreaterThan(10000)
    expect(created.ticketNo).toMatch(/^LOCAL-\d+$/)
    expect(created.submitUserId).toBe(2)
    expect(created.submitter).toBe('李晓安')
    expect(created.priorityLevel).toBe('P1')
    expect(created.priority).toBe('紧急')
    expect(created.assignee).toBe('待分配')
    expect(created.timeline?.[0].title).toBe('Ticket Created')
    expect(getLocalTicket(created.id)?.title).toBe('本地排查工单')
  })

  it('prepends comments, assignments, and status updates into the local workflow timeline', () => {
    const created = createLocalTicket({
      title: '本地协作工单',
      content: '需要继续协作处理。',
      type: 'INCIDENT',
      categoryId: 3,
      priority: 3,
    })
    const assigned = assignLocalTicket(created, '王晨', '', 9)
    const commented = appendLocalComment(assigned, {
      content: '已补充内部排查记录。',
      internal: true,
      commentType: 3,
    })
    const updated = updateLocalTicketStatus(commented, 3, '')

    expect(assigned.assignee).toBe('王晨')
    expect(assigned.assigneeUserId).toBe(9)
    expect(assigned.timeline?.[0].title).toBe('Ticket Assigned')
    expect(commented.comments?.[0]).toEqual({
      author: '李晓安',
      content: '已补充内部排查记录。',
      typeLabel: '内部备注',
      internal: true,
      createdAt: commented.comments?.[0].createdAt,
    })
    expect(commented.timeline?.[0].desc).toBe('新增了一条内部备注。')
    expect(updated.status).toBe('已解决')
    expect(updated.timeline?.[0].title).toBe('Status Updated')
    expect(updated.timeline?.[0].desc).toBe('工单状态已更新为已解决。')
  })

  it('merges local tickets ahead of remote tickets with matching ids and can seed fallback locals once', () => {
    const local = createLocalTicket({
      title: '本地优先工单',
      content: 'local',
      type: 'QUESTION',
      categoryId: 1,
      priority: 2,
    })
    const merged = mergeTickets([
      {
        id: local.id,
        ticketNo: 'REMOTE-SAME',
        title: '远程同 ID 工单',
        meta: 'remote',
        submitUserId: 1,
        assigneeUserId: null,
        content: 'remote',
        priorityLevel: 'P2',
        priority: '高优先级',
        status: '处理中',
        priorityClass: 'chip-orange',
        assignee: '远程处理人',
        submitter: '远程提交人',
        updatedAt: '2099-05-14 10:00:00',
        tags: ['故障事件', '分类 3'],
      },
      {
        id: 88,
        ticketNo: 'REMOTE-88',
        title: '远程新工单',
        meta: 'remote',
        submitUserId: 1,
        assigneeUserId: null,
        content: 'remote',
        priorityLevel: 'P3',
        priority: '普通',
        status: '新建',
        priorityClass: 'chip-green',
        assignee: '待分配',
        submitter: '远程提交人',
        updatedAt: '2099-05-14 10:00:00',
        tags: ['问题咨询', '分类 1'],
      },
    ])

    expect(merged.find((item) => item.id === local.id)?.title).toBe('本地优先工单')
    expect(merged.find((item) => item.id === 88)?.title).toBe('远程新工单')

    localStorage.clear()
    seedFallbackLocals()
    const firstSeedIds = listLocalTickets().map((item) => item.id)
    seedFallbackLocals()
    expect(listLocalTickets().map((item) => item.id)).toEqual(firstSeedIds)
  })

  it('falls back cleanly for broken storage, anonymous actors, default priority, ordinary comments, and unknown status values', () => {
    localStorage.setItem('docflow.ai.localTickets', '{broken-json')
    expect(listLocalTickets()).toEqual([])
    expect(localStorage.getItem('docflow.ai.localTickets')).toBeNull()

    authState.user = null

    const created = createLocalTicket({
      title: '匿名本地工单',
      content: 'anonymous',
      type: 'QUESTION',
      categoryId: 1,
      priority: 99,
    })
    const assigned = assignLocalTicket(created, '新处理人', '', undefined)
    const commented = appendLocalComment(assigned, {
      content: '补充一条外部协作评论。',
      internal: false,
      commentType: 99,
    })
    const updated = updateLocalTicketStatus(commented, 99, '手工备注优先')

    expect(created.submitUserId).toBeUndefined()
    expect(created.submitter).toBe('当前用户')
    expect(created.priorityLevel).toBe('P3')
    expect(created.priority).toBe('普通')
    expect(created.priorityClass).toBe('chip-green')
    expect(assigned.assigneeUserId).toBeNull()
    expect(assigned.timeline?.[0].desc).toBe('工单已指派给 新处理人。')
    expect(commented.comments?.[0].typeLabel).toBe('普通评论')
    expect(commented.timeline?.[0].desc).toBe('新增了一条协作评论。')
    expect(updated.status).toBe('新建')
    expect(updated.timeline?.[0].desc).toBe('手工备注优先')
  })

  it('keeps an existing assignee id when reassigned without an explicit new id', () => {
    const created = createLocalTicket({
      title: '保留 assigneeUserId',
      content: 'content',
      type: 'TASK',
      categoryId: 2,
      priority: 2,
    })
    const firstAssign = assignLocalTicket(created, '李一', '先指派', 15)
    const secondAssign = assignLocalTicket(firstAssign, '李二', '', undefined)

    expect(secondAssign.assigneeUserId).toBe(15)
    expect(secondAssign.assignee).toBe('李二')
    expect(secondAssign.timeline?.[0].desc).toBe('工单已指派给 李二。')
  })
})
