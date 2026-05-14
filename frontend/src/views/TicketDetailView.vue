<template>
  <div class="detail-page">
    <header class="detail-topbar">
      <RouterLink class="back-link" to="/tickets">返回工单中心</RouterLink>
      <div class="detail-topbar-actions">
        <button class="ghost-button" type="button" @click="loadTicket">刷新详情</button>
        <button
          v-if="canCreateKnowledgeFromTicket"
          class="ghost-button"
          type="button"
          @click="createKnowledgeDraftFromAction"
        >
          沉淀为知识文章
        </button>
        <button class="primary-button" type="button" @click="focusCommentInput">
          {{ canUseInternalComments ? '新增评论' : '补充反馈' }}
        </button>
      </div>
    </header>

    <main v-if="ticket" class="detail-layout">
      <section class="detail-main">
        <article class="panel">
          <div class="detail-chip-row">
            <span class="chip chip-blue">{{ ticket.ticketNo }}</span>
            <span class="chip" :class="ticket.priorityClass">{{ ticket.priority }}</span>
            <span class="chip chip-green">{{ ticket.status }}</span>
            <span v-if="isLocalTicket" class="chip chip-orange">本地工作流</span>
            <span v-if="hasLinkedKnowledgeArticles" class="chip chip-blue">已沉淀 {{ linkedKnowledgeArticles.length }} 篇</span>
          </div>

          <h1 class="detail-title">{{ ticket.title }}</h1>
          <p class="detail-summary">{{ ticket.content }}</p>

          <ErrorTraceNotice v-if="topErrorMessage" :message="topErrorMessage" :trace-id="topErrorTraceId" />
          <div v-else-if="successMessage" class="state-box">{{ successMessage }}</div>

          <div class="detail-meta">
            <span>提交人：{{ ticket.submitter }}</span>
            <span>当前处理人：{{ ticket.assignee }}</span>
            <span>最后更新时间：{{ ticket.updatedAt }}</span>
            <span>关联标签：{{ ticket.tags?.length ?? 0 }}</span>
          </div>

          <div class="ticket-overview-grid">
            <div class="ticket-overview-card">
              <span class="muted">优先级</span>
              <strong>{{ ticket.priorityLevel }}</strong>
              <p>{{ ticket.priority }}</p>
            </div>
            <div class="ticket-overview-card">
              <span class="muted">当前状态</span>
              <strong>{{ ticket.status }}</strong>
              <p>围绕处理流转展开下一步动作</p>
            </div>
            <div class="ticket-overview-card">
              <span class="muted">评论数量</span>
              <strong>{{ ticket.comments?.length ?? 0 }}</strong>
              <p>协作信息集中沉淀在评论区</p>
            </div>
          </div>

          <div class="ticket-insight-strip">
            <div class="insight-card">
              <span class="muted">推荐下一步</span>
              <strong>{{ nextActionTitle }}</strong>
            </div>
            <div class="insight-card">
              <span class="muted">协作健康度</span>
              <strong>{{ collaborationHealth }}</strong>
            </div>
            <div class="insight-card">
              <span class="muted">知识关联度</span>
              <strong>{{ ticket.linkedKnowledgeArticleCount || relatedArticles.length }} 篇</strong>
            </div>
          </div>

          <div v-if="showKnowledgeCapturePrompt" class="state-box knowledge-capture-box">
            <div class="knowledge-capture-copy">
              <strong>{{ knowledgePromptTitle }}</strong>
              <p>
                {{ knowledgePromptDescription }}
              </p>
            </div>
            <div class="knowledge-capture-actions">
              <button v-if="canCreateKnowledgeFromTicket" class="ghost-button" type="button" @click="createKnowledgeDraftFromAction">
                {{ knowledgePrimaryActionLabel }}
              </button>
              <RouterLink
                v-if="primaryLinkedKnowledgeArticle"
                class="ghost-button inline-link-button"
                :to="`/knowledge/articles/${primaryLinkedKnowledgeArticle.id}`"
              >
                查看已有文章
              </RouterLink>
            </div>
          </div>
        </article>

        <article class="panel">
          <div v-if="knowledgeReturnMessage" class="state-box section-highlight-banner">
            {{ knowledgeReturnMessage }}
          </div>

          <div class="panel-head">
            <div>
              <h3>协作评论</h3>
              <p>把用户反馈、处理说明和内部备注拆开看，工单会更像真实协作系统。</p>
            </div>
            <button class="ghost-button" type="button" @click="focusCommentInput">定位到输入区</button>
          </div>

          <div v-if="ticketCommentAccessCopy.banner" class="state-box">
            {{ ticketCommentAccessCopy.banner }}
          </div>

          <div ref="commentsSectionRef" class="comment-toolbar" :class="{ 'section-highlight': highlightedSection === 'comments' }">
            <button
              v-for="filter in visibleCommentFilters"
              :key="filter.value"
              class="chip quick-filter-chip"
              :class="activeCommentFilter === filter.value ? 'chip-blue active-chip' : 'chip-default'"
              type="button"
              @click="activeCommentFilter = filter.value"
            >
              {{ filter.label }}
            </button>
          </div>

          <form class="ticket-form" @submit.prevent="submitComment">
            <div v-if="canUseInternalComments" class="form-grid">
              <label class="field">
                <span>评论类型</span>
                <select v-model="commentForm.commentType" class="field-control">
                  <option :value="1">普通评论</option>
                  <option :value="2">处理说明</option>
                  <option :value="3">内部备注</option>
                </select>
              </label>
              <label class="field checkbox-field">
                <input v-model="commentForm.internal" type="checkbox" />
                <span>仅内部可见</span>
              </label>
            </div>
            <div v-else class="state-box">
              {{ ticketCommentAccessCopy.formHint }}
            </div>

            <label class="field">
              <span>评论内容</span>
              <textarea
                ref="commentInputRef"
                v-model="commentForm.content"
                class="field-control field-textarea"
                rows="4"
                placeholder="补充处理进展、排查结论或需要同步给团队的信息"
              />
            </label>

            <div class="form-actions">
              <ErrorTraceNotice v-if="commentError" inline :message="commentError" :trace-id="commentErrorTraceId" />
              <button class="primary-button" type="submit" :disabled="commentSubmitting">
                {{ commentSubmitting ? '提交中...' : '提交评论' }}
              </button>
            </div>
          </form>

          <div v-if="!filteredComments.length" class="state-box">当前筛选下还没有评论记录。</div>
          <div v-else class="comment-list">
            <div
              v-for="(comment, index) in filteredComments"
              :key="comment.author + comment.createdAt + comment.content"
              class="comment-card"
              :class="{ 'context-card-highlight': highlightedSection === 'comments' && index < 2 }"
            >
              <div class="comment-head">
                <div>
                  <strong>{{ comment.author }}</strong>
                  <p>{{ comment.createdAt }}</p>
                </div>
                <div class="comment-badges">
                  <span class="chip chip-blue">{{ comment.typeLabel }}</span>
                  <span v-if="comment.internal" class="chip chip-orange">内部可见</span>
                </div>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
            </div>
          </div>
        </article>

        <article ref="timelineSectionRef" class="panel" :class="{ 'section-highlight': highlightedSection === 'timeline' }">
          <div class="panel-head">
            <div>
              <h3>处理记录</h3>
              <p>这部分直接对应数据库里的 `ticket_record`，重点体现可追踪性和过程留痕。</p>
            </div>
            <span class="chip chip-blue">{{ ticket.timeline?.length ?? 0 }} 条</span>
          </div>

          <div class="timeline">
            <div
              v-for="(item, index) in ticket.timeline"
              :key="item.title + item.desc + item.createdAt"
              class="timeline-item timeline-card"
              :class="{
                'timeline-highlight': index === 0,
                'context-card-highlight': highlightedSection === 'timeline' && index < 2,
              }"
            >
              <span class="dot"></span>
              <div class="timeline-content">
                <div class="timeline-heading">
                  <strong>{{ item.title }}</strong>
                  <span>{{ item.createdAt }}</span>
                </div>
                <p>{{ item.desc }}</p>
                <span v-if="item.operator" class="timeline-operator">操作人：{{ item.operator }}</span>
              </div>
            </div>
          </div>
        </article>
      </section>

      <aside class="detail-side">
        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>处理 checklist</h3>
              <p>让处理动作更像真实工作流，而不是靠人脑记忆。</p>
            </div>
          </div>
          <div class="checklist">
            <label v-for="item in actionChecklist" :key="item.key" class="checklist-item">
              <input :checked="item.done" type="checkbox" disabled />
              <span>{{ item.label }}</span>
            </label>
          </div>
          <div class="state-box checklist-summary">已完成 {{ completedChecklistCount }}/{{ actionChecklist.length }} 项</div>
        </article>

        <article v-if="canAssignCurrentTicket" class="panel">
          <div class="panel-head">
            <div>
              <h3>指派处理人</h3>
              <p>让工单明确归属，后续的状态流转、通知和 SLA 才有真实业务基础。</p>
            </div>
          </div>
          <form class="ticket-form" @submit.prevent="submitAssignment">
            <label class="field">
              <span>处理人</span>
              <select v-model="assignForm.assigneeUserId" class="field-control">
                <option :value="0">请选择处理人</option>
                <option v-for="option in assigneeOptions" :key="option.id" :value="option.id">
                  {{ option.displayName }} · {{ option.roles.join(' / ') }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>指派备注</span>
              <textarea
                v-model="assignForm.remark"
                class="field-control field-textarea"
                rows="3"
                placeholder="例如：该问题与支付回调链路相关，请优先接手排查"
              />
            </label>

            <div class="form-actions">
              <ErrorTraceNotice v-if="assignError" inline :message="assignError" :trace-id="assignErrorTraceId" />
              <button class="primary-button" type="submit" :disabled="assignSubmitting">
                {{ assignSubmitting ? '提交中...' : '确认指派' }}
              </button>
            </div>
          </form>
        </article>

        <article v-if="canTransitionCurrentTicket" class="panel">
          <div class="panel-head">
            <div>
              <h3>状态流转</h3>
              <p>这一步正式把“能看详情”推进到“能操作工单”。</p>
            </div>
          </div>
          <form class="ticket-form" @submit.prevent="submitStatusUpdate">
            <label class="field">
              <span>更新状态</span>
              <select v-model="statusForm.status" class="field-control">
                <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>流转备注</span>
              <textarea
                v-model="statusForm.remark"
                class="field-control field-textarea"
                rows="3"
                placeholder="例如：已联系接口负责人，预计 30 分钟内完成验证"
              />
            </label>

            <div class="form-actions">
              <ErrorTraceNotice v-if="statusError" inline :message="statusError" :trace-id="statusErrorTraceId" />
              <button class="primary-button" type="submit" :disabled="statusSubmitting">
                {{ statusSubmitting ? '更新中...' : '提交状态变更' }}
              </button>
            </div>
          </form>
        </article>

        <article v-if="!canOperateCurrentTicket" class="panel">
          <div class="panel-head">
            <div>
              <h3>{{ ticketPermissionSummary.title }}</h3>
              <p>{{ ticketPermissionSummary.description }}</p>
            </div>
          </div>
          <div class="mini-list">
            <div v-for="(item, index) in ticketPermissionSummary.items" :key="item" class="mini-item">
              <span>{{ index + 1 }}.</span><span>{{ item }}</span>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>工单标签</h3>
              <p>标签、优先级和状态是很适合面试展开的业务点。</p>
            </div>
          </div>
          <div class="ticket-board-tags">
            <span v-for="tag in ticket.tags" :key="tag" class="chip chip-blue">{{ tag }}</span>
          </div>
        </article>

        <article class="panel knowledge-workbench-panel">
          <div class="panel-head">
            <div>
              <h3>知识沉淀工作台</h3>
              <p>把“看过这个工单”推进到“顺手沉淀、顺手回查、顺手继续补完”。</p>
            </div>
          </div>
          <div class="knowledge-workbench-grid">
            <div class="knowledge-workbench-card">
              <span class="muted">当前沉淀状态</span>
              <strong>{{ knowledgeWorkbenchStatusTitle }}</strong>
              <p>{{ knowledgeWorkbenchDescription }}</p>
            </div>
            <div class="knowledge-workbench-card">
              <span class="muted">推荐动作</span>
              <strong>{{ knowledgeWorkbenchActionTitle }}</strong>
              <p>尽量把根因、排查路径和预防动作沉淀成可复用文档。</p>
            </div>
          </div>
          <div class="knowledge-workbench-actions">
            <button
              v-if="canCreateKnowledgeFromTicket"
              class="primary-button"
              type="button"
              @click="createKnowledgeDraftFromAction"
            >
              {{ knowledgePrimaryActionLabel }}
            </button>
            <RouterLink class="ghost-button inline-link-button" :to="buildKnowledgeListRoute()">
              查看同工单文章
            </RouterLink>
            <RouterLink
              v-if="primaryLinkedKnowledgeArticle"
              class="ghost-button inline-link-button"
              :to="`/knowledge/articles/${primaryLinkedKnowledgeArticle.id}`"
            >
              打开最近沉淀
            </RouterLink>
          </div>
        </article>

        <article ref="knowledgeSectionRef" class="panel" :class="{ 'section-highlight': highlightedSection === 'knowledge' }">
          <div v-if="primaryLinkedKnowledgeArticle" class="state-box knowledge-summary-box">
            <span class="muted">最近沉淀文章</span>
            <strong>{{ primaryLinkedKnowledgeArticle.title }}</strong>
            <p>{{ primaryLinkedKnowledgeArticle.statusLabel }} · {{ primaryLinkedKnowledgeArticle.updatedAt }}</p>
            <RouterLink class="source-ticket-link" :to="`/knowledge/articles/${primaryLinkedKnowledgeArticle.id}`">
              打开这篇文章
            </RouterLink>
          </div>

          <div v-if="primaryLinkedKnowledgeArticle" class="panel-divider"></div>

          <div class="panel-head">
            <div>
              <h3>关联知识</h3>
              <p>让知识库真正参与处理闭环，而不只是另一个页面。</p>
            </div>
          </div>
          <div class="mini-list">
            <div v-if="sourceKnowledgeArticles.length" class="knowledge-group">
              <div class="knowledge-group-head">
                <strong>来源沉淀</strong>
                <div class="knowledge-group-tools">
                  <span>{{ sourceKnowledgeArticles.length }} 篇</span>
                  <select v-model="sourceKnowledgeSort" class="knowledge-sort-select">
                    <option value="latest">按最近更新</option>
                    <option value="status">按状态</option>
                    <option value="title">按标题</option>
                  </select>
                </div>
              </div>
              <div class="knowledge-filter-chips">
                <button
                  v-for="filter in sourceKnowledgeStatusFilters"
                  :key="filter.value"
                  class="chip quick-filter-chip"
                  :class="activeSourceKnowledgeStatus === filter.value ? 'chip-blue active-chip' : 'chip-default'"
                  type="button"
                  @click="activeSourceKnowledgeStatus = filter.value"
                >
                  {{ filter.label }}
                </button>
              </div>
              <RouterLink
                v-for="article in sourceKnowledgeArticles"
                :key="`linked-${article.id}`"
                class="mini-item related-link"
                :to="`/knowledge/articles/${article.id}`"
              >
                <div class="related-link-main">
                  <strong>{{ article.title }}</strong>
                  <span>{{ article.metric }}</span>
                </div>
                <div class="knowledge-badges">
                  <span class="chip" :class="article.statusTone">{{ article.statusLabel }}</span>
                </div>
              </RouterLink>
              <div
                v-if="linkedKnowledgeArticles.length && !sourceKnowledgeArticles.length"
                class="mini-item knowledge-empty-hint"
              >
                <span>当前状态筛选下暂无来源沉淀文章，切换上方筛选试试看。</span>
              </div>
            </div>
            <div v-if="recommendedKnowledgeArticles.length" class="knowledge-group">
              <div class="knowledge-group-head">
                <strong>相似推荐</strong>
                <div class="knowledge-group-tools">
                  <span>{{ recommendedKnowledgeArticles.length }} 篇</span>
                  <select v-model="recommendedKnowledgeSort" class="knowledge-sort-select">
                    <option value="default">按推荐优先</option>
                    <option value="title">按标题</option>
                  </select>
                </div>
              </div>
              <RouterLink
                v-for="article in recommendedKnowledgeArticles"
                :key="`related-${article.id}`"
                class="mini-item related-link"
                :to="`/knowledge/articles/${article.id}`"
              >
                <span>{{ article.title }}</span>
                <span>{{ article.reason }}</span>
              </RouterLink>
            </div>
            <div v-if="!sourceKnowledgeArticles.length && !recommendedKnowledgeArticles.length" class="mini-item">
              <span>当前还没有可展示的关联知识。</span>
            </div>
          </div>
        </article>

        <article class="panel panel-dark">
          <div class="panel-head panel-head-dark">
            <div>
              <h3>AI 处理建议</h3>
              <p>后面这里可以继续接真实的工单回复建议和知识推荐。</p>
            </div>
          </div>
          <div class="ai-summary">
            建议先匹配历史相似工单，再把标准处理文档推送给当前处理人。这样既能提升效率，也能让知识库真正参与工单闭环。
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>当前建议动作</h3>
              <p>让详情页除了“展示”之外，还能给用户一点下一步方向。</p>
            </div>
          </div>
          <div class="mini-list">
            <div v-for="(item, index) in ticketSuggestedActions" :key="item" class="mini-item">
              <span>{{ index + 1 }}.</span><span>{{ item }}</span>
            </div>
          </div>
        </article>
      </aside>
    </main>

    <section v-else class="panel">
      <div class="state-box state-warning">没有找到对应工单。</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  addTicketComment,
  assignTicket,
  createTicketKnowledgeDraft,
  fetchTicketDetail,
  fetchTicketAssignees,
  type TicketAssigneeOption,
  type TicketDetailApiItem,
  updateTicketStatus,
} from '../api/ticket'
import {
  buildTicketCommentAccessCopy,
  buildTicketPermissionSummary,
  buildTicketSuggestedActions,
} from '../access-policy'
import { fallbackArticles, tickets as fallbackTickets } from '../mock/dashboard'
import { listKnowledgeDrafts } from '../mock/knowledgeDrafts'
import {
  appendLocalComment,
  assignLocalTicket,
  getLocalTicket,
  mergeTickets,
  updateLocalTicketStatus,
} from '../mock/ticketWorkspace'
import { authState } from '../auth'
import {
  canAssignTickets,
  canManageKnowledgeArticles,
  canTransitionTickets,
  canUseInternalTicketComments,
  canViewAllTickets,
} from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import type { TicketItem } from '../types/dashboard'
import { getApiErrorDisplay } from '../utils/apiErrorDisplay'
import { resolveTicketDetailFallback } from '../utils/ticketDetailFailure'
import { buildKnowledgeDraftFromTicket, saveKnowledgeDraftSeed } from '../utils/knowledgeFromTicket'
import { attachArticleSourceTicket } from '../utils/knowledgeSourceTicket'
import { isDemoMode } from '../utils/runtimeMode'
import { formatDateText, formatTicketDetailItem, toKnowledgeArticleStatusText, toStatusText } from '../utils/ticketPresentation'

