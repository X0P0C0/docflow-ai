import { flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getLocalTicket } from '../../src/mock/ticketWorkspace'
import { mountTicketCreateView } from './helpers/pageMounts'
import { resetWebStorage } from './helpers/testHarness'

const { replace, createTicket } = vi.hoisted(() => ({
  replace: vi.fn(),
  createTicket: vi.fn(),
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="typeof to === \'string\' ? to : to.path"><slot /></a>',
  },
  useRouter: () => ({
    replace,
  }),
}))

vi.mock('../../src/api/ticket', () => ({
  createTicket,
}))

describe('TicketCreateView', () => {
  beforeEach(() => {
    replace.mockReset()
    createTicket.mockReset()
    resetWebStorage()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows a required-field error before submitting when title is empty', async () => {
    const wrapper = await mountTicketCreateView()

    await wrapper.find('textarea').setValue('问题描述已经填写，但标题为空。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(createTicket).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请先填写工单标题')
  })

  it('redirects to the remote ticket detail after a successful submission', async () => {
    createTicket.mockResolvedValue({ id: 501 })

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')
    const selects = wrapper.findAll('select.field-control')

    await inputs[0].setValue('支付回调接口偶发超时')
    await selects[0].setValue('INCIDENT')
    await selects[1].setValue('1')
    await selects[2].setValue('4')
    await wrapper.find('textarea').setValue('用户支付成功后订单仍停留在待支付状态，需要排查回调链路。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(createTicket).toHaveBeenCalledWith({
      title: '支付回调接口偶发超时',
      type: 'INCIDENT',
      categoryId: 1,
      priority: 4,
      content: '用户支付成功后订单仍停留在待支付状态，需要排查回调链路。',
    })
    expect(replace).toHaveBeenCalledWith('/tickets/501?created=1')
  })

  it('keeps backend business errors visible on the form instead of falling back locally', async () => {
    createTicket.mockRejectedValue(Object.assign(new Error('标题长度超过限制'), {
      status: 422,
      traceId: 'trace-create-422',
    }))

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')

    await inputs[0].setValue('超长标题测试')
    await wrapper.find('textarea').setValue('这里是完整的问题描述。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(replace).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('标题长度超过限制')
    expect(wrapper.text()).toContain('trace-create-422')
  })

  it('falls back to a local ticket and redirects into the local workflow on network-like failures', async () => {
    createTicket.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-create-503',
    }))

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')

    await inputs[0].setValue('本地兜底工单')
    await wrapper.find('textarea').setValue('后端暂不可用时，先把排查问题写进本地工作流。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    const localMatch = replace.mock.calls
      .map(([value]) => String(value))
      .find((value) => value.includes('?localCreated=1'))
    expect(localMatch).toBeTruthy()
    const localId = Number(localMatch!.match(/\/tickets\/(\d+)\?localCreated=1/)?.[1])
    expect(localId).toBeGreaterThan(0)
    expect(getLocalTicket(localId)?.title).toBe('本地兜底工单')
    expect(getLocalTicket(localId)?.content).toBe('后端暂不可用时，先把排查问题写进本地工作流。')
  })

  it('shows a required-field error before submitting when content is empty', async () => {
    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')

    await inputs[0].setValue('只有标题，没有描述')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(createTicket).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请先填写问题描述')
  })

  it('trims submitted title and content values before creating a remote ticket', async () => {
    createTicket.mockResolvedValue({ id: 601 })

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')

    await inputs[0].setValue('  支付回调链路排查  ')
    await wrapper.find('textarea').setValue('  需要补充回调超时的上下文和复现步骤。  ')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(createTicket).toHaveBeenCalledWith({
      title: '支付回调链路排查',
      type: 'TASK',
      categoryId: 3,
      priority: 2,
      content: '需要补充回调超时的上下文和复现步骤。',
    })
    expect(replace).toHaveBeenCalledWith('/tickets/601?created=1')
  })

  it('resets the form and clears stale submit errors', async () => {
    createTicket.mockRejectedValue(Object.assign(new Error('标题长度超过限制'), {
      status: 422,
      traceId: 'trace-create-422',
    }))

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')
    const selects = wrapper.findAll('select.field-control')
    const textarea = wrapper.find('textarea')

    await inputs[0].setValue('会被清空的标题')
    await selects[0].setValue('INCIDENT')
    await selects[1].setValue('1')
    await selects[2].setValue('4')
    await textarea.setValue('会被清空的问题描述。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('标题长度超过限制')

    await wrapper.findAll('button').find((item) => item.text() === '清空表单')!.trigger('click')

    expect((inputs[0].element as HTMLInputElement).value).toBe('')
    expect((selects[0].element as HTMLSelectElement).value).toBe('TASK')
    expect((selects[1].element as HTMLSelectElement).value).toBe('3')
    expect((selects[2].element as HTMLSelectElement).value).toBe('2')
    expect((textarea.element as HTMLTextAreaElement).value).toBe('')
    expect(wrapper.text()).not.toContain('标题长度超过限制')
  })

  it('prevents duplicate submissions while the create request is still in flight', async () => {
    let resolveCreate: ((value: { id: number }) => void) | null = null
    createTicket.mockImplementation(() => new Promise((resolve) => {
      resolveCreate = resolve as (value: { id: number }) => void
    }))

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')

    await inputs[0].setValue('重复提交保护')
    await wrapper.find('textarea').setValue('这条工单用于验证提交中的重复触发不会再次发请求。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await flushPromises()

    expect(createTicket).toHaveBeenCalledTimes(1)

    resolveCreate?.({ id: 777 })
    await flushPromises()

    expect(replace).toHaveBeenCalledWith('/tickets/777?created=1')
  })

  it('prevents clearing the form while the create request is still in flight', async () => {
    let resolveCreate: ((value: { id: number }) => void) | null = null
    createTicket.mockImplementation(() => new Promise((resolve) => {
      resolveCreate = resolve as (value: { id: number }) => void
    }))

    const wrapper = await mountTicketCreateView()
    const inputs = wrapper.findAll('input.field-control')
    const textarea = wrapper.find('textarea')

    await inputs[0].setValue('提交中的表单')
    await textarea.setValue('提交过程中不应该还能把表单清空。')
    await wrapper.find('form.ticket-form').trigger('submit.prevent')
    await wrapper.findAll('button').find((item) => item.text() === '清空表单')!.trigger('click')
    await flushPromises()

    expect((inputs[0].element as HTMLInputElement).value).toBe('提交中的表单')
    expect((textarea.element as HTMLTextAreaElement).value).toBe('提交过程中不应该还能把表单清空。')
    expect((wrapper.findAll('button').find((item) => item.text() === '清空表单')!.element as HTMLButtonElement).disabled).toBe(true)

    resolveCreate?.({ id: 778 })
    await flushPromises()
  })
})
