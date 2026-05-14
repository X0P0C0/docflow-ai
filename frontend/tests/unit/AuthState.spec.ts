import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('auth state helpers', () => {
  beforeEach(() => {
    vi.resetModules()
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function loadAuthModule(options?: {
    fetchCurrentUserImpl?: () => Promise<unknown>
    isDemoTokenImpl?: (token: string) => boolean
  }) {
    const fetchCurrentUser = vi.fn(options?.fetchCurrentUserImpl || (async () => ({
      id: 9,
      username: 'admin',
      nickname: '系统管理员',
      realName: '系统管理员',
      email: 'admin@docflow.ai',
      phone: '13800000000',
      avatar: null,
      roles: ['ADMIN'],
      permissions: ['KNOWLEDGE_VIEW'],
      capabilities: ['SYSTEM_MANAGE'],
    })))
    const isDemoToken = vi.fn(options?.isDemoTokenImpl || ((token: string) => token.startsWith('demo-token:')))

    vi.doMock('../../src/api/auth', () => ({
      fetchCurrentUser,
      isDemoToken,
    }))

    const authModule = await import('../../src/auth')
    return {
      ...authModule,
      fetchCurrentUser,
      isDemoToken,
    }
  }

  it('marks the session restored immediately when no token exists', async () => {
    const { authState, restoreSession, fetchCurrentUser } = await loadAuthModule()

    await restoreSession()

    expect(authState.restored).toBe(true)
    expect(authState.token).toBe('')
    expect(fetchCurrentUser).not.toHaveBeenCalled()
  })

  it('skips remote restore for demo tokens', async () => {
    localStorage.setItem('docflow.ai.token', 'demo-token:support01')

    const { authState, restoreSession, fetchCurrentUser, isDemoToken } = await loadAuthModule()

    await restoreSession()

    expect(isDemoToken).toHaveBeenCalledWith('demo-token:support01')
    expect(fetchCurrentUser).not.toHaveBeenCalled()
    expect(authState.restored).toBe(true)
  })

  it('hydrates the current user from the backend when restoring a real session', async () => {
    localStorage.setItem('docflow.ai.token', 'live-token-123')
    localStorage.setItem('docflow.ai.user', JSON.stringify({
      id: 2,
      username: 'stale-user',
      nickname: '旧用户',
      realName: '旧用户',
      email: 'old@docflow.ai',
      phone: '13800000002',
      avatar: null,
      roles: ['USER'],
      permissions: [],
      capabilities: [],
    }))

    const backendUser = {
      id: 1,
      username: 'admin',
      nickname: '系统管理员',
      realName: '系统管理员',
      email: 'admin@docflow.ai',
      phone: '13800000000',
      avatar: null,
      roles: ['ADMIN'],
      permissions: ['SYSTEM_VIEW'],
      capabilities: ['SYSTEM_MANAGE'],
    }
    const { authState, restoreSession, fetchCurrentUser } = await loadAuthModule({
      fetchCurrentUserImpl: async () => backendUser,
    })

    await restoreSession()

    expect(fetchCurrentUser).toHaveBeenCalledTimes(1)
    expect(authState.restored).toBe(true)
    expect(authState.user).toEqual(backendUser)
    expect(JSON.parse(localStorage.getItem('docflow.ai.user') || '{}')).toEqual(backendUser)
  })

  it('clears local session state when remote restore fails', async () => {
    localStorage.setItem('docflow.ai.token', 'live-token-123')
    localStorage.setItem('docflow.ai.user', JSON.stringify({ username: 'stale-user' }))

    const { authState, restoreSession } = await loadAuthModule({
      fetchCurrentUserImpl: async () => {
        throw new Error('Unauthorized')
      },
    })

    await restoreSession()

    expect(authState.restored).toBe(true)
    expect(authState.token).toBe('')
    expect(authState.user).toBeNull()
    expect(localStorage.getItem('docflow.ai.token')).toBeNull()
    expect(localStorage.getItem('docflow.ai.user')).toBeNull()
  })

  it('persists saveSession and clearSession changes to local storage', async () => {
    const { authState, saveSession, clearSession, isAuthenticated } = await loadAuthModule()
    const session = {
      token: 'live-token-456',
      expireSeconds: 3600,
      user: {
        id: 5,
        username: 'support01',
        nickname: '李晓安',
        realName: '李晓安',
        email: 'support01@docflow.ai',
        phone: '13800000001',
        avatar: null,
        roles: ['SUPPORT'],
        permissions: ['TICKET_VIEW'],
        capabilities: ['TICKET_OPERATE'],
      },
    }

    saveSession(session)

    expect(isAuthenticated()).toBe(true)
    expect(authState.token).toBe('live-token-456')
    expect(localStorage.getItem('docflow.ai.token')).toBe('live-token-456')
    expect(JSON.parse(localStorage.getItem('docflow.ai.user') || '{}')).toEqual(session.user)

    clearSession()

    expect(isAuthenticated()).toBe(false)
    expect(authState.user).toBeNull()
    expect(localStorage.getItem('docflow.ai.token')).toBeNull()
    expect(localStorage.getItem('docflow.ai.user')).toBeNull()
  })
})
