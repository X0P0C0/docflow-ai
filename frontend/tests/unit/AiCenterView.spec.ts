import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AiCenterView from '../../src/views/AiCenterView.vue'
import { authState } from '../../src/auth'
import { getSafeLocalStorage } from '../../src/utils/safeStorage'

const {
  fetchAiWorkspace,
  fetchAiReplyDraft,
  adoptAiReplyDraft,
  unadoptAiReplyDraft,
  createTicketKnowledgeDraft,
  fetchTicketDetail,
  getRuntimeModeText,
  getRuntimeModeHeadline,
  getRuntimeDataSourceMessage,
  isDemoMode,
  push,
} = vi.hoisted(() => ({
  fetchAiWorkspace: vi.fn(),
  fetchAiReplyDraft: vi.fn(),
  adoptAiReplyDraft: vi.fn(),
  unadoptAiReplyDraft: vi.fn(),
  createTicketKnowledgeDraft: vi.fn(),
  fetchTicketDetail: vi.fn(),
  getRuntimeModeText: vi.fn(),
  getRuntimeModeHeadline: vi.fn(),
  getRuntimeDataSourceMessage: vi.fn(),
  isDemoMode: vi.fn(),
  push: vi.fn(),
}))

vi.mock('../../src/api/ai', () => ({
  fetchAiWorkspace,
  fetchAiReplyDraft,
  adoptAiReplyDraft,
  unadoptAiReplyDraft,
}))

vi.mock('../../src/api/ticket', () => ({
  createTicketKnowledgeDraft,
  fetchTicketDetail,
}))

vi.mock('../../src/utils/runtimeMode', () => ({
  getRuntimeModeText,
  getRuntimeModeHeadline,
  getRuntimeDataSourceMessage,
  isDemoMode,
}))

vi.mock('../../src/authz', () => ({
  canManageKnowledgeArticles: () => true,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push,
  }),
  RouterLink: defineComponent({
    name: 'RouterLink',
    props: {
      to: {
        type: [String, Object],
        required: true,
      },
    },
    setup(props, { slots }) {
      return () => h('a', {
        class: 'router-link-stub',
        'data-to': typeof props.to === 'string' ? props.to : JSON.stringify(props.to),
      }, slots.default?.())
    },
  }),
}))

