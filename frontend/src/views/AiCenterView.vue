<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
    <section class="ai-page workspace-page">
      <section class="panel ai-header-card workspace-card">
        <div class="ai-header workspace-header">
          <div class="ai-header__main workspace-header-main">
            <div class="ai-header__title workspace-title">
              <span class="page-eyebrow">AI Center</span>
              <h2>AI Collaboration Workspace</h2>
              <p>
                Bring reply drafting, knowledge reuse, and follow-up planning into one
                operator workspace so the team can act on real backend context instead
                of only reviewing a static shell.
              </p>
            </div>
            <div class="ai-header__actions workspace-actions">
              <RouterLink class="ghost-button ai-link-button" to="/tickets">Review queue</RouterLink>
            </div>
          </div>

          <div class="ai-stats workspace-stats">
            <article
              v-for="stat in overviewStats"
              :key="stat.label"
              class="ai-stat workspace-stat"
            >
              <span class="ai-stat__label workspace-stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.description }}</p>
            </article>
          </div>
        </div>
      </section>

      <section class="ai-main-grid">
        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>Reply Suggestion</h3>
              <p>
                Start from one live suggestion card backed by tickets, comments, and
                knowledge links, then decide whether to refine it before sending.
              </p>
            </div>
            <span class="chip" :class="workspaceStatusChipClass">{{ workspaceStatusChip }}</span>
          </div>

          <div v-if="aiLoading || aiErrorMessage" class="state-box ai-state-box" :class="{ 'state-warning': aiLoading || usedFallbackData }">
            <strong>{{ aiLoading ? 'Loading live AI workspace' : 'Using fallback AI workspace preview' }}</strong>
            <p>{{ aiLoading ? 'Pulling reply suggestions and knowledge recommendations from the real backend.' : aiErrorMessage }}</p>
            <p v-if="aiErrorTraceId" class="ai-state-trace">Trace ID: {{ aiErrorTraceId }}</p>
          </div>

          <div class="ai-highlight-card workspace-highlight-card">
            <strong>{{ replySuggestion.title }}</strong>
            <p>{{ replySuggestion.summary }}</p>
            <p v-if="replySuggestionOwnershipText" class="ai-adoption-meta">{{ replySuggestionOwnershipText }}</p>
            <div class="ai-highlight-meta workspace-highlight-meta">
              <span>Scene: {{ replySuggestion.scene }}</span>
              <span>Confidence: {{ replySuggestion.confidence }}</span>
            </div>
            <div class="ai-inline-actions">
              <button
                class="ghost-button ai-link-button"
                type="button"
                :disabled="!replySuggestion.ticketId || draftLoading"
                @click="selectTicket(replySuggestion.ticketId)"
              >
                {{ draftLoading && selectedTicketId === replySuggestion.ticketId ? 'Loading draft...' : 'Open draft' }}
              </button>
              <RouterLink
                v-if="replySuggestion.ticketId"
                class="ghost-button ai-link-button"
                :to="`/tickets/${replySuggestion.ticketId}`"
              >
                Open ticket
              </RouterLink>
              <RouterLink
                v-if="replySuggestion.ticketId"
                class="ghost-button ai-link-button"
                :to="buildTicketFocusRoute(replySuggestion.ticketId, 'comments')"
              >
                Reply context
              </RouterLink>
            </div>
          </div>

          <div class="mini-list">
            <div
              v-for="item in replyChecklist"
              :key="item"
              class="mini-item"
            >
              <span>{{ item }}</span>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>Reply Draft Detail</h3>
              <p>
                Pull a fuller operator draft for the selected ticket so the team can move
                from suggestion to action without leaving the workspace.
              </p>
            </div>
            <span class="chip" :class="draftStatusChipClass">{{ draftStatusChip }}</span>
          </div>

          <div v-if="draftLoading || draftErrorMessage" class="state-box ai-state-box" :class="{ 'state-warning': draftLoading || draftUsedFallback }">
            <strong>{{ draftLoading ? 'Loading live reply draft' : 'Using fallback draft preview' }}</strong>
            <p>{{ draftLoading ? 'Generating a richer operator draft for the selected ticket.' : draftErrorMessage }}</p>
            <p v-if="draftErrorTraceId" class="ai-state-trace">Trace ID: {{ draftErrorTraceId }}</p>
          </div>

          <div class="mini-list ai-draft-list">
            <div class="mini-item ai-draft-block">
              <strong>{{ activeDraft.ticketNo }} 路 {{ activeDraft.ticketTitle }}</strong>
              <p>{{ activeDraft.scene }} 路 {{ activeDraft.confidence }}</p>
              <p v-if="activeDraftOwnershipText" class="ai-adoption-meta">{{ activeDraftOwnershipText }}</p>
            </div>
            <div class="mini-item ai-draft-block">
              <strong>开场语</strong>
              <p>{{ activeDraft.opener }}</p>
            </div>
            <div class="mini-item ai-draft-block">
              <strong>诊断分析</strong>
              <p>{{ activeDraft.diagnosis }}</p>
            </div>
            <div class="mini-item ai-draft-block">
              <strong>Next Step</strong>
              <p>{{ activeDraft.nextStep }}</p>
            </div>
            <div class="mini-item ai-draft-block">
              <strong>Customer Reply</strong>
              <p>{{ activeDraft.customerReply }}</p>
            </div>
          </div>

          <div class="ai-draft-notes">
            <strong>Operator Notes</strong>
            <div class="mini-list">
              <div
                v-for="note in activeDraft.operatorNotes"
                :key="note"
                class="mini-item"
              >
                <span>{{ note }}</span>
              </div>
            </div>
            <div v-if="selectedTicketId" class="ai-inline-actions">
              <RouterLink
                class="ghost-button ai-link-button"
                :to="buildTicketFocusRoute(selectedTicketId, 'comments')"
              >
                Open comment stream
              </RouterLink>
              <button
                class="ghost-button ai-link-button"
                type="button"
                :disabled="!activeDraft.customerReply"
                @click="copyCustomerReply"
              >
                {{ copySucceeded ? 'Copied reply' : 'Copy customer reply' }}
              </button>
              <button
                class="primary-button ai-link-button"
                type="button"
                :disabled="!selectedTicketId"
                @click="adoptReplyDraftIntoTicket"
              >
                Adopt into comment
              </button>
              <RouterLink
                class="ghost-button ai-link-button"
                :to="buildTicketFocusRoute(selectedTicketId, 'timeline')"
              >
                Open timeline
              </RouterLink>
              <RouterLink
                v-if="displayedKnowledgeRecommendations.length"
                class="ghost-button ai-link-button"
                :to="buildTicketFocusRoute(selectedTicketId, 'knowledge')"
              >
                Open knowledge context
              </RouterLink>
              <button
                v-if="canManageKnowledge"
                class="primary-button ai-link-button"
                type="button"
                :disabled="!canGenerateKnowledgeDraft"
                @click="createKnowledgeDraftFromAiCenter"
              >
                {{ knowledgeDraftSubmitting ? 'Creating draft...' : 'Create knowledge draft' }}
              </button>
              <button
                class="ghost-button ai-link-button"
                type="button"
                :disabled="!selectedTicketId || replyActionSubmitting"
                @click="toggleReplyDraftAdoption"
              >
                {{ adoptionActionLabel }}
              </button>
            </div>
            <p class="ai-action-hint">{{ knowledgeActionHint }}</p>
            <div
              v-if="replyActionMessage"
              class="state-box ai-state-box"
            >
              <strong>Reply action updated</strong>
              <p>{{ replyActionMessage }}</p>
            </div>
            <div
              v-if="knowledgeDraftMessage"
              class="state-box ai-state-box"
              :class="{ 'state-warning': !knowledgeDraftTraceId && knowledgeDraftMessage.includes('temporarily unavailable') }"
            >
              <strong>{{ knowledgeDraftTraceId ? 'Knowledge draft action needs attention' : 'Knowledge draft action updated' }}</strong>
              <p>{{ knowledgeDraftMessage }}</p>
              <p v-if="knowledgeDraftTraceId" class="ai-state-trace">Trace ID: {{ knowledgeDraftTraceId }}</p>
            </div>
          </div>
        </article>
      </section>

      <section class="ai-secondary-grid">
        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>Knowledge Recommendations</h3>
              <p>
                Surface the closest reusable articles so operators can respond with
                context instead of searching manually.
              </p>
            </div>
            <RouterLink class="ghost-button ai-link-button" to="/knowledge/articles">Open library</RouterLink>
          </div>

          <div class="mini-list">
            <div
              v-for="item in displayedKnowledgeRecommendations"
              :key="`${item.articleId ?? 'preview'}-${item.title}`"
              class="mini-item ai-recommendation-item"
            >
              <div class="ai-recommendation-copy workspace-link-main">
                <strong>{{ item.title }}</strong>
                <p>{{ item.reason }}</p>
              </div>
              <div class="ai-recommendation-actions">
                <span class="chip chip-blue">{{ item.matchRate }}</span>
                <RouterLink
                  v-if="selectedTicketId"
                  class="ghost-button ai-link-button"
                  :to="buildTicketFocusRoute(selectedTicketId, 'knowledge')"
                >
                  Ticket context
                </RouterLink>
                <RouterLink
                  v-if="item.articleId"
                  class="ghost-button ai-link-button"
                  :to="`/knowledge/articles/${item.articleId}`"
                >
                  Open
                </RouterLink>
              </div>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>Follow-ups</h3>
              <p>
                Expose the tickets that still need either a first reply or a knowledge
                handoff.
              </p>
            </div>
          </div>
          <div class="ai-followup-groups">
            <section
              v-for="group in followupGroups"
              :key="group.key"
              class="ai-followup-group"
            >
              <div class="ai-followup-group-head">
                <div>
                  <strong>{{ group.title }}</strong>
                  <p>{{ group.description }}</p>
                </div>
                <span class="chip" :class="group.chipClass">{{ group.items.length }}</span>
              </div>
              <div class="mini-list">
                <div
                  v-for="item in group.items"
                  :key="`${item.ticketId ?? 'static'}-${item.title}`"
                  class="mini-item ai-followup-item"
                  :class="{
                    'ai-followup-item--claimed': isFollowupClaimedByAnotherUser(item),
                    'ai-followup-item--mine': isFollowupClaimedByCurrentUser(item),
                  }"
                >
                  <div>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.desc }}</p>
                    <p v-if="formatAdoptionText(item.adoptedByName, item.adoptedAt, item.claimFreshness, item.lastActivityAt)" class="ai-adoption-meta">
                      {{ formatAdoptionText(item.adoptedByName, item.adoptedAt, item.claimFreshness, item.lastActivityAt) }}
                    </p>
                  </div>
                  <div class="ai-followup-actions">
                    <span
                      v-if="isFollowupClaimedByAnotherUser(item)"
                      class="chip ai-owner-chip"
                      :class="{ 'ai-owner-chip--stale': isClaimStale(item.claimFreshness, item.lastActivityAt, item.adoptedAt) }"
                    >
                      {{ item.adoptedByName }}
                    </span>
                    <span
                      v-if="item.adoptedByName && (item.lastActivityAt || item.adoptedAt)"
                      class="chip"
                      :class="isClaimStale(item.claimFreshness, item.lastActivityAt, item.adoptedAt) ? 'chip-orange' : 'chip-blue'"
                    >
                      {{ getClaimFreshnessLabel(item.claimFreshness, item.lastActivityAt, item.adoptedAt) }}
                    </span>
                    <span class="chip" :class="item.chipClass">{{ item.chip }}</span>
                    <button
                      v-if="item.ticketId"
                      class="ghost-button ai-link-button"
                      type="button"
                      @click="selectTicket(item.ticketId)"
                    >
                      {{ isFollowupClaimedByAnotherUser(item) ? 'Load claimed draft' : 'Load draft' }}
                    </button>
                    <button
                      v-if="item.ticketId && isFollowupClaimedByAnotherUser(item)"
                      class="primary-button ai-link-button"
                      type="button"
                      @click="openFollowupTakeover(item.ticketId)"
                    >
                      Take over
                    </button>
                    <RouterLink
                      v-if="item.ticketId"
                      class="ghost-button ai-link-button"
                      :to="buildTicketFocusRoute(item.ticketId, 'comments')"
                    >
                      Open context
                    </RouterLink>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </article>
      </section>

      <section class="ai-footer-grid">
        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>Today&apos;s Feed</h3>
              <p>
                Keep the AI workspace tied to real backend counts instead of static demo
                totals.
              </p>
            </div>
          </div>
          <div class="mini-list">
            <div
              v-for="item in suggestionFeed"
              :key="item.title"
              class="mini-item"
            >
              <span>{{ item.title }}</span>
              <span>{{ item.value }}</span>
            </div>
          </div>
          <div class="state-box ai-generated-at">
            <strong>Workspace snapshot</strong>
            <p>{{ generatedAtText }}</p>
          </div>
        </article>
      </section>

      <div
        v-if="showTakeoverConfirm"
        class="ai-modal-backdrop"
        @click.self="cancelTakeoverClaim"
      >
        <section class="ai-modal-card">
          <div class="panel-head ai-modal-head">
            <div>
              <h3>Take Over Claim</h3>
              <p>
                This draft is already being handled in the shared workspace. Review the
                current claim before reassigning it.
              </p>
            </div>
            <span class="chip chip-orange">已认领</span>
          </div>
          <div class="mini-list">
            <div class="mini-item">
              <span>Current owner</span>
              <strong>{{ activeDraft.adoptedByName || 'Unknown operator' }}</strong>
            </div>
            <div class="mini-item">
              <span>Claimed at</span>
              <strong>{{ takeoverClaimedAtText }}</strong>
            </div>
            <div class="mini-item">
              <span>Last activity</span>
              <strong>{{ takeoverLastActivityText }}</strong>
            </div>
            <div class="mini-item">
              <span>Next action</span>
              <strong>You will replace the current shared claim with {{ currentUserName }}.</strong>
            </div>
          </div>
          <p class="ai-action-hint">
            {{ isClaimStale(activeDraft.claimFreshness, activeDraft.lastActivityAt, activeDraft.adoptedAt)
              ? 'This claim looks older than the freshness window, so it may no longer reflect active ownership.'
              : 'Continue only if you need to pick up this reply from the current owner right now.' }}
          </p>
          <div class="ai-inline-actions ai-modal-actions">
            <button
              class="ghost-button ai-link-button"
              type="button"
              @click="cancelTakeoverClaim"
            >
              Keep current claim
            </button>
            <button
              class="primary-button ai-link-button"
              type="button"
              :disabled="replyActionSubmitting"
              @click="confirmTakeoverClaim"
            >
              {{ replyActionSubmitting ? 'Taking over claim...' : 'Confirm takeover' }}
            </button>
          </div>
        </section>
      </div>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  adoptAiReplyDraft,
  type AiFollowupItem,
  fetchAiReplyDraft,
  fetchAiWorkspace,
  type AiReplyDraftResponse,
  type AiWorkspaceResponse,
  unadoptAiReplyDraft,
} from '../api/ai'
import { isNetworkFallbackCandidate } from '../api/http'
import { createTicketKnowledgeDraft, fetchTicketDetail, type TicketDetailApiItem } from '../api/ticket'
import { authState } from '../auth'
import { canManageKnowledgeArticles } from '../authz'
import AppShell from '../components/layout/AppShell.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'
import type { TicketItem } from '../types/dashboard'
import { saveAiReplyDraftSeed } from '../utils/aiReplySeed'
import { listAdoptedAiReplyTicketIds, markAiReplyDraftAdopted, unmarkAiReplyDraftAdopted } from '../utils/aiWorkspaceActions'
import { buildKnowledgeDraftFromTicket, saveKnowledgeDraftSeed } from '../utils/knowledgeFromTicket'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