const route = useRoute()
const router = useRouter()
const remoteTicket = ref<TicketItem | null>(null)
const commentInputRef = ref<HTMLTextAreaElement | null>(null)
const commentsSectionRef = ref<HTMLElement | null>(null)
const timelineSectionRef = ref<HTMLElement | null>(null)
const knowledgeSectionRef = ref<HTMLElement | null>(null)
const commentSubmitting = ref(false)
const statusSubmitting = ref(false)
const assignSubmitting = ref(false)
const commentError = ref('')
const statusError = ref('')
const assignError = ref('')
const commentErrorTraceId = ref('')
const statusErrorTraceId = ref('')
const assignErrorTraceId = ref('')
const assigneeOptions = ref<TicketAssigneeOption[]>([])
const successMessage = ref('')
const topErrorMessage = ref('')
const topErrorTraceId = ref('')
const activeCommentFilter = ref<'all' | 'public' | 'internal'>('all')
const linkedKnowledgeArticles = ref<Array<{
  id: number
  title: string
  metric: string
  statusRank: number
  updatedAt: string
  statusLabel: string
  statusTone: string
  statusKey: 'published' | 'draft' | 'archived' | 'local'
}>>([])
const remoteSourceKnowledgeArticles = ref<Array<{
  id: number
  title: string
  metric: string
  statusRank: number
  updatedAt: string
  statusLabel: string
  statusTone: string
  statusKey: 'published' | 'draft' | 'archived'
}>>([])
const highlightedSection = ref<'comments' | 'timeline' | 'knowledge' | null>(null)
const sourceKnowledgeSort = ref<'latest' | 'status' | 'title'>('latest')
const recommendedKnowledgeSort = ref<'default' | 'title'>('default')
const activeSourceKnowledgeStatus = ref<'all' | 'published' | 'draft' | 'archived' | 'local'>('all')
let ticketLoadRequestId = 0

