import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('runtime mode helpers', () => {
  beforeEach(() => {
    vi.resetModules()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  async function loadRuntimeMode(options: { token: string; isDemoToken?: boolean }) {
    vi.doMock('../../src/auth', () => ({
      authState: {
        token: options.token,
      },
    }))
    vi.doMock('../../src/api/auth', () => ({
      isDemoToken: vi.fn(() => options.isDemoToken ?? false),
    }))

    return import('../../src/utils/runtimeMode')
  }

  it('treats missing tokens as demo mode', async () => {
    const runtimeMode = await loadRuntimeMode({
      token: '',
    })

    expect(runtimeMode.isDemoMode()).toBe(true)
    expect(runtimeMode.getRuntimeModeText()).toBe('演示模式')
  })

  it('treats demo tokens as demo mode', async () => {
    const runtimeMode = await loadRuntimeMode({
      token: 'demo-token:support01',
      isDemoToken: true,
    })

    expect(runtimeMode.isDemoMode()).toBe(true)
    expect(runtimeMode.getRuntimeModeText()).toBe('演示模式')
  })

  it('treats live tokens as normal mode', async () => {
    const runtimeMode = await loadRuntimeMode({
      token: 'live-token-123',
      isDemoToken: false,
    })

    expect(runtimeMode.isDemoMode()).toBe(false)
    expect(runtimeMode.getRuntimeModeText()).toBe('正常模式')
  })
})