const router = useRouter()
const fallbackReplyDraft: AiReplyDraftResponse = {
  ticketId: 0,
  statusKey: 'needs-reply',
  adopted: false,
  adoptedByUserId: null,
  adoptedByName: '',
  adoptedAt: '',
  ticketNo: 'PREVIEW-001',
  ticketTitle: 'Payment callback anomaly',
  scene: 'payment callback / escalation',
  confidence: 'High',
  opener: 'Thanks for reporting the issue. We have started checking the callback chain and are aligning the next troubleshooting checkpoint now.',
  diagnosis: 'The current assessment focuses on the callback route, impact scope, and log range that should be confirmed before promising an ETA.',
  nextStep: 'Confirm the affected scope, verify callback logs, and share the next update window in the first external reply.',
  customerReply: 'Hello, we have received your report and our team is checking the affected callback path now. We will share the next update as soon as the current troubleshooting checkpoint is confirmed.',
  operatorNotes: [
    'Priority is high enough to require an explicit next-update promise',
    'No need to over-explain before the first scope confirmation is finished',
    'Link the nearest knowledge article once the response path is stable',
  ],
  relatedKnowledge: [
    {
      articleId: null,
      title: 'Payment callback troubleshooting playbook',
      reason: 'Closest reusable context for the preview draft.',
      matchRate: '92% match',
    },
  ],
}