const commentForm = ref({
  content: '',
  commentType: 1,
  internal: false,
})

const statusForm = ref({
  status: 1,
  remark: '',
})

const assignForm = ref({
  assigneeUserId: 0,
  remark: '',
})

const statusOptions = [
  { value: 1, label: '\u65b0\u5efa' },
  { value: 2, label: '\u5904\u7406\u4e2d' },
  { value: 3, label: '\u5df2\u89e3\u51b3' },
  { value: 4, label: '\u5df2\u5173\u95ed' },
]
const commentFilters = [
  { value: 'all', label: '\u5168\u90e8\u8bc4\u8bba' },
  { value: 'public', label: '\u5bf9\u5916\u53ef\u89c1' },
  { value: 'internal', label: '\u5185\u90e8\u5907\u6ce8' },
] as const
const sourceKnowledgeStatusFilters = [
  { value: 'all', label: '\u5168\u90e8' },
  { value: 'published', label: '\u5df2\u53d1\u5e03' },
  { value: 'draft', label: '\u8349\u7a3f' },
  { value: 'archived', label: '\u5df2\u5f52\u6863' },
  { value: 'local', label: '\u672c\u5730\u8349\u7a3f' },
] as const

const canManageKnowledge = computed(() => canManageKnowledgeArticles())
const canUseInternalComments = computed(() => canUseInternalTicketComments())
const canAssignCurrentTicket = computed(() => canAssignTickets())
const canTransitionCurrentTicket = computed(() => canTransitionTickets())
const canOperateCurrentTicket = computed(() => canAssignCurrentTicket.value || canTransitionCurrentTicket.value)
const currentUserId = computed(() => authState.user?.id ?? null)
const currentUserName = computed(() => authState.user?.nickname || authState.user?.realName || authState.user?.username || '当前用户')
const isLocalTicket = computed(() => !!ticket.value && 'source' in ticket.value && ticket.value.source === 'local')
const currentTicketStatusValue = computed(() => {
  if (!ticket.value?.status) {
    return null
  }
  return statusOptions.find((item) => item.label === ticket.value?.status || toStatusText(item.value) === ticket.value?.status)?.value ?? null
})
const isKnowledgeEligibleStatus = computed(() => currentTicketStatusValue.value === 3 || currentTicketStatusValue.value === 4)
const canCreateKnowledgeFromTicket = computed(() => canManageKnowledge.value && isKnowledgeEligibleStatus.value)
const showKnowledgeCapturePrompt = computed(() => isKnowledgeEligibleStatus.value && canManageKnowledge.value)
const ticketCommentAccessCopy = computed(() => buildTicketCommentAccessCopy({
  canUseInternalComments: canUseInternalComments.value,
}))
const ticketPermissionSummary = computed(() => buildTicketPermissionSummary())
const ticketSuggestedActions = computed(() => buildTicketSuggestedActions({
  canUseInternalComments: canUseInternalComments.value,
}))
const hasLinkedKnowledgeArticles = computed(() => linkedKnowledgeArticles.value.length > 0)
const hasKnowledgeContext = computed(() => hasLinkedKnowledgeArticles.value || relatedArticles.value.length > 0)
const hasTicketOwner = computed(() => {
  const assignee = ticket.value?.assignee?.trim()
  return !!assignee && assignee !== '\u5f85\u5206\u914d'
})
const hasCollaborationContext = computed(() => {
  const commentCount = ticket.value?.comments?.length ?? 0
  const timelineCount = ticket.value?.timeline?.length ?? 0
  return commentCount > 0 || timelineCount > 0
})
const actionChecklist = computed(() => ([
  { key: 'owner', label: '\u786e\u8ba4\u5f53\u524d\u5904\u7406\u4eba', done: hasTicketOwner.value },
  { key: 'context', label: '\u8865\u9f50\u6392\u67e5\u4e0a\u4e0b\u6587', done: hasCollaborationContext.value },
  { key: 'kb', label: '\u67e5\u9605\u5173\u8054\u77e5\u8bc6\u6587\u7ae0', done: hasKnowledgeContext.value },
]))
const primaryLinkedKnowledgeArticle = computed(() => (
  linkedKnowledgeArticles.value
    .slice()
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))[0] || null
))
const knowledgePromptTitle = computed(() => (
  hasLinkedKnowledgeArticles.value
    ? '\u8fd9\u5f20\u5de5\u5355\u5df2\u7ecf\u6c89\u6dc0\u8fc7\u77e5\u8bc6\u6587\u7ae0\u3002'
    : '\u8fd9\u5f20\u5de5\u5355\u5df2\u7ecf\u5904\u7406\u5b8c\u6210\uff0c\u9002\u5408\u7ee7\u7eed\u6c89\u6dc0\u77e5\u8bc6\u6587\u7ae0\u3002'
))
const knowledgePromptDescription = computed(() => (
  hasLinkedKnowledgeArticles.value
    ? '\u4f60\u53ef\u4ee5\u7ee7\u7eed\u8865\u5145\u65b0\u7684\u5904\u7406\u89c6\u89d2\uff0c\u6216\u8005\u56de\u770b\u5df2\u6709\u6587\u7ae0\u3002'
    : '\u5efa\u8bae\u628a\u6839\u56e0\u3001\u6392\u67e5\u8def\u5f84\u548c\u9884\u9632\u52a8\u4f5c\u6574\u7406\u6210\u6807\u51c6\u6587\u6863\uff0c\u65b9\u4fbf\u540e\u7eed\u590d\u7528\u3002'
))
const knowledgePrimaryActionLabel = computed(() => (
  hasLinkedKnowledgeArticles.value ? '\u7ee7\u7eed\u6c89\u6dc0' : '\u751f\u6210\u77e5\u8bc6\u8349\u7a3f'
))
const knowledgeWorkbenchStatusTitle = computed(() => (
  hasLinkedKnowledgeArticles.value ? `\u5df2\u5173\u8054 ${linkedKnowledgeArticles.value.length} \u7bc7` : '\u5c1a\u672a\u6c89\u6dc0'
))
const knowledgeWorkbenchDescription = computed(() => (
  canCreateKnowledgeFromTicket.value
    ? '\u8fd9\u5f20\u5de5\u5355\u5df2\u7ecf\u9002\u5408\u7ee7\u7eed\u6c89\u6dc0\u77e5\u8bc6\u6587\u7ae0\u3002'
    : '\u5148\u5b8c\u6210\u5904\u7406\u6d41\u8f6c\uff0c\u518d\u628a\u7ecf\u9a8c\u6574\u7406\u6210\u77e5\u8bc6\u3002'
))
const knowledgeWorkbenchActionTitle = computed(() => (
  hasLinkedKnowledgeArticles.value ? '\u7ee7\u7eed\u8865\u5145\u5df2\u6709\u6c89\u6dc0' : '\u751f\u6210\u7b2c\u4e00\u7bc7\u6c89\u6dc0\u8349\u7a3f'
))
const visibleCommentFilters = computed(() => canUseInternalComments.value
  ? commentFilters
  : commentFilters.filter((item) => item.value !== 'internal'))