describe('AiCenterView', () => {
  beforeEach(() => {
    fetchAiWorkspace.mockReset()
    fetchAiReplyDraft.mockReset()
    adoptAiReplyDraft.mockReset()
    unadoptAiReplyDraft.mockReset()
    createTicketKnowledgeDraft.mockReset()
    fetchTicketDetail.mockReset()
    getRuntimeModeText.mockReset()
    getRuntimeModeHeadline.mockReset()
    getRuntimeDataSourceMessage.mockReset()
    isDemoMode.mockReset()
    push.mockReset()
    vi.spyOn(console, 'error').mockImplementation(() => {})
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    })
    getSafeLocalStorage().clear()
    authState.user = {
      id: 11,
      username: 'support11',
      nickname: 'Operator Li',
      realName: 'Operator Li',
      email: '',
      phone: '',
      avatar: '',
      roles: ['SUPPORT'],
      permissions: [],
      capabilities: [],
    }

    getRuntimeModeText.mockReturnValue('real mode')
    getRuntimeModeHeadline.mockReturnValue('live backend session')
    getRuntimeDataSourceMessage.mockImplementation(({ subject }: { subject?: string }) => `runtime:${subject ?? ''}`)
    isDemoMode.mockReturnValue(false)
  })

  afterEach(() => {
    authState.user = null
    vi.restoreAllMocks()
  })

  async function mountView() {
    const wrapper = mount(AiCenterView, {
      global: {
        stubs: {
          AppShell: {
            template: '<div class="app-shell-stub"><slot /></div>',
          },
        },
      },
    })
    await flushPromises()
    return wrapper
  }

  it('loads the live workspace and exposes ticket-focus entry points for the selected draft', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 4,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 3,
      },
      primarySuggestion: {
        ticketId: 21,
        statusKey: 'needs-reply',
        adopted: true,
        adoptedByUserId: 8,
        adoptedByName: 'Support Wang',
        adoptedAt: '2026-05-19T16:12:00',
        ticketNo: 'INC-20260519-0021',
        title: 'Payment callback failed',
        summary: 'Confirm scope before sending the next customer update.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Confirm scope', 'Set next update window'],
      },
      recommendations: [
        {
          articleId: 88,
          title: 'Payment callback playbook',
          reason: 'Closest reusable path for this ticket.',
          matchRate: '92% match',
        },
      ],
      feed: [
        { title: 'Pending reply suggestions', value: '4' },
      ],
      followups: [
        {
          ticketId: 21,
          queueKey: 'active-reply',
          statusKey: 'needs-reply',
          adopted: true,
          adoptedByUserId: 8,
          adoptedByName: 'Support Wang',
          adoptedAt: '2026-05-19T16:12:00',
          title: 'Payment callback follow-up',
          desc: 'Still needs the first external reply.',
          chip: 'Needs reply',
          chipClass: 'chip-orange',
        },
      ],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 21,
      statusKey: 'reply-in-progress',
      adopted: true,
      adoptedByUserId: 8,
      adoptedByName: 'Support Wang',
      adoptedAt: '2026-05-19T16:12:00',
      ticketNo: 'INC-20260519-0021',
      ticketTitle: 'Payment callback failed',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The current draft is based on the latest ticket context.',
      nextStep: 'Verify the callback log range and send the next update.',
      customerReply: 'We are checking the affected callback path now.',
      operatorNotes: ['Keep the next update explicit.'],
      relatedKnowledge: [],
    })

    const wrapper = await mountView()

    expect(fetchAiWorkspace).toHaveBeenCalledTimes(1)
    expect(fetchAiReplyDraft).toHaveBeenCalledWith(21)
    expect(wrapper.text()).toContain('Adopted')
    expect(wrapper.text()).toContain('Adopt into comment')
    expect(wrapper.text()).toContain('Open comment stream')
    expect(wrapper.text()).toContain('Open timeline')
    expect(wrapper.text()).toContain('Open knowledge context')
    expect(wrapper.text()).toContain('Create knowledge draft')
    expect(wrapper.text()).toContain('Workspace snapshot')
    expect(wrapper.text()).toContain('Claimed by Support Wang')
    expect(wrapper.text()).toContain('Take over claim')
    expect(wrapper.text()).toContain('Support Wang is already working this reply draft')
    expect(wrapper.text()).toContain('Claimed By Others')
    expect(wrapper.findAll('[data-to*="fromKnowledge"]').length).toBeGreaterThan(0)
  })

  it('copies the current customer reply and shows confirmation feedback', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 21,
        statusKey: 'needs-reply',
        adopted: false,
        ticketNo: 'INC-20260519-0021',
        title: 'Payment callback failed',
        summary: 'Confirm scope before sending the next customer update.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Confirm scope'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 21,
      statusKey: 'reply-in-progress',
      adopted: false,
      ticketNo: 'INC-20260519-0021',
      ticketTitle: 'Payment callback failed',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The current draft is based on the latest ticket context.',
      nextStep: 'Verify the callback log range and send the next update.',
      customerReply: 'We are checking the affected callback path now.',
      operatorNotes: ['Keep the next update explicit.'],
      relatedKnowledge: [],
    })

    const wrapper = await mountView()
    const copyButton = wrapper.findAll('button').find((item) => item.text().includes('Copy customer reply'))

    await copyButton!.trigger('click')
    await flushPromises()

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('We are checking the affected callback path now.')
    expect(wrapper.text()).toContain('Copied the current customer reply')
    expect(wrapper.text()).toContain('Copied reply')
  })

  it('falls back to preview data when live workspace and draft loading both fail', async () => {
    fetchAiWorkspace.mockRejectedValue(Object.assign(new Error('service unavailable'), {
      status: 503,
      traceId: 'trace-ai-workspace-503',
    }))
    fetchAiReplyDraft.mockRejectedValue(Object.assign(new Error('service unavailable'), {
      status: 503,
      traceId: 'trace-ai-draft-503',
    }))

    const wrapper = await mountView()

    expect(fetchAiWorkspace).toHaveBeenCalledTimes(1)
    expect(fetchAiReplyDraft).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('Using fallback AI workspace preview')
    expect(wrapper.text()).toContain('Using fallback draft preview')
    expect(wrapper.text()).toContain('Fallback')
    expect(wrapper.text()).toContain('PREVIEW-001')
  })

  it('creates a live knowledge draft from the selected ticket and jumps into the editor', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'needs-knowledge',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for knowledge capture.',
        scene: 'incident / Resolved',
        confidence: 'High',
        checklist: ['Capture the reusable handling path'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 45,
          queueKey: 'knowledge-capture',
          statusKey: 'needs-knowledge',
          adopted: false,
          title: 'Resolved callback issue',
          desc: 'Turn this into a reusable knowledge draft.',
          chip: 'Needs knowledge',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'needs-knowledge',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / Resolved',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The current issue is already resolved.',
      nextStep: 'Turn the handling path into knowledge.',
      customerReply: 'The issue is resolved.',
      operatorNotes: ['Capture the final root cause and fix.'],
      relatedKnowledge: [],
    })
    createTicketKnowledgeDraft.mockResolvedValue({ id: 901 })

    const wrapper = await mountView()
    const actionButton = wrapper.findAll('button').find((item) => item.text().includes('Create knowledge draft'))

    await actionButton!.trigger('click')
    await flushPromises()

    expect(createTicketKnowledgeDraft).toHaveBeenCalledWith(45, { origin: 'manual' })
    expect(push).toHaveBeenCalledWith('/knowledge/articles/901/edit?from=ticket')
    expect(wrapper.text()).toContain('Created a real knowledge draft from the selected ticket.')
  })

  it('adopts the current reply draft into the ticket comment workflow through the shared workspace action', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    adoptAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      adopted: true,
      adoptedByUserId: 11,
      adoptedByName: 'Operator Li',
      adoptedAt: '2026-05-19T16:30:00',
    })

    const wrapper = await mountView()
    const adoptButton = wrapper.findAll('button').find((item) => item.text().includes('Adopt into comment'))

    await adoptButton!.trigger('click')
    await flushPromises()

    expect(adoptAiReplyDraft).toHaveBeenCalledWith(45)
    expect(wrapper.text()).toContain('Remove adopted mark')
    expect(wrapper.text()).toContain('Claimed by Operator Li')
    expect(push).toHaveBeenCalledWith({
      path: '/tickets/45',
      query: {
        fromAiCenter: '1',
        focus: 'comments',
        prefillReply: '1',
      },
    })
  })

  it('marks the reply draft as adopted in the shared workspace and updates the indicator', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    adoptAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      adopted: true,
      adoptedByUserId: 11,
      adoptedByName: 'Operator Li',
      adoptedAt: '2026-05-19T16:30:00',
    })

    const wrapper = await mountView()
    const markButton = wrapper.findAll('button').find((item) => item.text().includes('Mark adopted'))

    await markButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Adopted')
    expect(wrapper.text()).toContain('Operator Li')
    expect(wrapper.text()).toContain('1')
  })

  it('shows a takeover action when another operator already claimed the draft', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: true,
        adoptedByUserId: 8,
        adoptedByName: 'Support Wang',
        adoptedAt: '2026-05-19T16:20:00',
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [45],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: true,
      adoptedByUserId: 8,
      adoptedByName: 'Support Wang',
      adoptedAt: '2026-05-19T16:20:00',
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    adoptAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      adopted: true,
      adoptedByUserId: 11,
      adoptedByName: 'Operator Li',
      adoptedAt: '2026-05-19T16:30:00',
    })

    const wrapper = await mountView()
    const takeoverButton = wrapper.findAll('button').find((item) => item.text().includes('Take over claim'))

    expect(wrapper.text()).toContain('Support Wang is already working this reply draft')

    await takeoverButton!.trigger('click')
    await flushPromises()

    expect(adoptAiReplyDraft).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Take Over Claim')
    expect(wrapper.text()).toContain('Current owner')
    expect(wrapper.text()).toContain('Confirm takeover')

    const confirmButton = wrapper.findAll('button').find((item) => item.text().includes('Confirm takeover'))
    await confirmButton!.trigger('click')
    await flushPromises()

    expect(adoptAiReplyDraft).toHaveBeenCalledWith(45)
    expect(wrapper.text()).toContain('Reassigned this shared AI workspace claim from Support Wang to Operator Li.')
    expect(wrapper.text()).toContain('Remove adopted mark')
  })

  it('surfaces follow-up owner badges and takeover shortcuts for claimed drafts', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 2,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 46,
          queueKey: 'active-reply',
          statusKey: 'reply-in-progress',
          adopted: true,
          adoptedByUserId: 8,
          adoptedByName: 'Support Wang',
          adoptedAt: '2026-05-19T16:20:00',
          title: 'Claimed callback follow-up',
          desc: 'Already being handled by another operator.',
          chip: 'Adopted in workspace',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [46],
    })
    fetchAiReplyDraft
      .mockResolvedValueOnce({
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        ticketTitle: 'Resolved callback issue',
        scene: 'incident / In Progress',
        confidence: 'High',
        opener: 'Thanks for reporting the issue.',
        diagnosis: 'The issue is under active review.',
        nextStep: 'Verify the callback path.',
        customerReply: 'We are checking the callback path now and will share the next update shortly.',
        operatorNotes: ['Keep the wording concise.'],
        relatedKnowledge: [],
      })
      .mockResolvedValueOnce({
        ticketId: 46,
        statusKey: 'reply-in-progress',
        adopted: true,
        adoptedByUserId: 8,
        adoptedByName: 'Support Wang',
        adoptedAt: '2026-05-19T16:20:00',
        ticketNo: 'INC-20260519-0046',
        ticketTitle: 'Claimed callback follow-up',
        scene: 'incident / In Progress',
        confidence: 'High',
        opener: 'Thanks for reporting the issue.',
        diagnosis: 'Another operator already picked this up.',
        nextStep: 'Coordinate before sending the next reply.',
        customerReply: 'We are checking the callback path now and will share the next update shortly.',
        operatorNotes: ['Keep the wording concise.'],
        relatedKnowledge: [],
      })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Support Wang')
    expect(wrapper.text()).toContain('Load claimed draft')

    const takeoverButton = wrapper.findAll('button').find((item) => item.text().includes('Take over'))
    await takeoverButton!.trigger('click')
    await flushPromises()

    expect(fetchAiReplyDraft).toHaveBeenCalledWith(46)
    expect(wrapper.text()).toContain('Take Over Claim')
    expect(wrapper.text()).toContain('Claimed callback follow-up')
  })

  it('separates claimed counts and softens stale claims', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 2,
        adoptedSuggestions: 2,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: true,
        adoptedByUserId: 8,
        adoptedByName: 'Support Wang',
        adoptedAt: '2020-05-19T16:20:00',
        claimFreshness: 'stale',
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 45,
          queueKey: 'active-reply',
          statusKey: 'reply-in-progress',
          adopted: true,
          adoptedByUserId: 8,
          adoptedByName: 'Support Wang',
          adoptedAt: '2020-05-19T16:20:00',
          claimFreshness: 'stale',
          title: 'Stale claimed issue',
          desc: 'This claim is old and may no longer be active.',
          chip: 'Adopted in workspace',
          chipClass: 'chip-blue',
        },
        {
          ticketId: 46,
          queueKey: 'active-reply',
          statusKey: 'reply-in-progress',
          adopted: true,
          adoptedByUserId: 11,
          adoptedByName: 'Operator Li',
          adoptedAt: '2026-05-19T16:20:00',
          title: 'My claimed issue',
          desc: 'This draft belongs to the current operator.',
          chip: 'Adopted in workspace',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [45, 46],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: true,
      adoptedByUserId: 8,
      adoptedByName: 'Support Wang',
      adoptedAt: '2020-05-19T16:20:00',
      claimFreshness: 'stale',
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Claimed By Me')
    expect(wrapper.text()).toContain('Claimed By Others')
    expect(wrapper.text()).toContain('Claim may be stale')
    expect(wrapper.text()).toContain('last held this reply draft')
  })

  it('shows a neutral claim activity label when the backend freshness field is unavailable', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: true,
        adoptedByUserId: 8,
        adoptedByName: 'Support Wang',
        adoptedAt: '2026-05-19T16:20:00',
        ticketNo: 'INC-20260519-0045',
        title: 'Claimed issue with unknown freshness',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 45,
          queueKey: 'active-reply',
          statusKey: 'reply-in-progress',
          adopted: true,
          adoptedByUserId: 8,
          adoptedByName: 'Support Wang',
          adoptedAt: '2026-05-19T16:20:00',
          title: 'Claimed issue with unknown freshness',
          desc: 'The backend did not provide claim freshness for this record.',
          chip: 'Adopted in workspace',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [45],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: true,
      adoptedByUserId: 8,
      adoptedByName: 'Support Wang',
      adoptedAt: '2026-05-19T16:20:00',
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Claimed issue with unknown freshness',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Activity status unavailable')
    expect(wrapper.text()).not.toContain('Claim may be stale')
  })

  it('keeps the existing claim when takeover confirmation is canceled', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: true,
        adoptedByUserId: 8,
        adoptedByName: 'Support Wang',
        adoptedAt: '2026-05-19T16:20:00',
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [45],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: true,
      adoptedByUserId: 8,
      adoptedByName: 'Support Wang',
      adoptedAt: '2026-05-19T16:20:00',
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })

    const wrapper = await mountView()
    const takeoverButton = wrapper.findAll('button').find((item) => item.text().includes('Take over claim'))

    await takeoverButton!.trigger('click')
    await flushPromises()

    const keepButton = wrapper.findAll('button').find((item) => item.text().includes('Keep current claim'))
    await keepButton!.trigger('click')
    await flushPromises()

    expect(adoptAiReplyDraft).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Kept the current shared claim with Support Wang.')
    expect(wrapper.text()).toContain('Take over claim')
  })

  it('removes the shared adopted mark and clears the workspace adopted queue', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: true,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 45,
          queueKey: 'active-reply',
          statusKey: 'reply-in-progress',
          adopted: true,
          title: 'Resolved callback issue',
          desc: 'Already claimed in the shared workspace.',
          chip: 'Adopted in workspace',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [45],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: true,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    unadoptAiReplyDraft.mockResolvedValue({ ticketId: 45, adopted: false })

    const wrapper = await mountView()
    const unmarkButton = wrapper.findAll('button').find((item) => item.text().includes('Remove adopted mark'))

    await unmarkButton!.trigger('click')
    await flushPromises()

    expect(unadoptAiReplyDraft).toHaveBeenCalledWith(45)
    expect(wrapper.text()).toContain('Removed this reply draft from the shared AI workspace adopted queue.')
    expect(wrapper.text()).not.toContain('Workspace Adopted')
  })

  it('groups follow-ups by active reply, knowledge capture, and claimed ownership states', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 3,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 2,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'needs-reply',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Reply-needed issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 45,
          queueKey: 'active-reply',
          statusKey: 'needs-reply',
          adopted: false,
          title: 'Reply-needed issue',
          desc: 'Still needs the first external reply.',
          chip: 'Needs reply',
          chipClass: 'chip-orange',
        },
        {
          ticketId: 46,
          queueKey: 'knowledge-capture',
          statusKey: 'needs-knowledge',
          adopted: false,
          title: 'Knowledge candidate',
          desc: 'This one looks ready for capture.',
          chip: 'Needs knowledge',
          chipClass: 'chip-green',
        },
        {
          ticketId: null,
          queueKey: 'knowledge-capture',
          statusKey: 'needs-refresh',
          adopted: false,
          title: 'FAQ refresh',
          desc: 'This article needs a policy refresh.',
          chip: 'Needs refresh',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'needs-reply',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Reply-needed issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    adoptAiReplyDraft.mockResolvedValue({ ticketId: 45, adopted: true })

    const wrapper = await mountView()

    expect(wrapper.text()).toContain('Active Reply Queue')
    expect(wrapper.text()).toContain('Knowledge Capture Queue')
    expect(wrapper.text()).not.toContain('Claimed in workspace')
    expect(wrapper.text()).not.toContain('Claim may be stale')

    const markButton = wrapper.findAll('button').find((item) => item.text().includes('Mark adopted'))
    await markButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Claimed By Me')
    expect(wrapper.text()).toContain('Claimed in workspace')
  })

  it('falls back to a local adopted marker when the shared adoption action is unavailable', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    adoptAiReplyDraft.mockRejectedValue(Object.assign(new Error('service unavailable'), {
      status: 503,
      traceId: 'trace-ai-adopt-503',
    }))

    const wrapper = await mountView()
    const markButton = wrapper.findAll('button').find((item) => item.text().includes('Mark adopted'))

    await markButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('marked as adopted locally in this browser instead')
    expect(wrapper.text()).toContain('Remove adopted mark')
  })

  it('clears the local adopted marker when the shared unadopt action is unavailable', async () => {
    getSafeLocalStorage().setItem('docflow.ai.workspaceActions', JSON.stringify({
      adoptedTicketIds: [45],
    }))
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'reply-in-progress',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for operator follow-up.',
        scene: 'incident / In Progress',
        confidence: 'High',
        checklist: ['Review the reply draft'],
      },
      recommendations: [],
      feed: [],
      followups: [],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'reply-in-progress',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / In Progress',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The issue is under active review.',
      nextStep: 'Verify the callback path.',
      customerReply: 'We are checking the callback path now and will share the next update shortly.',
      operatorNotes: ['Keep the wording concise.'],
      relatedKnowledge: [],
    })
    unadoptAiReplyDraft.mockRejectedValue(Object.assign(new Error('service unavailable'), {
      status: 503,
      traceId: 'trace-ai-unadopt-503',
    }))

    const wrapper = await mountView()
    const unmarkButton = wrapper.findAll('button').find((item) => item.text().includes('Remove adopted mark'))

    await unmarkButton!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('local adopted marker was cleared instead')
    expect(wrapper.text()).toContain('Mark adopted')
  })

  it('falls back to a local knowledge draft seed when live draft creation fails with a network-like error', async () => {
    fetchAiWorkspace.mockResolvedValue({
      generatedAt: '2026-05-19T16:10:00',
      heuristicBased: true,
      overview: {
        pendingSuggestions: 1,
        adoptedSuggestions: 1,
        knowledgeRecommendations: 1,
      },
      primarySuggestion: {
        ticketId: 45,
        statusKey: 'needs-knowledge',
        adopted: false,
        ticketNo: 'INC-20260519-0045',
        title: 'Resolved callback issue',
        summary: 'Ready for knowledge capture.',
        scene: 'incident / Resolved',
        confidence: 'High',
        checklist: ['Capture the reusable handling path'],
      },
      recommendations: [],
      feed: [],
      followups: [
        {
          ticketId: 45,
          queueKey: 'knowledge-capture',
          statusKey: 'needs-knowledge',
          adopted: false,
          title: 'Resolved callback issue',
          desc: 'Turn this into a reusable knowledge draft.',
          chip: 'Needs knowledge',
          chipClass: 'chip-blue',
        },
      ],
      adoptedTicketIds: [],
    })
    fetchAiReplyDraft.mockResolvedValue({
      ticketId: 45,
      statusKey: 'needs-knowledge',
      adopted: false,
      ticketNo: 'INC-20260519-0045',
      ticketTitle: 'Resolved callback issue',
      scene: 'incident / Resolved',
      confidence: 'High',
      opener: 'Thanks for reporting the issue.',
      diagnosis: 'The current issue is already resolved.',
      nextStep: 'Turn the handling path into knowledge.',
      customerReply: 'The issue is resolved.',
      operatorNotes: ['Capture the final root cause and fix.'],
      relatedKnowledge: [],
    })
    createTicketKnowledgeDraft.mockRejectedValue(Object.assign(new Error('Bad Gateway'), {
      status: 503,
      traceId: 'trace-ai-kb-503',
    }))
    fetchTicketDetail.mockResolvedValue({
      id: 45,
      ticketNo: 'INC-20260519-0045',
      title: 'Resolved callback issue',
      content: 'Callback requests are stable after the fix.',
      type: 'INCIDENT',
      categoryId: 3,
      priority: 3,
      priorityLabel: 'High',
      status: 3,
      statusLabel: 'Resolved',
      submitUserId: 7,
      assigneeUserId: 101,
      submitterName: 'Wang',
      assigneeName: 'Li',
      linkedKnowledgeArticleCount: 0,
      latestLinkedKnowledgeArticle: null,
      createTime: '2026-05-19T15:00:00',
      updateTime: '2026-05-19T16:00:00',
      sourceKnowledgeArticles: [],
      timeline: [
        {
          id: 1,
          operatorName: 'Li',
          title: 'Resolved the callback issue',
          desc: 'Applied the callback retry fix.',
          createTime: '2026-05-19T15:50:00',
        },
      ],
      comments: [
        {
          id: 1,
          authorName: 'Li',
          content: 'The callback retry fix is verified.',
          commentTypeLabel: 'Handling Note',
          internal: false,
          createTime: '2026-05-19T15:55:00',
        },
      ],
      relatedArticles: [],
    })

    const wrapper = await mountView()
    const actionButton = wrapper.findAll('button').find((item) => item.text().includes('Create knowledge draft'))

    await actionButton!.trigger('click')
    await flushPromises()

    expect(createTicketKnowledgeDraft).toHaveBeenCalledTimes(1)
    expect(fetchTicketDetail).toHaveBeenCalledWith(45)
    expect(push).toHaveBeenCalledWith('/knowledge/articles/create?from=ticket')
    expect(wrapper.text()).toContain('A local draft seed has been prepared instead.')
  })
})