const fallbackWorkspace: AiWorkspaceResponse = {
  generatedAt: '',
  heuristicBased: true,
  overview: {
    pendingSuggestions: 42,
    adoptedSuggestions: 26,
    knowledgeRecommendations: 18,
  },
  primarySuggestion: {
    ticketId: 1,
    statusKey: 'needs-reply',
    adopted: false,
    adoptedByUserId: null,
    adoptedByName: '',
    adoptedAt: '',
    ticketNo: 'PREVIEW-001',
    title: 'Payment callback anomaly first-reply draft',
    summary: 'Confirm payment time, order number, and callback log range before promising the next troubleshooting checkpoint.',
    scene: 'payment callback / escalation',
    confidence: 'High',
    checklist: [
      'Confirm impact scope, order number, and payment time first',
      'State the current troubleshooting checkpoint in the first reply',
      'Promise the next update window so the user does not need to re-chase',
    ],
  },
  recommendations: [
    {
      articleId: null,
      title: 'Payment callback troubleshooting playbook',
      reason: 'Covers the same callback chain, gateway logs, and compensation checks as the current incident.',
      matchRate: '92% match',
    },
    {
      articleId: null,
      title: 'Release incident communication template',
      reason: 'Useful when the ticket needs a clearer external update while investigation is still ongoing.',
      matchRate: '78% match',
    },
    {
      articleId: null,
      title: 'Permission sync issue SOP',
      reason: 'A reusable fallback recommendation when the current workspace has not returned stronger matches yet.',
      matchRate: '71% match',
    },
  ],
  feed: [
    { title: 'Pending reply suggestions', value: '14' },
    { title: 'Drafts already adopted', value: '9' },
    { title: 'New knowledge summaries', value: '11' },
    { title: 'Manually refined replies', value: '8' },
  ],
  followups: [
    {
      ticketId: 1,
      queueKey: 'knowledge-capture',
      statusKey: 'needs-knowledge',
      adopted: false,
      adoptedByUserId: null,
      adoptedByName: '',
      adoptedAt: '',
      title: 'Payment callback handling notes',
      desc: 'This route has appeared repeatedly and should be turned into a reusable article once the case is closed.',
      chip: 'Needs knowledge',
      chipClass: 'chip-orange',
    },
    {
      ticketId: 2,
      queueKey: 'active-reply',
      statusKey: 'reply-in-progress',
      adopted: false,
      adoptedByUserId: null,
      adoptedByName: '',
      adoptedAt: '',
      title: 'Release incident communication template',
      desc: 'The reply frame is stable enough to become a reusable branch-based response pattern.',
      chip: 'Needs polish',
      chipClass: 'chip-blue',
    },
    {
      ticketId: null,
      queueKey: 'knowledge-capture',
      statusKey: 'needs-refresh',
      adopted: false,
      adoptedByUserId: null,
      adoptedByName: '',
      adoptedAt: '',
      title: 'Permission issue FAQ refresh',
      desc: 'The article still matches often, but its content needs a fresher policy explanation.',
      chip: 'Needs refresh',
      chipClass: 'chip-green',
    },
  ],
  adoptedTicketIds: [],
}