const filteredComments = computed(() => {
  const comments = (ticket.value?.comments || []).filter((item) => canUseInternalComments.value || !item.internal)
  if (activeCommentFilter.value === 'public') {
    return comments.filter((item) => !item.internal)
  }
  if (activeCommentFilter.value === 'internal') {
    return canUseInternalComments.value ? comments.filter((item) => item.internal) : []
  }
  return comments
})
const completedChecklistCount = computed(() => actionChecklist.value.filter((item) => item.done).length)
const relatedArticles = computed(() => {
  if (ticket.value?.relatedArticles?.length) {
    return ticket.value.relatedArticles
  }

  const tags = ticket.value?.tags || []
  return fallbackArticles
    .filter((article) => tags.some((tag) => article.title.includes(tag) || article.summary.includes(tag)))
    .slice(0, 3)
    .map((article) => ({
      id: article.id,
      title: article.title,
      reason: article.tag,
    }))
})
const sourceKnowledgeArticles = computed(() => {
  const items = linkedKnowledgeArticles.value
    .filter((item) => {
      if (activeSourceKnowledgeStatus.value === 'all') {
        return true
      }
      return item.statusKey === activeSourceKnowledgeStatus.value
    })
    .slice() as Array<{
      id: number
      title: string
      metric: string
      statusRank: number
      updatedAt: string
      statusLabel: string
      statusTone: string
      statusKey: 'published' | 'draft' | 'archived' | 'local'
    }>
  if (sourceKnowledgeSort.value === 'title') {
    return items.sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'))
  }
  if (sourceKnowledgeSort.value === 'status') {
    return items.sort((a, b) => a.statusRank - b.statusRank || b.updatedAt.localeCompare(a.updatedAt))
  }
  return items.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
})
const recommendedKnowledgeArticles = computed(() => {
  const items = relatedArticles.value
    .filter((article) => !linkedKnowledgeArticles.value.some((linked) => linked.id === article.id))
    .slice(0, 3)

  if (recommendedKnowledgeSort.value === 'title') {
    return [...items].sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'))
  }
  return items
})
const nextActionTitle = computed(() => {
  if (currentTicketStatusValue.value === 1) {
    return '\u5c3d\u5feb\u786e\u8ba4\u5904\u7406\u4eba\u5e76\u8865\u9f50\u4e0a\u4e0b\u6587'
  }
  if (currentTicketStatusValue.value === 2) {
    return '\u8865\u5145\u6700\u65b0\u8bc4\u8bba\u5e76\u540c\u6b65\u5173\u8054\u77e5\u8bc6'
  }
  return '\u786e\u8ba4\u662f\u5426\u9700\u8981\u7ee7\u7eed\u6c89\u6dc0\u77e5\u8bc6\u6587\u7ae0'
})

