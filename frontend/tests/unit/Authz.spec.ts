import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { authState } from '../../src/auth'
import { CAPABILITY_CODES } from '../../src/capability-constants'
import {
  canAccessAiCenter,
  canAccessCapability,
  canAccessRoute,
  canAssignTickets,
  canManageKnowledgeArticles,
  canManageSystem,
  canOperateTickets,
  canTransitionTickets,
  canUseInternalTicketComments,
  canViewAllTickets,
  hasAnyPermission,
  hasAnyRole,
  hasCapability,
  hasPermission,
} from '../../src/authz'

describe('authz helpers', () => {
  beforeEach(() => {
    authState.token = ''
    authState.restored = true
    authState.user = null
  })

  afterEach(() => {
    authState.user = null
  })

  it('checks capabilities, permissions, and roles directly from the current user state', () => {
    authState.user = {
      id: 1,
      username: 'admin',
      nickname: '系统管理员',
      realName: '系统管理员',
      email: 'admin@docflow.ai',
      phone: '13800000000',
      avatar: null,
      roles: ['ADMIN'],
      permissions: ['SYSTEM_VIEW', 'KNOWLEDGE_VIEW'],
      capabilities: [CAPABILITY_CODES.SYSTEM_MANAGE],
    }

    expect(hasCapability(CAPABILITY_CODES.SYSTEM_MANAGE)).toBe(true)
    expect(hasPermission('SYSTEM_VIEW')).toBe(true)
    expect(hasAnyPermission(['NOPE', 'KNOWLEDGE_VIEW'])).toBe(true)
    expect(hasAnyRole(['SUPPORT', 'ADMIN'])).toBe(true)
  })

  it('unlocks knowledge management through fallback role and permission rules', () => {
    authState.user = {
      id: 2,
      username: 'support01',
      nickname: '支持小李',
      realName: '李晓安',
      email: 'support01@docflow.ai',
      phone: '13800000001',
      avatar: null,
      roles: ['SUPPORT'],
      permissions: ['KNOWLEDGE_ARTICLE_CREATE'],
      capabilities: [],
    }

    expect(canManageKnowledgeArticles()).toBe(true)
    expect(canAccessCapability(CAPABILITY_CODES.KNOWLEDGE_MANAGE)).toBe(true)
    expect(canAccessRoute('/knowledge/articles/create')).toBe(true)
  })

  it('treats ticket-operator privileges as enabling assign, transition, internal comments, and full-ticket view', () => {
    authState.user = {
      id: 3,
      username: 'support02',
      nickname: '支持小王',
      realName: '王晨',
      email: 'support02@docflow.ai',
      phone: '13800000002',
      avatar: null,
      roles: ['SUPPORT'],
      permissions: [],
      capabilities: [CAPABILITY_CODES.TICKET_OPERATE],
    }

    expect(canOperateTickets()).toBe(true)
    expect(canAssignTickets()).toBe(true)
    expect(canTransitionTickets()).toBe(true)
    expect(canUseInternalTicketComments()).toBe(true)
    expect(canViewAllTickets()).toBe(true)
    expect(canAccessCapability(CAPABILITY_CODES.TICKET_ASSIGN)).toBe(true)
  })

  it('allows AI center and system management through role- and capability-based access', () => {
    authState.user = {
      id: 4,
      username: 'admin',
      nickname: '系统管理员',
      realName: '系统管理员',
      email: 'admin@docflow.ai',
      phone: '13800000000',
      avatar: null,
      roles: ['ADMIN'],
      permissions: [],
      capabilities: [CAPABILITY_CODES.AI_CENTER_ACCESS],
    }

    expect(canAccessAiCenter()).toBe(true)
    expect(canManageSystem()).toBe(true)
    expect(canAccessRoute('/ai-center')).toBe(true)
    expect(canAccessRoute('/settings')).toBe(true)
  })

  it('rejects protected routes when no matching access path is available', () => {
    authState.user = {
      id: 5,
      username: 'user01',
      nickname: '业务小王',
      realName: '王晨',
      email: 'user01@docflow.ai',
      phone: '13800000003',
      avatar: null,
      roles: ['USER'],
      permissions: ['TICKET_VIEW'],
      capabilities: [],
    }

    expect(canManageKnowledgeArticles()).toBe(false)
    expect(canAccessAiCenter()).toBe(false)
    expect(canManageSystem()).toBe(false)
    expect(canAccessRoute('/knowledge/articles/create')).toBe(false)
    expect(canAccessRoute('/ai-center')).toBe(false)
    expect(canAccessRoute('/settings')).toBe(false)
    expect(canAccessRoute('/tickets')).toBe(true)
  })
})
