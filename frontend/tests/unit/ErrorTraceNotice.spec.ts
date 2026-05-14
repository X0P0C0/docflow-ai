import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ErrorTraceNotice from '../../src/components/common/ErrorTraceNotice.vue'

describe('ErrorTraceNotice', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    vi.useRealTimers()
  })

  it('renders the warning wrapper and hides trace action when no traceId is provided', () => {
    const wrapper = mount(ErrorTraceNotice, {
      props: {
        message: '知识文章保存失败，请稍后重试。',
      },
    })

    expect(wrapper.classes()).toContain('state-box')
    expect(wrapper.classes()).toContain('state-warning')
    expect(wrapper.text()).toContain('知识文章保存失败，请稍后重试。')
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('renders inline mode and shows the trace copy action when traceId is provided', () => {
    const wrapper = mount(ErrorTraceNotice, {
      props: {
        message: '评论提交失败',
        traceId: 'trace-123',
        inline: true,
      },
    })

    expect(wrapper.classes()).toContain('error-trace-inline')
    expect(wrapper.classes()).not.toContain('state-box')
    expect(wrapper.get('button').text()).toContain('trace-123')
  })

  it('copies the traceId and updates the button label temporarily', async () => {
    vi.useFakeTimers()
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
    })

    const wrapper = mount(ErrorTraceNotice, {
      props: {
        message: '工单详情加载失败，请稍后重试。',
        traceId: 'trace-copy-ok',
      },
    })

    await wrapper.get('button').trigger('click')
    await wrapper.vm.$nextTick()

    expect(writeText).toHaveBeenCalledWith('trace-copy-ok')
    expect(wrapper.get('button').text()).toBe('已复制追踪号')

    vi.advanceTimersByTime(1800)
    await wrapper.vm.$nextTick()

    expect(wrapper.get('button').text()).toContain('trace-copy-ok')
  })
})