const collaborationHealth = computed(() => {
  const commentCount = ticket.value?.comments?.length ?? 0
  const timelineCount = ticket.value?.timeline?.length ?? 0
  if (commentCount >= 2 && timelineCount >= 2) {
    return '良好'
  }
  if (commentCount >= 1) {
    return '正常'
  }
  return '待补充'
})
const knowledgeReturnMessage = computed(() => {
  if (route.query.fromKnowledge !== '1') {
    return ''
  }
  const articleTitle = typeof route.query.articleTitle === 'string' ? route.query.articleTitle : ''
  if (!articleTitle) {
    return '你正在回看这篇知识文章对应的工单上下文。'
  }
  return `你正在回看知识文章《${articleTitle}》对应的工单上下文。`
})

const ticket = computed(() => {
  const id = Number(route.params.id)
  return remoteTicket.value ?? mergeTickets(fallbackTickets).find((item) => item.id === id)
})

const latestDraftKnowledgeArticle = computed(() => {
  return linkedKnowledgeArticles.value
    .filter((item) => item.statusKey === 'draft' || item.statusKey === 'local')
    .slice()
    .sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))[0] || null
})

async function createKnowledgeDraft(options?: { origin?: 'manual' | 'ticket-close'; closeRemark?: string }) {
  if (!ticket.value || !canCreateKnowledgeFromTicket.value) {
    return
  }

  topErrorMessage.value = ''
  topErrorTraceId.value = ''

  const existingDraft = latestDraftKnowledgeArticle.value
  if (existingDraft) {
    successMessage.value = existingDraft.statusKey === 'local'
      ? '\u8fd9\u5f20\u5de5\u5355\u5df2\u7ecf\u6709\u672c\u5730\u8349\u7a3f\uff0c\u6b63\u5728\u7ee7\u7eed\u7f16\u8f91\u3002'
      : '\u8fd9\u5f20\u5de5\u5355\u5df2\u7ecf\u6709\u771f\u5b9e\u8349\u7a3f\uff0c\u6b63\u5728\u7ee7\u7eed\u7f16\u8f91\u3002'
    const query = getKnowledgeDraftRedirectQuery(options?.origin)
    const targetPath = `/knowledge/articles/${existingDraft.id}/edit${query}`
    await router.push(targetPath)
    return
  }

    if (!isDemoMode()) {
      try {
        const article = await createTicketKnowledgeDraft(ticket.value.id, {
          origin: options?.origin,
          closeRemark: options?.closeRemark,
        })
        saveKnowledgeDraftSeed(buildKnowledgeDraftFromTicket(ticket.value, options))
        successMessage.value = '\u5df2\u57fa\u4e8e\u5f53\u524d\u5de5\u5355\u751f\u6210\u771f\u5b9e\u77e5\u8bc6\u8349\u7a3f\uff0c\u6b63\u5728\u8fdb\u5165\u7f16\u8f91\u9875\u3002'
        await loadLinkedKnowledgeArticles()
        const query = getKnowledgeDraftRedirectQuery(options?.origin)
        await router.push(`/knowledge/articles/${article.id}/edit${query}`)
        return
    } catch (error) {
      const result = resolveTicketDetailFallback(error, '知识草稿生成失败，请稍后重试。')
      if (result.mode === 'show-error') {
        topErrorMessage.value = result.message
        topErrorTraceId.value = result.traceId
        return
      }
      console.error(error)
    }
  }

  saveKnowledgeDraftSeed(buildKnowledgeDraftFromTicket(ticket.value, options))
  const query = getKnowledgeDraftRedirectQuery(options?.origin)
  await router.push(`/knowledge/articles/create${query}`)
}