const _runtimeModeText = computed(() => getRuntimeModeText())
const _runtimeHeadline = computed(() => getRuntimeModeHeadline())
const usedFallbackData = ref(false)
const aiLoading = ref(false)
const aiErrorMessage = ref('')
const aiErrorTraceId = ref('')
const draftLoading = ref(false)
const draftUsedFallback = ref(false)
const draftErrorMessage = ref('')
const draftErrorTraceId = ref('')
const knowledgeDraftSubmitting = ref(false)
const knowledgeDraftMessage = ref('')
const knowledgeDraftTraceId = ref('')
const replyActionSubmitting = ref(false)
const replyActionMessage = ref('')
const showTakeoverConfirm = ref(false)
const takeoverTargetTicketId = ref<number | null>(null)
const copySucceeded = ref(false)
const selectedTicketId = ref<number | null>(null)
const workspaceAdoptedTicketIds = ref<number[]>([])
const localAdoptedTicketIds = ref<number[]>(listAdoptedAiReplyTicketIds())
const aiWorkspace = ref<AiWorkspaceResponse>(fallbackWorkspace)
const activeDraft = ref<AiReplyDraftResponse>(fallbackReplyDraft)

const _runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: usedFallbackData.value,
  subject: 'AI workspace',
}))

const myClaimedCount = computed(() => followups.value.filter((item) => isFollowupClaimedByCurrentUser(item)).length)
const otherClaimedCount = computed(() => followups.value.filter((item) => isFollowupClaimedByAnotherUser(item)).length)
const overviewStats = computed(() => [
  {
    label: 'Reply Suggestions',
    value: String(aiWorkspace.value.overview.pendingSuggestions),
    description: 'Tickets currently queued for an AI-first reply draft',
  },
  {
    label: 'Claimed By Me',
    value: String(myClaimedCount.value),
    description: 'Shared AI drafts that are currently assigned to this operator',
  },
  {
    label: 'Claimed By Others',
    value: String(otherClaimedCount.value + adoptedDraftCount.value),
    description: 'Shared AI drafts already being handled elsewhere or only marked locally',
  },
  {
    label: 'Knowledge Links',
    value: String(aiWorkspace.value.overview.knowledgeRecommendations),
    description: 'Reusable knowledge assets that the workspace can currently surface',
  },
])

const replySuggestion = computed(() => aiWorkspace.value.primarySuggestion)
const replyChecklist = computed(() => aiWorkspace.value.primarySuggestion.checklist)
const suggestionFeed = computed(() => aiWorkspace.value.feed)
const followups = computed(() => aiWorkspace.value.followups)
const displayedKnowledgeRecommendations = computed(() => {
  if (activeDraft.value.relatedKnowledge.length) {
    return activeDraft.value.relatedKnowledge
  }
  return aiWorkspace.value.recommendations
})
const selectedFollowup = computed(() => followups.value.find((item) => item.ticketId === selectedTicketId.value) || null)
const canManageKnowledge = computed(() => canManageKnowledgeArticles())
const currentUserId = computed(() => authState.user?.id ?? null)
const currentUserName = computed(() => (
  authState.user?.nickname
  || authState.user?.realName
  || authState.user?.username
  || 'Current user'
))
const adoptedTicketIds = computed(() => Array.from(new Set([
  ...workspaceAdoptedTicketIds.value,
  ...localAdoptedTicketIds.value,
])))
const activeDraftOwnershipText = computed(() => formatAdoptionText(
  activeDraft.value.adoptedByName,
  activeDraft.value.adoptedAt,
  activeDraft.value.claimFreshness,
  activeDraft.value.lastActivityAt,
))
const replySuggestionOwnershipText = computed(() => formatAdoptionText(
  replySuggestion.value.adoptedByName,
  replySuggestion.value.adoptedAt,
  replySuggestion.value.claimFreshness,
  replySuggestion.value.lastActivityAt,
))
const takeoverClaimedAtText = computed(() => (
  activeDraft.value.adoptedAt
    ? new Date(activeDraft.value.adoptedAt).toLocaleString()
    : 'Time unavailable'
))
const takeoverLastActivityText = computed(() => (
  activeDraft.value.lastActivityAt
    ? new Date(activeDraft.value.lastActivityAt).toLocaleString()
    : 'No activity recorded yet'
))
const isReplyDraftAdopted = computed(() => {
  if (!selectedTicketId.value) {
    return false
  }
  return !!activeDraft.value.adopted || adoptedTicketIds.value.includes(selectedTicketId.value)
})
const isAdoptedByCurrentUser = computed(() => (
  !!activeDraft.value.adopted
  && isOwnedByCurrentUser(activeDraft.value.adoptedByUserId, activeDraft.value.adoptedByName)
))
const isAdoptedByAnotherUser = computed(() => (
  !!activeDraft.value.adopted
  && isOwnedByAnotherUser(activeDraft.value.adoptedByUserId, activeDraft.value.adoptedByName)
))
const adoptionActionLabel = computed(() => {
  if (replyActionSubmitting.value) {
    if (isAdoptedByCurrentUser.value) {
      return 'Removing adopted mark...'
    }
    if (isAdoptedByAnotherUser.value) {
      return 'Taking over claim...'
    }
    return 'Marking adopted...'
  }
  if (isAdoptedByCurrentUser.value) {
    return 'Remove adopted mark'
  }
  if (isAdoptedByAnotherUser.value) {
    return 'Take over claim'
  }
  if (isReplyDraftAdopted.value) {
    return 'Remove adopted mark'
  }
  return 'Mark adopted'
})
const followupGroups = computed(() => {
  const claimedByMe = [] as typeof followups.value
  const claimedByOthers = [] as typeof followups.value
  const knowledge = [] as typeof followups.value
  const active = [] as typeof followups.value

  for (const item of followups.value) {
    if (item.adopted || (item.ticketId && adoptedTicketIds.value.includes(item.ticketId))) {
      const claimedItem = {
        ...item,
        chip: isClaimStale(item.claimFreshness, item.lastActivityAt, item.adoptedAt) ? 'Claim may be stale' : 'Claimed in workspace',
        chipClass: isClaimStale(item.claimFreshness, item.lastActivityAt, item.adoptedAt) ? 'chip-orange' : 'chip-blue',
      }
      if (isFollowupClaimedByCurrentUser(item)) {
        claimedByMe.push(claimedItem)
      } else {
        claimedByOthers.push(claimedItem)
      }
      continue
    }

    if (item.queueKey === 'knowledge-capture' || item.statusKey === 'needs-knowledge' || item.statusKey === 'needs-refresh') {
      knowledge.push(item)
      continue
    }

    if (item.queueKey === 'stable') {
      knowledge.push(item)
      continue
    }

    active.push(item)
  }

  return [
    {
      key: 'active',
      title: 'Active Reply Queue',
      description: 'Tickets that still need an operator reply pass or draft review.',
      chipClass: 'chip-orange',
      items: active,
    },
    {
      key: 'knowledge',
      title: 'Knowledge Capture Queue',
      description: 'Tickets and articles that look ready for knowledge capture or refresh.',
      chipClass: 'chip-green',
      items: knowledge,
    },
    {
      key: 'claimed-by-me',
      title: 'Claimed By Me',
      description: 'Drafts this operator is actively carrying in the shared AI workspace.',
      chipClass: 'chip-blue',
      items: claimedByMe,
    },
    {
      key: 'claimed-by-others',
      title: 'Claimed By Others',
      description: 'Drafts already being handled by teammates, including claims that may need a freshness check.',
      chipClass: 'chip-orange',
      items: claimedByOthers,
    },
  ].filter((group) => group.items.length)
})

