import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type AuthFailureHandler = ((error: { status?: number }) => void) | null
type NavigationGuard = ((to: {
  path: string
  fullPath: string
  meta: Record<string, unknown>
}) => unknown | Promise<unknown>) | null

describe('router guards', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function loadRouter(options?: {
    authenticated?: boolean
    restoreAuthenticated?: boolean
    canAccessCapability?: boolean
  }) {
    let authFailureHandler: AuthFailureHandler = null
    let navigationGuard: NavigationGuard = null
    let authenticated = options?.authenticated ?? false

    const replace = vi.fn()
    const clearSession = vi.fn(() => {
      authenticated = false
    })
    const restoreSession = vi.fn(async () => {
      if (typeof options?.restoreAuthenticated === 'boolean') {
        authenticated = options.restoreAuthenticated
      }
    })
    const isAuthenticated = vi.fn(() => authenticated)
    const canAccessCapability = vi.fn(() => options?.canAccessCapability ?? true)

    vi.doMock('vue-router', () => ({
      createRouter: () => ({
        currentRoute: {
          value: {
            path: '/tickets/101',
            fullPath: '/tickets/101',
            query: {},
          },
        },
        beforeEach: (guard: NavigationGuard) => {
          navigationGuard = guard
        },
        replace,
      }),
      createWebHistory: () => ({}),
    }))
    vi.doMock('../../src/access-policy', () => ({
      getRouteRequiredCapability: () => undefined,
    }))
    vi.doMock('../../src/api/http', () => ({
      setAuthFailureHandler: (handler: AuthFailureHandler) => {
        authFailureHandler = handler
      },
    }))
    vi.doMock('../../src/auth', () => ({
      clearSession,
      isAuthenticated,
      restoreSession,
    }))
    vi.doMock('../../src/authz', () => ({
      canAccessCapability,
    }))
    vi.doMock('../../src/capability-constants', () => ({
      CAPABILITY_CODES: {
        KNOWLEDGE_MANAGE: 'KNOWLEDGE_MANAGE',
        AI_CENTER_ACCESS: 'AI_CENTER_ACCESS',
        SYSTEM_MANAGE: 'SYSTEM_MANAGE',
      },
    }))

    const viewStub = { default: { name: 'ViewStub' } }
    vi.doMock('../../src/views/AiCenterView.vue', () => viewStub)
    vi.doMock('../../src/views/DashboardView.vue', () => viewStub)
    vi.doMock('../../src/views/KnowledgeArticleDetailView.vue', () => viewStub)
    vi.doMock('../../src/views/KnowledgeArticleEditorView.vue', () => viewStub)
    vi.doMock('../../src/views/KnowledgeArticleListView.vue', () => viewStub)
    vi.doMock('../../src/views/LoginView.vue', () => viewStub)
    vi.doMock('../../src/views/NotificationCenterView.vue', () => viewStub)
    vi.doMock('../../src/views/ProfileView.vue', () => viewStub)
    vi.doMock('../../src/views/SystemManageView.vue', () => viewStub)
    vi.doMock('../../src/views/TicketCreateView.vue', () => viewStub)
    vi.doMock('../../src/views/TicketDetailView.vue', () => viewStub)
    vi.doMock('../../src/views/TicketListView.vue', () => viewStub)

    await import('../../src/router')

    return {
      clearSession,
      restoreSession,
      isAuthenticated,
      canAccessCapability,
      replace,
      getAuthFailureHandler: () => authFailureHandler,
      getNavigationGuard: () => navigationGuard,
    }
  }

  it('redirects unauthenticated users to login and preserves the target path', async () => {
    const { getNavigationGuard, restoreSession } = await loadRouter({
      authenticated: false,
      restoreAuthenticated: false,
    })

    const result = await getNavigationGuard()!({
      path: '/tickets/create',
      fullPath: '/tickets/create',
      meta: { requiresAuth: true },
    })

    expect(restoreSession).toHaveBeenCalledTimes(1)
    expect(result).toEqual({
      path: '/login',
      query: {
        redirect: '/tickets/create',
      },
    })
  })

  it('allows protected navigation after session restore makes the user authenticated', async () => {
    const { getNavigationGuard, restoreSession } = await loadRouter({
      authenticated: false,
      restoreAuthenticated: true,
    })

    const result = await getNavigationGuard()!({
      path: '/tickets',
      fullPath: '/tickets',
      meta: { requiresAuth: true },
    })

    expect(restoreSession).toHaveBeenCalledTimes(1)
    expect(result).toBe(true)
  })

  it('redirects knowledge-manage routes to the knowledge list when capability is missing', async () => {
    const { getNavigationGuard, canAccessCapability } = await loadRouter({
      authenticated: true,
      canAccessCapability: false,
    })

    const result = await getNavigationGuard()!({
      path: '/knowledge/articles/create',
      fullPath: '/knowledge/articles/create',
      meta: {
        requiresAuth: true,
        requiredCapability: 'KNOWLEDGE_MANAGE',
      },
    })

    expect(canAccessCapability).toHaveBeenCalledWith('KNOWLEDGE_MANAGE')
    expect(result).toEqual({
      path: '/knowledge/articles',
      query: {
        reason: 'forbidden',
        from: '/knowledge/articles/create',
      },
    })
  })

  it('sends authenticated users away from guest-only routes', async () => {
    const { getNavigationGuard } = await loadRouter({
      authenticated: true,
    })

    const result = await getNavigationGuard()!({
      path: '/login',
      fullPath: '/login',
      meta: { guestOnly: true },
    })

    expect(result).toBe('/dashboard')
  })

  it('clears session and redirects through the auth failure handler on 401 responses', async () => {
    const { clearSession, replace, getAuthFailureHandler } = await loadRouter({
      authenticated: true,
    })

    const handler = getAuthFailureHandler()
    expect(handler).toBeTruthy()
    handler!({ status: 401 })

    expect(clearSession).toHaveBeenCalledTimes(1)
    expect(replace).toHaveBeenCalledWith({
      path: '/login',
      query: {
        redirect: '/tickets/101',
        reason: 'session-expired',
      },
    })
  })
})