function createKnowledgeDraftFromAction() {
  createKnowledgeDraft()
}

function buildKnowledgeListRoute() {
  if (!ticket.value) {
    return '/knowledge/articles'
  }

  return {
    path: '/knowledge/articles',
    query: {
      sourceTicketNo: ticket.value.ticketNo,
      quickFilter: 'source-linked',
    },
  }
}

function canCurrentUserAccessTicket(item: TicketItem | null) {
  if (!item) {
    return false
  }
  if (canViewAllTickets()) {
    return true
  }
  return item.submitUserId === currentUserId.value
    || item.submitter === currentUserName.value
    || item.assigneeUserId === currentUserId.value
    || item.assignee === currentUserName.value
}

function toKnowledgeStatusTone(status: number) {
  if (status === 1) {
    return 'chip-green'
  }
  if (status === 2) {
    return 'chip-default'
  }
  return 'chip-blue'
}


function buildKnowledgeArticleMetric(statusLabel: string, ticketNo?: string, ticketId?: number) {
  return `${statusLabel} · ${ticketNo || `#${ticketId}`}`
}

function getKnowledgeDraftRedirectQuery(origin?: 'manual' | 'ticket-close') {
  return origin === 'ticket-close' ? '?from=ticket-close' : '?from=ticket'
}

function buildCommentSuccessMessage(isLocal: boolean) {
  return isLocal ? '\u8bc4\u8bba\u5df2\u5199\u5165\u672c\u5730\u5de5\u4f5c\u6d41\u3002' : '\u8bc4\u8bba\u5df2\u63d0\u4ea4\u3002'
}

function buildAssignmentSuccessMessage(isLocal: boolean) {
  return isLocal ? '\u5904\u7406\u4eba\u5df2\u5199\u5165\u672c\u5730\u5de5\u4f5c\u6d41\u3002' : '\u5904\u7406\u4eba\u5df2\u66f4\u65b0\u3002'
}

function buildStatusSuccessMessage(status: number, isLocal: boolean) {
  if (status >= 3) {
    return isLocal
      ? '\u72b6\u6001\u5df2\u66f4\u65b0\uff0c\u672c\u5730\u5de5\u5355\u5df2\u7ecf\u9002\u5408\u6c89\u6dc0\u77e5\u8bc6\u6587\u7ae0\u3002'
      : '\u72b6\u6001\u5df2\u66f4\u65b0\uff0c\u8fd9\u5f20\u5de5\u5355\u5df2\u7ecf\u9002\u5408\u7ee7\u7eed\u6c89\u6dc0\u77e5\u8bc6\u6587\u7ae0\u3002'
  }
  return isLocal ? '\u72b6\u6001\u5df2\u66f4\u65b0\u5230\u672c\u5730\u5de5\u4f5c\u6d41\u3002' : '\u72b6\u6001\u5df2\u66f4\u65b0\u3002'
}

function mapRemoteSourceKnowledgeArticles(data: TicketDetailApiItem | null) {
  if (!data?.sourceKnowledgeArticles?.length) {
    remoteSourceKnowledgeArticles.value = []
    return
  }

  remoteSourceKnowledgeArticles.value = data.sourceKnowledgeArticles.map((item) => ({
    id: item.id,
    title: item.title,
    metric: buildKnowledgeArticleMetric(
      toKnowledgeArticleStatusText(item.status, item.statusLabel),
      ticket.value?.ticketNo,
      ticket.value?.id,
    ),
    statusRank: item.status === 1 ? 1 : item.status === 0 ? 2 : 3,
    updatedAt: formatDateText(item.updateTime || item.publishTime || ''),
    statusLabel: toKnowledgeArticleStatusText(item.status, item.statusLabel),
    statusTone: toKnowledgeStatusTone(item.status),
    statusKey: item.status === 1
      ? 'published'
      : item.status === 2
        ? 'archived'
        : 'draft',
  }))
}

async function loadLinkedKnowledgeArticles() {
  if (!ticket.value) {
    linkedKnowledgeArticles.value = []
    return
  }

  const localDraftArticles = listKnowledgeDrafts()
    .map((item) => attachArticleSourceTicket(item))
    .filter((item) => item.sourceTicket?.id === ticket.value?.id)
    .map((item) => ({
      id: item.id,
      title: item.title,
      metric: buildKnowledgeArticleMetric('\u672c\u5730\u8349\u7a3f', item.sourceTicket?.ticketNo, ticket.value?.id),
      statusRank: 2,
      updatedAt: formatDateText(item.updateTime || item.createTime),
      statusLabel: '\u672c\u5730\u8349\u7a3f',
      statusTone: 'chip-orange',
      statusKey: 'local' as const,
    }))

  linkedKnowledgeArticles.value = [...remoteSourceKnowledgeArticles.value, ...localDraftArticles]
}

function syncStatusForm() {
  if (!remoteTicket.value) {
    return
  }
  const currentStatus = statusOptions.find((item) => item.label === remoteTicket.value?.status || toStatusText(item.value) === remoteTicket.value?.status)
  statusForm.value.status = currentStatus?.value ?? 1
}

function syncAssignForm(data: TicketDetailApiItem | null) {
  assignForm.value.assigneeUserId = data?.assigneeUserId ?? 0
}

function resetTransientDetailState() {
  commentError.value = ''
  statusError.value = ''
  assignError.value = ''
  commentErrorTraceId.value = ''
  statusErrorTraceId.value = ''
  assignErrorTraceId.value = ''
  commentForm.value.content = ''
  commentForm.value.commentType = 1
  commentForm.value.internal = false
  statusForm.value.remark = ''
  assignForm.value.remark = ''
  activeCommentFilter.value = 'all'
}

async function loadAssignableUsers() {
  if (!canAssignCurrentTicket.value) {
    assigneeOptions.value = []
    return
  }
  try {
    assigneeOptions.value = await fetchTicketAssignees()
  } catch (error) {
    const result = resolveTicketDetailFallback(error, '处理人列表加载失败，请稍后重试。')
    if (result.mode === 'show-error') {
      assignError.value = result.message
      assignErrorTraceId.value = result.traceId
      assigneeOptions.value = []
      return
    }
    assignError.value = ''
    assignErrorTraceId.value = ''
    assigneeOptions.value = [
      { id: 101, username: 'support01', displayName: '李晓安', roles: ['SUPPORT'] },
      { id: 102, username: 'support02', displayName: '林哲', roles: ['SUPPORT'] },
      { id: 103, username: 'admin', displayName: '系统管理员', roles: ['ADMIN'] },
    ]
    console.error(error)
  }
}