const workspaceStatusChip = computed(() => {
  if (aiLoading.value) {
    return 'Loading'
  }
  if (usedFallbackData.value) {
    return 'Fallback'
  }
  if (isReplyDraftAdopted.value) {
    return 'Adopted'
  }
  return aiWorkspace.value.heuristicBased ? 'Live Heuristic' : 'Live'
})

const workspaceStatusChipClass = computed(() => {
  if (aiLoading.value) {
    return 'chip-blue'
  }
  if (usedFallbackData.value) {
    return 'chip-orange'
  }
  if (isReplyDraftAdopted.value) {
    return 'chip-blue'
  }
  return 'chip-green'
})

const draftStatusChip = computed(() => {
  if (draftLoading.value) {
    return 'Loading Draft'
  }
  if (draftUsedFallback.value) {
    return 'Fallback Draft'
  }
  return selectedTicketId.value ? 'Live Draft' : 'Preview Draft'
})

const draftStatusChipClass = computed(() => {
  if (draftLoading.value) {
    return 'chip-blue'
  }
  if (draftUsedFallback.value) {
    return 'chip-orange'
  }
  return 'chip-green'
})

const generatedAtText = computed(() => {
  if (!aiWorkspace.value.generatedAt) {
    return usedFallbackData.value || isDemoMode()
      ? 'Showing the local preview workspace snapshot.'
      : 'Waiting for the first live workspace snapshot.'
  }

  return `Generated from the current backend workspace at ${new Date(aiWorkspace.value.generatedAt).toLocaleString()}.`
})

const adoptedDraftCount = computed(() => localAdoptedTicketIds.value.filter((ticketId) => !workspaceAdoptedTicketIds.value.includes(ticketId)).length)

const isKnowledgeReadyStatus = computed(() => {
  if (activeDraft.value.statusKey === 'needs-knowledge' || activeDraft.value.statusKey === 'knowledge-linked') {
    return true
  }
  const scene = activeDraft.value.scene.toLowerCase()
  return scene.includes('resolved') || scene.includes('closed')
})

const canGenerateKnowledgeDraft = computed(() => (
  !!selectedTicketId.value
  && canManageKnowledge.value
  && !knowledgeDraftSubmitting.value
  && (isKnowledgeReadyStatus.value || selectedFollowup.value?.chip.toLowerCase().includes('knowledge'))
))

const knowledgeActionHint = computed(() => {
  if (!selectedTicketId.value) {
    return 'Select a ticket first before turning it into a knowledge draft.'
  }
  if (isAdoptedByAnotherUser.value) {
    return isClaimStale(activeDraft.value.claimFreshness, activeDraft.value.lastActivityAt, activeDraft.value.adoptedAt)
      ? `${activeDraft.value.adoptedByName} last held this reply draft, but the claim may now be stale. You can double-check context and take it over if the work is no longer active.`
      : `${activeDraft.value.adoptedByName} is already working this reply draft in the shared workspace. You can respect the current claim, or take it over if you need to continue from here.`
  }
  if (!canManageKnowledge.value) {
    return 'The current account can review AI suggestions but cannot publish or manage knowledge drafts.'
  }
  if (canGenerateKnowledgeDraft.value) {
    return isReplyDraftAdopted.value
      ? 'This reply draft has already been marked as adopted in the shared workspace, and the ticket is also ready for knowledge capture.'
      : 'The selected ticket is ready for knowledge capture. You can jump straight into the editor from here.'
  }
  if (isReplyDraftAdopted.value) {
    return isAdoptedByCurrentUser.value
      ? 'You have already claimed this reply draft in the shared workspace. You can keep working here, remove the claim, or continue into knowledge capture later.'
      : 'This reply draft has already been marked as adopted in the shared workspace. You can still jump back into the ticket context or continue into knowledge capture later.'
  }
  return 'This ticket still looks like an active reply workflow. Move it to a resolved or closed state before generating a knowledge draft.'
})

function applyWorkspaceData(data: AiWorkspaceResponse) {
  aiWorkspace.value = data
  workspaceAdoptedTicketIds.value = data.adoptedTicketIds ?? []
}

function applyDraftData(data: AiReplyDraftResponse) {
  activeDraft.value = data
  selectedTicketId.value = data.ticketId || null
  copySucceeded.value = false
  replyActionSubmitting.value = false
}

