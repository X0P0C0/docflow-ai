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
    expect(runtimeMode.getRuntimeModeHeadline()).toBe('当前正在使用演示会话')
  })

  it('treats demo tokens as demo mode', async () => {
    const runtimeMode = await loadRuntimeMode({
      token: 'demo-token:support01',
      isDemoToken: true,
    })

    expect(runtimeMode.isDemoMode()).toBe(true)
    expect(runtimeMode.getRuntimeModeText()).toBe('演示模式')
    expect(runtimeMode.getRuntimeEntryMessage('登录入口')).toContain('演示模式')
  })

  it('treats live tokens as live mode', async () => {
    const runtimeMode = await loadRuntimeMode({
      token: 'live-token-123',
      isDemoToken: false,
    })

    expect(runtimeMode.isDemoMode()).toBe(false)
    expect(runtimeMode.getRuntimeModeText()).toBe('真实模式')
    expect(runtimeMode.getRuntimeModeHeadline()).toBe('当前正在使用真实后端会话')
    expect(runtimeMode.getRuntimeEntryMessage('顶部导航')).toContain('真实模式')
  })

  it('describes fallback and local-only data sources consistently', async () => {
    const runtimeMode = await loadRuntimeMode({
      token: 'live-token-123',
      isDemoToken: false,
    })

    expect(runtimeMode.getRuntimeDataSourceMessage({
      usedFallbackData: true,
      subject: '工单列表',
    })).toContain('已回退到兜底数据')

    expect(runtimeMode.getRuntimeDataSourceMessage({
      usedFallbackData: false,
      subject: '知识库',
      localOnlyLabel: '知识草稿',
    })).toContain('本地知识草稿会作为补充一起显示')
  })
})
