import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authState } from '../../src/auth'
import { mountLoginView } from './helpers/pageMounts'
import { assignRouteState, resetWebStorage } from './helpers/testHarness'

const { replace, login, createDemoSession, isDemoMode, getRuntimeModeText } = vi.hoisted(() => ({
  replace: vi.fn(),
  login: vi.fn(),
  createDemoSession: vi.fn(),
  isDemoMode: vi.fn(),
  getRuntimeModeText: vi.fn(),
}))

const route = {
  query: {},
  fullPath: '/login',
}

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({
    replace,
  }),
}))

vi.mock('../../src/api/auth', () => ({
  login,
  createDemoSession,
}))

vi.mock('../../src/utils/runtimeMode', () => ({
  isDemoMode,
  getRuntimeModeText,
}))

describe('LoginView', () => {
  beforeEach(() => {
    replace.mockReset()
    login.mockReset()
    createDemoSession.mockReset()
    isDemoMode.mockReset()
    getRuntimeModeText.mockReset()
    resetWebStorage()
    authState.token = ''
    authState.user = null
    authState.restored = false
    assignRouteState(route, {
      query: {},
      fullPath: '/login',
    })
    isDemoMode.mockReturnValue(true)
    getRuntimeModeText.mockReturnValue('演示模式')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the session-expired notice from the auth reason on the login route', async () => {
    assignRouteState(route, {
      query: { reason: 'session-expired' },
      fullPath: '/login?reason=session-expired',
    })

    const wrapper = await mountLoginView()

    expect(wrapper.text()).toContain('登录状态已失效，请重新登录后继续。')
  })

  it('clears the route-driven auth notice when the reason disappears', async () => {
    assignRouteState(route, {
      query: { reason: 'session-expired' },
      fullPath: '/login?reason=session-expired',
    })

    const wrapper = await mountLoginView()
    expect(wrapper.text()).toContain('登录状态已失效，请重新登录后继续。')

    assignRouteState(route, {
      query: {},
      fullPath: '/login',
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('登录状态已失效，请重新登录后继续。')
  })

  it('falls back to a demo session on network-like login failures and preserves the redirect target', async () => {
    assignRouteState(route, {
      query: { redirect: '/tickets/101' },
      fullPath: '/login?redirect=%2Ftickets%2F101',
    })
    login.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-login-503',
    }))
    createDemoSession.mockReturnValue({
      token: 'demo-token:support01',
      expireSeconds: 86400,
      user: {
        id: 2,
        username: 'support01',
        nickname: '支持小李',
        realName: '李晓安',
        email: 'support01@docflow.ai',
        phone: '13800000001',
        avatar: null,
        roles: ['SUPPORT'],
        permissions: [],
        capabilities: [],
      },
    })

    const wrapper = await mountLoginView()
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('support01')
    await inputs[1].setValue('password')
    await wrapper.find('form.login-form').trigger('submit.prevent')
    await flushPromises()

    expect(login).toHaveBeenCalledWith({
      username: 'support01',
      password: 'password',
    })
    expect(createDemoSession).toHaveBeenCalledWith('support01', 'password')
    expect(authState.token).toBe('demo-token:support01')
    expect(authState.user?.username).toBe('support01')
    expect(replace).toHaveBeenCalledWith('/tickets/101')
  })

  it('shows a validation message when username or password is missing', async () => {
    const wrapper = await mountLoginView()
    const inputs = wrapper.findAll('input')

    await inputs[0].setValue('')
    await inputs[1].setValue('')
    await wrapper.find('form.login-form').trigger('submit.prevent')
    await flushPromises()

    expect(login).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请输入用户名和密码。')
  })

  it('uses the real backend login result and falls back to /dashboard when no redirect is provided', async () => {
    login.mockResolvedValue({
      token: 'live-token-123',
      expireSeconds: 7200,
      user: {
        id: 1,
        username: 'admin',
        nickname: '系统管理员',
        realName: '系统管理员',
        email: 'admin@docflow.ai',
        phone: '13800000000',
        avatar: null,
        roles: ['ADMIN'],
        permissions: [],
        capabilities: [],
      },
    })
    isDemoMode.mockReturnValue(false)
    getRuntimeModeText.mockReturnValue('正常模式')

    const wrapper = await mountLoginView()
    await wrapper.find('form.login-form').trigger('submit.prevent')
    await flushPromises()

    expect(authState.token).toBe('live-token-123')
    expect(authState.user?.username).toBe('admin')
    expect(wrapper.text()).toContain('当前默认会进入正常模式')
    expect(replace).toHaveBeenCalledWith('/dashboard')
  })

  it('falls back to /dashboard when the redirect target is not an in-app path', async () => {
    assignRouteState(route, {
      query: { redirect: 'https://evil.example/phish' },
      fullPath: '/login?redirect=https%3A%2F%2Fevil.example%2Fphish',
    })
    login.mockResolvedValue({
      token: 'live-token-redirect',
      expireSeconds: 7200,
      user: {
        id: 1,
        username: 'admin',
        nickname: '系统管理员',
        realName: '系统管理员',
        email: 'admin@docflow.ai',
        phone: '13800000000',
        avatar: null,
        roles: ['ADMIN'],
        permissions: [],
        capabilities: [],
      },
    })

    const wrapper = await mountLoginView()
    await wrapper.find('form.login-form').trigger('submit.prevent')
    await flushPromises()

    expect(replace).toHaveBeenCalledWith('/dashboard')
  })

  it('keeps business login errors visible instead of falling back to demo mode', async () => {
    login.mockRejectedValue(Object.assign(new Error('用户名或密码错误'), {
      status: 401,
      traceId: 'trace-login-401',
    }))
    createDemoSession.mockReturnValue({
      token: 'demo-token:admin',
      expireSeconds: 86400,
      user: {
        id: 1,
        username: 'admin',
        nickname: '系统管理员',
        realName: '系统管理员',
        email: 'admin@docflow.ai',
        phone: '13800000000',
        avatar: null,
        roles: ['ADMIN'],
        permissions: [],
        capabilities: [],
      },
    })

    const wrapper = await mountLoginView()
    await wrapper.find('form.login-form').trigger('submit.prevent')
    await flushPromises()

    expect(createDemoSession).toHaveBeenCalled()
    expect(authState.token).toBe('')
    expect(replace).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('用户名或密码错误')
  })

  it('fills demo accounts and clears stale error state when a demo shortcut is clicked', async () => {
    assignRouteState(route, {
      query: { reason: 'session-expired' },
      fullPath: '/login?reason=session-expired',
    })

    const wrapper = await mountLoginView()
    const demoButtons = wrapper.findAll('button.login-demo-account')

    expect(wrapper.text()).toContain('登录状态已失效，请重新登录后继续。')

    await demoButtons[1].trigger('click')

    const inputs = wrapper.findAll('input')
    expect((inputs[0].element as HTMLInputElement).value).toBe('support01')
    expect((inputs[1].element as HTMLInputElement).value).toBe('password')
    expect(wrapper.text()).not.toContain('登录状态已失效，请重新登录后继续。')
  })

  it('prevents duplicate login submissions while the first request is still in flight', async () => {
    let resolveLogin: ((value: {
      token: string
      expireSeconds: number
      user: {
        id: number
        username: string
        nickname: string
        realName: string
        email: string
        phone: string
        avatar: null
        roles: string[]
        permissions: string[]
        capabilities: string[]
      }
    }) => void) | null = null
    login.mockImplementation(() => new Promise((resolve) => {
      resolveLogin = resolve as typeof resolveLogin
    }))

    const wrapper = await mountLoginView()

    await wrapper.find('form.login-form').trigger('submit.prevent')
    await wrapper.find('form.login-form').trigger('submit.prevent')
    await flushPromises()

    expect(login).toHaveBeenCalledTimes(1)

    resolveLogin?.({
      token: 'live-token-once',
      expireSeconds: 7200,
      user: {
        id: 1,
        username: 'admin',
        nickname: '系统管理员',
        realName: '系统管理员',
        email: 'admin@docflow.ai',
        phone: '13800000000',
        avatar: null,
        roles: ['ADMIN'],
        permissions: [],
        capabilities: [],
      },
    })
    await flushPromises()

    expect(replace).toHaveBeenCalledWith('/dashboard')
  })

  it('prevents demo-account switches while the login request is still in flight', async () => {
    let resolveLogin: ((value: {
      token: string
      expireSeconds: number
      user: {
        id: number
        username: string
        nickname: string
        realName: string
        email: string
        phone: string
        avatar: null
        roles: string[]
        permissions: string[]
        capabilities: string[]
      }
    }) => void) | null = null
    login.mockImplementation(() => new Promise((resolve) => {
      resolveLogin = resolve as typeof resolveLogin
    }))

    const wrapper = await mountLoginView()
    const demoButtons = wrapper.findAll('button.login-demo-account')

    await wrapper.find('form.login-form').trigger('submit.prevent')
    await demoButtons[1].trigger('click')
    await flushPromises()

    const inputs = wrapper.findAll('input')
    expect((inputs[0].element as HTMLInputElement).value).toBe('admin')
    expect((inputs[1].element as HTMLInputElement).value).toBe('password')
    expect((demoButtons[1].element as HTMLButtonElement).disabled).toBe(true)

    resolveLogin?.({
      token: 'live-token',
      expireSeconds: 7200,
      user: {
        id: 1,
        username: 'admin',
        nickname: '系统管理员',
        realName: '系统管理员',
        email: 'admin@docflow.ai',
        phone: '13800000000',
        avatar: null,
        roles: ['ADMIN'],
        permissions: [],
        capabilities: [],
      },
    })
    await flushPromises()
  })
})