function formatAdoptionText(adoptedByName?: string, adoptedAt?: string, claimFreshness?: string, lastActivityAt?: string) {
  if (!adoptedByName) {
    return ''
  }
  if (!adoptedAt) {
    return `Claimed by ${adoptedByName}.`
  }
  const prefix = isClaimStale(claimFreshness, lastActivityAt, adoptedAt) ? 'Claim was last held' : 'Claimed'
  const claimText = `${prefix} by ${adoptedByName} on ${new Date(adoptedAt).toLocaleString()}.`
  if (!lastActivityAt || lastActivityAt === adoptedAt) {
    return claimText
  }
  return `${claimText} Last activity ${new Date(lastActivityAt).toLocaleString()}.`
}

function isClaimStale(claimFreshness?: string, _lastActivityAt?: string, _adoptedAt?: string) {
  if (claimFreshness === 'stale') {
    return true
  }
  return false
}

function getClaimFreshnessLabel(claimFreshness?: string, _lastActivityAt?: string, _adoptedAt?: string) {
  if (claimFreshness === 'fresh') {
    return 'Recently active'
  }
  if (claimFreshness === 'stale') {
    return 'Claim may be stale'
  }
  return 'Activity status unavailable'
}

function isOwnedByCurrentUser(adoptedByUserId?: number | null, adoptedByName?: string) {
  return !!currentUserId.value
    ? adoptedByUserId === currentUserId.value
    : !!adoptedByName && adoptedByName === currentUserName.value
}

function isOwnedByAnotherUser(adoptedByUserId?: number | null, adoptedByName?: string) {
  if (!adoptedByName || adoptedByName === 'This browser') {
    return false
  }
  return !isOwnedByCurrentUser(adoptedByUserId, adoptedByName)
}

function isFollowupClaimedByCurrentUser(item: AiFollowupItem) {
  return !!item.adopted && isOwnedByCurrentUser(item.adoptedByUserId, item.adoptedByName)
}

function isFollowupClaimedByAnotherUser(item: AiFollowupItem) {
  return !!item.adopted && isOwnedByAnotherUser(item.adoptedByUserId, item.adoptedByName)
}

function updateWorkspaceAdoptionState(
  ticketId: number,
  adopted: boolean,
  adoptedByName?: string,
  adoptedAt?: string,
  adoptedByUserId?: number | null,
  lastActivityAt?: string,
  claimFreshness?: string,
) {
  aiWorkspace.value = {
    ...aiWorkspace.value,
    primarySuggestion: aiWorkspace.value.primarySuggestion.ticketId === ticketId
      ? {
          ...aiWorkspace.value.primarySuggestion,
          adopted,
          adoptedByName: adopted ? (adoptedByName || '') : '',
          adoptedAt: adopted ? (adoptedAt || '') : '',
          adoptedByUserId: adopted ? (adoptedByUserId ?? null) : null,
          lastActivityAt: adopted ? (lastActivityAt || adoptedAt || '') : '',
          claimFreshness: adopted ? (claimFreshness || 'fresh') : '',
        }
      : aiWorkspace.value.primarySuggestion,
    followups: aiWorkspace.value.followups.map((item) => (
      item.ticketId === ticketId
        ? {
            ...item,
            adopted,
            adoptedByName: adopted ? (adoptedByName || '') : '',
            adoptedAt: adopted ? (adoptedAt || '') : '',
            adoptedByUserId: adopted ? (adoptedByUserId ?? null) : null,
            lastActivityAt: adopted ? (lastActivityAt || adoptedAt || '') : '',
            claimFreshness: adopted ? (claimFreshness || 'fresh') : '',
          }
        : item
    )),
  }
}

function buildTicketFocusRoute(ticketId: number, focus: 'comments' | 'timeline' | 'knowledge') {
  return {
    path: `/tickets/${ticketId}`,
    query: {
      fromKnowledge: focus === 'comments' ? undefined : '1',
      fromAiCenter: focus === 'comments' ? '1' : undefined,
      focus,
    },
  }
}

function requestTakeoverClaim() {
  if (!selectedTicketId.value || !isAdoptedByAnotherUser.value) {
    return false
  }
  takeoverTargetTicketId.value = selectedTicketId.value
  showTakeoverConfirm.value = true
  return true
}

function cancelTakeoverClaim() {
  showTakeoverConfirm.value = false
  takeoverTargetTicketId.value = null
  if (activeDraft.value.adoptedByName) {
    replyActionMessage.value = `Kept the current shared claim with ${activeDraft.value.adoptedByName}.`
  }
}

async function confirmTakeoverClaim() {
  if (!takeoverTargetTicketId.value || !selectedTicketId.value || takeoverTargetTicketId.value !== selectedTicketId.value) {
    showTakeoverConfirm.value = false
    takeoverTargetTicketId.value = null
    return
  }
  showTakeoverConfirm.value = false
  takeoverTargetTicketId.value = null
  await markReplyDraftAsAdopted(true)
}

async function openFollowupTakeover(ticketId: number) {
  await selectTicket(ticketId)
  if (isAdoptedByAnotherUser.value) {
    requestTakeoverClaim()
  }
}

async function copyCustomerReply() {
  if (!activeDraft.value.customerReply) {
    return
  }

  await navigator.clipboard.writeText(activeDraft.value.customerReply)
  copySucceeded.value = true
  replyActionMessage.value = 'Copied the current customer reply so it can be pasted into chat, email, or another tool.'
}