async function loadTicket() {
  const requestId = ++ticketLoadRequestId
  remoteTicket.value = null
  remoteSourceKnowledgeArticles.value = []
  linkedKnowledgeArticles.value = []
  resetTransientDetailState()
  const id = Number(route.params.id)
  if (!id) {
      topErrorMessage.value = '工单 ID 不合法'
    topErrorTraceId.value = ''
    successMessage.value = ''
    return
  }

  topErrorMessage.value = ''
  topErrorTraceId.value = ''
  successMessage.value = route.query.localCreated === '1'
    ? '\u5de5\u5355\u5df2\u4fdd\u5b58\u5230\u672c\u5730\u5de5\u4f5c\u6d41\uff0c\u540e\u7eed\u53ef\u4ee5\u7ee7\u7eed\u8bc4\u8bba\u3001\u6307\u6d3e\u548c\u6539\u72b6\u6001\u3002'
    : route.query.created === '1'
      ? '\u5de5\u5355\u5df2\u521b\u5efa\u6210\u529f\u3002'
      : ''

  const localTicket = getLocalTicket(id)
  if (localTicket) {
    if (requestId !== ticketLoadRequestId) {
      return
    }
    if (!canCurrentUserAccessTicket(localTicket)) {
      await router.replace('/tickets')
      return
    }
    remoteTicket.value = localTicket
    remoteSourceKnowledgeArticles.value = []
    syncStatusForm()
    assignForm.value.assigneeUserId = 0
    await loadLinkedKnowledgeArticles()
    return
  }

  try {
    const data = await fetchTicketDetail(id)
    const formattedTicket = formatTicketDetailItem(data)
    if (requestId !== ticketLoadRequestId) {
      return
    }
    if (!canCurrentUserAccessTicket(formattedTicket)) {
      await router.replace('/tickets')
      return
    }
    remoteTicket.value = formattedTicket
    mapRemoteSourceKnowledgeArticles(data)
    syncStatusForm()
    syncAssignForm(data)
    await loadLinkedKnowledgeArticles()
  } catch (error) {
    if (requestId !== ticketLoadRequestId) {
      return
    }
    remoteTicket.value = null
    remoteSourceKnowledgeArticles.value = []
    linkedKnowledgeArticles.value = []
    const result = resolveTicketDetailFallback(error, '工单详情加载失败，请稍后重试。')
    if (result.mode === 'show-error') {
      topErrorMessage.value = result.message
      topErrorTraceId.value = result.traceId
    }
    console.error(error)
  }
}

async function submitComment() {
  const id = Number(route.params.id)
  const content = commentForm.value.content.trim()
  const commentType = canUseInternalComments.value ? commentForm.value.commentType : 1
  const internal = canUseInternalComments.value ? commentForm.value.internal : false
  if (!id) {
    return
  }
  if (!content) {
    commentError.value = '\u8bf7\u5148\u8f93\u5165\u8bc4\u8bba\u5185\u5bb9'
    commentErrorTraceId.value = ''
    return
  }

  commentSubmitting.value = true
  commentError.value = ''
  commentErrorTraceId.value = ''

  try {
    const localTicket = getLocalTicket(id)
    if (localTicket) {
      remoteTicket.value = appendLocalComment(localTicket, {
        content,
        commentType,
        internal,
      })
      topErrorMessage.value = ''
      topErrorTraceId.value = ''
      remoteSourceKnowledgeArticles.value = []
      commentForm.value.content = ''
      commentForm.value.commentType = 1
      commentForm.value.internal = false
      successMessage.value = buildCommentSuccessMessage(true)
      return
    }

    const data = await addTicketComment(id, {
      content,
      commentType,
      internal,
    })
    remoteTicket.value = formatTicketDetailItem(data)
    topErrorMessage.value = ''
    topErrorTraceId.value = ''
    mapRemoteSourceKnowledgeArticles(data)
    commentForm.value.content = ''
    commentForm.value.commentType = 1
    commentForm.value.internal = false
    syncStatusForm()
    syncAssignForm(data)
    successMessage.value = buildCommentSuccessMessage(false)
  } catch (error) {
    const result = getApiErrorDisplay(error, '评论提交失败')
    commentError.value = result.message
    commentErrorTraceId.value = result.traceId
  } finally {
    commentSubmitting.value = false
  }
}

async function submitStatusUpdate() {
  const id = Number(route.params.id)
  if (!id) {
    return
  }
  if (!canTransitionCurrentTicket.value) {
    statusError.value = '当前账号没有状态流转权限'
    statusErrorTraceId.value = ''
    return
  }

  const shouldSuggestKnowledgeCapture = statusForm.value.status === 4
    && canManageKnowledge.value
    && !linkedKnowledgeArticles.value.length
    && window.confirm('\u5de5\u5355\u4f1a\u88ab\u5173\u95ed\u3002\u8981\u4e0d\u8981\u5728\u5173\u95ed\u540e\u7acb\u523b\u751f\u6210\u4e00\u7bc7\u77e5\u8bc6\u6c89\u6dc0\u8349\u7a3f\uff1f')
  const closeRemark = statusForm.value.remark.trim()

  statusSubmitting.value = true
  statusError.value = ''
  statusErrorTraceId.value = ''

  try {
    const localTicket = getLocalTicket(id)
    if (localTicket) {
      remoteTicket.value = updateLocalTicketStatus(localTicket, statusForm.value.status, statusForm.value.remark.trim())
      topErrorMessage.value = ''
      topErrorTraceId.value = ''
      remoteSourceKnowledgeArticles.value = []
      statusForm.value.remark = ''
      syncStatusForm()
      successMessage.value = buildStatusSuccessMessage(statusForm.value.status, true)
      await loadLinkedKnowledgeArticles()
      if (shouldSuggestKnowledgeCapture) {
        await createKnowledgeDraft({
          origin: 'ticket-close',
          closeRemark,
        })
      }
      return
    }

    const data = await updateTicketStatus(id, {
      status: statusForm.value.status,
      remark: statusForm.value.remark.trim(),
    })
    remoteTicket.value = formatTicketDetailItem(data)
    topErrorMessage.value = ''
    topErrorTraceId.value = ''
    mapRemoteSourceKnowledgeArticles(data)
    statusForm.value.remark = ''
    syncStatusForm()
    syncAssignForm(data)
    successMessage.value = buildStatusSuccessMessage(statusForm.value.status, false)
    await loadLinkedKnowledgeArticles()
    if (shouldSuggestKnowledgeCapture) {
      await createKnowledgeDraft({
        origin: 'ticket-close',
        closeRemark,
      })
    }
  } catch (error) {
    const result = getApiErrorDisplay(error, '状态更新失败')
    statusError.value = result.message
    statusErrorTraceId.value = result.traceId
  } finally {
    statusSubmitting.value = false
  }
}