async function markReplyDraftAsAdopted(skipTakeoverConfirmation = false) {
  if (!selectedTicketId.value) {
    return
  }

  const previousAdoptedByName = activeDraft.value.adoptedByName
  const takingOverClaim = isAdoptedByAnotherUser.value
  if (takingOverClaim && !skipTakeoverConfirmation) {
    requestTakeoverClaim()
    return
  }
  replyActionSubmitting.value = true
  try {
    const response = await adoptAiReplyDraft(selectedTicketId.value)
    workspaceAdoptedTicketIds.value = Array.from(new Set([
      ...workspaceAdoptedTicketIds.value,
      response.ticketId,
    ]))
    localAdoptedTicketIds.value = unmarkAiReplyDraftAdopted(response.ticketId)
    activeDraft.value = {
      ...activeDraft.value,
      adopted: response.adopted,
      adoptedByUserId: response.adoptedByUserId ?? null,
      adoptedByName: response.adoptedByName || '',
      adoptedAt: response.adoptedAt || '',
      lastActivityAt: response.lastActivityAt || response.adoptedAt || '',
      claimFreshness: response.claimFreshness || 'fresh',
    }
    updateWorkspaceAdoptionState(
      response.ticketId,
      response.adopted,
      response.adoptedByName,
      response.adoptedAt,
      response.adoptedByUserId,
      response.lastActivityAt,
      response.claimFreshness,
    )
    replyActionMessage.value = takingOverClaim && previousAdoptedByName && response.adoptedByName
      ? `Reassigned this shared AI workspace claim from ${previousAdoptedByName} to ${response.adoptedByName}.`
      : response.adoptedByName
      ? `Marked this reply draft as adopted in the shared AI workspace for ${response.adoptedByName}.`
      : 'Marked this reply draft as adopted in the shared AI workspace so the team can see it has already been handled.'
  } catch (error) {
    const localAdoptedAt = new Date().toISOString()
    localAdoptedTicketIds.value = markAiReplyDraftAdopted(selectedTicketId.value)
    activeDraft.value = {
      ...activeDraft.value,
      adopted: true,
      adoptedByUserId: null,
      adoptedByName: takingOverClaim ? `${currentUserName.value} (local)` : 'This browser',
      adoptedAt: localAdoptedAt,
      lastActivityAt: localAdoptedAt,
      claimFreshness: 'fresh',
    }
    updateWorkspaceAdoptionState(
      selectedTicketId.value,
      true,
      takingOverClaim ? `${currentUserName.value} (local)` : 'This browser',
      localAdoptedAt,
      null,
      localAdoptedAt,
      'fresh',
    )
    replyActionMessage.value = takingOverClaim
      ? 'The shared adoption state is temporarily unavailable, so this browser took over the claim locally only.'
      : 'The shared adoption state is temporarily unavailable, so this draft was marked as adopted locally in this browser instead.'
    console.error(error)
  } finally {
    replyActionSubmitting.value = false
  }
}

async function unmarkReplyDraftAsAdopted() {
  if (!selectedTicketId.value) {
    return
  }

  replyActionSubmitting.value = true
  try {
    const response = await unadoptAiReplyDraft(selectedTicketId.value)
    workspaceAdoptedTicketIds.value = workspaceAdoptedTicketIds.value.filter((ticketId) => ticketId !== response.ticketId)
    localAdoptedTicketIds.value = unmarkAiReplyDraftAdopted(response.ticketId)
    activeDraft.value = {
      ...activeDraft.value,
      adopted: response.adopted,
      adoptedByUserId: null,
      adoptedByName: '',
      adoptedAt: '',
      lastActivityAt: '',
      claimFreshness: '',
    }
    updateWorkspaceAdoptionState(response.ticketId, response.adopted)
    replyActionMessage.value = 'Removed this reply draft from the shared AI workspace adopted queue.'
  } catch (error) {
    localAdoptedTicketIds.value = unmarkAiReplyDraftAdopted(selectedTicketId.value)
    activeDraft.value = {
      ...activeDraft.value,
      adopted: false,
      adoptedByUserId: null,
      adoptedByName: '',
      adoptedAt: '',
      lastActivityAt: '',
      claimFreshness: '',
    }
    updateWorkspaceAdoptionState(selectedTicketId.value, false)
    replyActionMessage.value = 'The shared adoption state is temporarily unavailable, so the local adopted marker was cleared instead.'
    console.error(error)
  } finally {
    replyActionSubmitting.value = false
  }
}

async function toggleReplyDraftAdoption() {
  if (isAdoptedByCurrentUser.value || (isReplyDraftAdopted.value && !isAdoptedByAnotherUser.value)) {
    await unmarkReplyDraftAsAdopted()
    return
  }
  await markReplyDraftAsAdopted()
}

async function adoptReplyDraftIntoTicket() {
  if (!selectedTicketId.value) {
    return
  }
  if (!isAdoptedByCurrentUser.value) {
    await markReplyDraftAsAdopted()
  }
  saveAiReplyDraftSeed(activeDraft.value)
  await router.push({
    path: `/tickets/${selectedTicketId.value}`,
    query: {
      fromAiCenter: '1',
      focus: 'comments',
      prefillReply: '1',
    },
  })
}

async function loadAiWorkspace() {
  aiLoading.value = true
  aiErrorMessage.value = ''
  aiErrorTraceId.value = ''
  usedFallbackData.value = false

  try {
    const data = await fetchAiWorkspace()
    applyWorkspaceData(data)
  } catch (error) {
    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: 'Live AI workspace is temporarily unavailable. The page has fallen back to the local preview.',
      defaultMessage: 'Failed to load the live AI workspace. The page has kept the local preview so the team can continue reviewing the surface.',
    })
    aiErrorMessage.value = result.message
    aiErrorTraceId.value = result.traceId
    usedFallbackData.value = true
    applyWorkspaceData(fallbackWorkspace)
    console.error(error)
  } finally {
    aiLoading.value = false
  }
}

async function selectTicket(ticketId?: number | null) {
  if (!ticketId) {
    applyDraftData(fallbackReplyDraft)
    draftUsedFallback.value = true
    return
  }

  draftLoading.value = true
  draftErrorMessage.value = ''
  draftErrorTraceId.value = ''
  knowledgeDraftMessage.value = ''
  knowledgeDraftTraceId.value = ''
  draftUsedFallback.value = false

  try {
    applyDraftData(await fetchAiReplyDraft(ticketId))
  } catch (error) {
    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: 'Live draft generation is temporarily unavailable. The page has kept the preview draft.',
      defaultMessage: 'Failed to load the live reply draft. The page has kept the preview draft so the operator workflow still renders.',
    })
    draftErrorMessage.value = result.message
    draftErrorTraceId.value = result.traceId
    draftUsedFallback.value = true
    applyDraftData({
      ...fallbackReplyDraft,
      ticketId,
      ticketNo: replySuggestion.value.ticketNo || fallbackReplyDraft.ticketNo,
      ticketTitle: replySuggestion.value.title || fallbackReplyDraft.ticketTitle,
      scene: replySuggestion.value.scene || fallbackReplyDraft.scene,
      confidence: replySuggestion.value.confidence || fallbackReplyDraft.confidence,
      relatedKnowledge: aiWorkspace.value.recommendations,
    })
    console.error(error)
  } finally {
    draftLoading.value = false
  }
}

function toKnowledgeDraftSeed(detail: TicketDetailApiItem): TicketItem {
  return {
    id: detail.id,
    ticketNo: detail.ticketNo,
    title: detail.title,
    meta: detail.type,
    submitUserId: detail.submitUserId,
    assigneeUserId: detail.assigneeUserId,
    content: detail.content,
    priorityLevel: detail.priorityLabel,
    priority: detail.priorityLabel,
    status: detail.statusLabel,
    priorityClass: 'chip-blue',
    assignee: detail.assigneeName,
    submitter: detail.submitterName,
    updatedAt: detail.updateTime,
    tags: [detail.type, detail.priorityLabel].filter(Boolean),
    timeline: detail.timeline.map((item) => ({
      title: item.title,
      desc: item.desc,
      operator: item.operatorName,
      createdAt: item.createTime,
    })),
    comments: detail.comments.map((item) => ({
      author: item.authorName,
      content: item.content,
      typeLabel: item.commentTypeLabel,
      internal: item.internal,
      createdAt: item.createTime,
    })),
    relatedArticles: detail.relatedArticles,
    linkedKnowledgeArticleCount: detail.sourceKnowledgeArticles.length,
    latestLinkedKnowledgeArticle: null,
  }
}

async function createKnowledgeDraftFromAiCenter() {
  if (!selectedTicketId.value || !canManageKnowledge.value || knowledgeDraftSubmitting.value) {
    return
  }

  knowledgeDraftSubmitting.value = true
  knowledgeDraftMessage.value = ''
  knowledgeDraftTraceId.value = ''

  try {
    const article = await createTicketKnowledgeDraft(selectedTicketId.value, { origin: 'manual' })
    knowledgeDraftMessage.value = 'Created a real knowledge draft from the selected ticket. Opening the editor now.'
    await router.push(`/knowledge/articles/${article.id}/edit?from=ticket`)
  } catch (error) {
    if (!isNetworkFallbackCandidate(error)) {
      const result = resolveListLoadFailure(error, {
        networkFallbackMessage: '',
        defaultMessage: 'Failed to create the knowledge draft from the selected ticket.',
      })
      knowledgeDraftMessage.value = result.message
      knowledgeDraftTraceId.value = result.traceId
      return
    }

    try {
      const detail = await fetchTicketDetail(selectedTicketId.value)
      saveKnowledgeDraftSeed(buildKnowledgeDraftFromTicket(toKnowledgeDraftSeed(detail), { origin: 'manual' }))
      knowledgeDraftMessage.value = 'The live knowledge-draft endpoint is temporarily unavailable. A local draft seed has been prepared instead.'
      await router.push('/knowledge/articles/create?from=ticket')
    } catch (fallbackError) {
      const result = resolveListLoadFailure(fallbackError, {
        networkFallbackMessage: 'The live knowledge-draft path is temporarily unavailable, and the local fallback seed could not be prepared.',
        defaultMessage: 'Failed to prepare either the live or local knowledge draft flow from the selected ticket.',
      })
      knowledgeDraftMessage.value = result.message
      knowledgeDraftTraceId.value = result.traceId
      console.error(fallbackError)
    }
  } finally {
    knowledgeDraftSubmitting.value = false
  }
}

async function initializeWorkspace() {
  if (isDemoMode()) {
    applyWorkspaceData(fallbackWorkspace)
    applyDraftData(fallbackReplyDraft)
    draftUsedFallback.value = true
    return
  }

  await loadAiWorkspace()
  await selectTicket(aiWorkspace.value.primarySuggestion.ticketId)
}

onMounted(() => {
  initializeWorkspace()
})
</script>

<style scoped>
.page-eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ai-link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}




.ai-state-box {
  display: grid;
  gap: var(--space-2);
  margin-bottom: 14px;
}

.ai-state-box strong,
.ai-state-box p {
  margin: 0;
}

.ai-state-trace {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.ai-generated-at {
  display: grid;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.ai-generated-at strong,
.ai-generated-at p {
  margin: 0;
}

.ai-action-hint {
  margin: 12px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.ai-recommendation-copy p,
.ai-followup-item p,
.ai-highlight-card p,
.ai-draft-block p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.ai-adoption-meta {
  margin-top: 8px;
  color: #0f766e;
  font-size: var(--text-sm);
}

.ai-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 40;
  background: rgba(15, 23, 42, 0.48);
  display: grid;
  place-items: center;
  padding: 24px;
}

.ai-modal-card {
  width: min(560px, 100%);
  border-radius: 20px;
  background: var(--bg-panel-solid);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.24);
  padding: 24px;
}

.ai-modal-head {
  margin-bottom: 18px;
}

.ai-modal-actions {
  justify-content: flex-end;
  margin-top: 18px;
}

.ai-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: var(--space-4);
}

.ai-secondary-grid,
.ai-footer-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.ai-footer-grid {
  grid-template-columns: 1fr;
}

.ai-highlight-card {
  margin-bottom: 14px;
}

.ai-highlight-card strong,
.ai-recommendation-copy strong,
.ai-followup-item strong,
.ai-draft-block strong {
  color: var(--text-primary);
}

.ai-inline-actions,
.ai-recommendation-actions,
.ai-followup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
  margin-top: 12px;
}

.ai-recommendation-item,
.ai-followup-item {
  align-items: center;
  gap: var(--space-4);
}

.ai-followup-item--claimed {
  border-color: #f59e0b;
  background: linear-gradient(135deg, rgba(254, 243, 199, 0.92), rgba(255, 255, 255, 0.98));
}

.ai-followup-item--mine {
  border-color: #14b8a6;
  background: linear-gradient(135deg, rgba(204, 251, 241, 0.92), rgba(255, 255, 255, 0.98));
}

.ai-recommendation-copy,
.ai-followup-item > div,
.ai-draft-notes {
  display: grid;
  gap: var(--space-1);
}

.ai-owner-chip {
  background: var(--bg-panel-solid)7ed;
  color: #b45309;
  border: 1px solid rgba(245, 158, 11, 0.28);
}

.ai-owner-chip--stale {
  background: #fef3c7;
  color: #92400e;
  border-color: rgba(217, 119, 6, 0.34);
}

.ai-followup-groups {
  display: grid;
  gap: var(--space-4);
}

.ai-followup-group {
  display: grid;
  gap: 10px;
}

.ai-followup-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.ai-followup-group-head p {
  margin: 4px 0 0;
  color: var(--text-secondary);
}

.ai-draft-list {
  margin-bottom: 14px;
}

.ai-draft-block {
  align-items: start;
}

@media (max-width: 1280px) {
  .ai-main-grid,
  .ai-secondary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