async function submitAssignment() {
  const id = Number(route.params.id)
  if (!id) {
    return
  }
  if (!canAssignCurrentTicket.value) {
    assignError.value = '当前账号没有指派处理人的权限'
    assignErrorTraceId.value = ''
    return
  }
  if (!assignForm.value.assigneeUserId) {
    assignError.value = '\u8bf7\u5148\u9009\u62e9\u5904\u7406\u4eba'
    assignErrorTraceId.value = ''
    return
  }

  assignSubmitting.value = true
  assignError.value = ''
  assignErrorTraceId.value = ''

  try {
    const localTicket = getLocalTicket(id)
    if (localTicket) {
      const assigneeName = assigneeOptions.value.find((item) => item.id === assignForm.value.assigneeUserId)?.displayName || '\u5df2\u6307\u6d3e\u6210\u5458'
      remoteTicket.value = assignLocalTicket(localTicket, assigneeName, assignForm.value.remark.trim(), assignForm.value.assigneeUserId)
      topErrorMessage.value = ''
      topErrorTraceId.value = ''
      remoteSourceKnowledgeArticles.value = []
      assignForm.value.remark = ''
      successMessage.value = buildAssignmentSuccessMessage(true)
      return
    }

    const data = await assignTicket(id, {
      assigneeUserId: assignForm.value.assigneeUserId,
      remark: assignForm.value.remark.trim(),
    })
    remoteTicket.value = formatTicketDetailItem(data)
    topErrorMessage.value = ''
    topErrorTraceId.value = ''
    mapRemoteSourceKnowledgeArticles(data)
    assignForm.value.remark = ''
    syncStatusForm()
    syncAssignForm(data)
    successMessage.value = buildAssignmentSuccessMessage(false)
  } catch (error) {
    const result = getApiErrorDisplay(error, '指派处理人失败')
    assignError.value = result.message
    assignErrorTraceId.value = result.traceId
  } finally {
    assignSubmitting.value = false
  }
}

async function focusCommentInput() {
  await nextTick()
  commentInputRef.value?.focus()
}

async function focusKnowledgeContext() {
  if (route.query.fromKnowledge !== '1') {
    highlightedSection.value = null
    return
  }

  const focus = route.query.focus
  highlightedSection.value = focus === 'comments' || focus === 'knowledge' || focus === 'timeline' ? focus : 'timeline'
  await nextTick()

  const targetMap = {
    comments: commentsSectionRef.value,
    timeline: timelineSectionRef.value,
    knowledge: knowledgeSectionRef.value,
  }
  targetMap[highlightedSection.value]?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

onMounted(async () => {
  if (!canUseInternalComments.value) {
    commentForm.value.commentType = 1
    commentForm.value.internal = false
    if (activeCommentFilter.value === 'internal') {
      activeCommentFilter.value = 'all'
    }
  }
  await loadAssignableUsers()
  await loadTicket()
  await focusKnowledgeContext()
})

watch(
  () => route.params.id,
  async () => {
    await loadTicket()
    await focusKnowledgeContext()
  },
)

watch(
  () => route.fullPath,
  async () => {
    await focusKnowledgeContext()
  },
)
</script>

<style scoped>
.ticket-form {
  margin-top: 1.25rem;
  padding: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.78);
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 0.875rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-top: 0.85rem;
  color: #475569;
  font-size: 0.92rem;
}

.field:first-child {
  margin-top: 0;
}

.field-control {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  font: inherit;
}

.field-textarea {
  resize: vertical;
  min-height: 108px;
}

.checkbox-field {
  flex-direction: row;
  align-items: center;
  gap: 0.65rem;
}

.checkbox-field input {
  width: 16px;
  height: 16px;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
}

.form-error {
  margin: 0;
  color: #b42318;
  font-size: 0.9rem;
}

.section-highlight-banner {
  margin-bottom: 1rem;
  border-color: rgba(37, 99, 235, 0.18);
  background: rgba(239, 246, 255, 0.92);
}

.comment-toolbar,
.checklist {
  display: grid;
  gap: 0.75rem;
}

.comment-toolbar {
  margin-bottom: 1rem;
}

.section-highlight {
  padding: 0.6rem;
  border-radius: 18px;
  background: rgba(239, 246, 255, 0.62);
}

.context-card-highlight {
  border-color: rgba(37, 99, 235, 0.2);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.08);
  background: rgba(239, 246, 255, 0.8);
}

.knowledge-group {
  display: grid;
  gap: 0.75rem;
}

.knowledge-group + .knowledge-group {
  margin-top: 0.25rem;
}

.knowledge-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: #64748b;
}

.knowledge-group-tools {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.knowledge-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.knowledge-sort-select {
  padding: 0.35rem 0.6rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font: inherit;
}

.quick-filter-chip {
  border: 0;
  cursor: pointer;
  justify-content: center;
}

.chip-default {
  color: #475569;
  background: rgba(148, 163, 184, 0.14);
}

.active-chip {
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.14);
}

.knowledge-capture-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
  margin-bottom: 0;
}

.knowledge-capture-copy {
  display: grid;
  gap: 0.35rem;
}

.knowledge-capture-copy p,
.knowledge-capture-copy strong {
  margin: 0;
}

.knowledge-capture-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.knowledge-summary-box {
  display: grid;
  gap: 0.35rem;
  margin-bottom: 1rem;
}

.knowledge-summary-box strong,
.knowledge-summary-box p {
  margin: 0;
}

.knowledge-workbench-panel {
  display: grid;
  gap: 1rem;
}

.knowledge-workbench-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.knowledge-workbench-card {
  padding: 0.95rem 1rem;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.82);
}

.knowledge-workbench-card strong,
.knowledge-workbench-card p {
  display: block;
  margin-top: 0.35rem;
}

.knowledge-workbench-card p {
  color: #64748b;
  margin-bottom: 0;
}

.knowledge-workbench-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.panel-divider {
  height: 1px;
  margin-bottom: 1rem;
  background: rgba(15, 23, 42, 0.08);
}

.inline-link-button {
  text-decoration: none;
}

.ticket-insight-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-top: 1rem;
}

.insight-card {
  padding: 0.95rem 1rem;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.88);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.insight-card strong {
  display: block;
  margin-top: 0.4rem;
}

.timeline-highlight {
  border-color: rgba(37, 99, 235, 0.18);
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.8), rgba(255, 255, 255, 0.96));
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 0.95rem;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px solid rgba(15, 23, 42, 0.08);
  color: #334155;
}

.checklist-item input {
  width: 16px;
  height: 16px;
}

.checklist-summary {
  margin-top: 1rem;
  margin-bottom: 0;
}

.related-link {
  display: grid;
  gap: 0.55rem;
  border-radius: 14px;
  padding: 0.85rem 0.95rem;
  transition: background 180ms ease, transform 180ms ease;
}

.related-link:hover {
  background: rgba(37, 99, 235, 0.06);
  transform: translateX(2px);
}

.related-link-main {
  display: grid;
  gap: 0.2rem;
}

.related-link-main strong {
  color: #0f172a;
}

.knowledge-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.knowledge-empty-hint {
  color: #64748b;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .knowledge-capture-box {
    flex-direction: column;
    align-items: stretch;
  }

  .knowledge-workbench-grid {
    grid-template-columns: 1fr;
  }

  .ticket-insight-strip {
    grid-template-columns: 1fr;
  }
}
</style>
