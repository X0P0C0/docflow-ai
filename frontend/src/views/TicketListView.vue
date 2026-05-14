<template>
  <div class="detail-page">
    <header class="detail-topbar">
      <RouterLink class="back-link" to="/dashboard">返回控制台</RouterLink>
      <div class="detail-topbar-actions">
        <button class="ghost-button" type="button" @click="toggleFilters">
          {{ showFilters ? '收起筛选' : '筛选工单' }}
        </button>
        <button class="primary-button" type="button" @click="router.push('/tickets/create')">新建工单</button>
      </div>
    </header>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h3>工单中心</h3>
          <p>这里会逐步演进成真正的工单列表、筛选、搜索和状态流转页面</p>
        </div>
        <span class="chip chip-blue">Ticket Workspace</span>
      </div>

      <form v-if="showFilters" class="ticket-filter-bar" @submit.prevent="loadTickets">
        <input
          v-model="filters.keyword"
          class="field-control"
          type="text"
          placeholder="搜索工单标题、编号或描述"
        />
        <select v-model="filters.status" class="field-control">
          <option value="">全部状态</option>
          <option value="1">新建</option>
          <option value="2">处理中</option>
          <option value="3">已解决</option>
          <option value="4">已关闭</option>
        </select>
        <select v-model="filters.priority" class="field-control">
          <option value="">全部优先级</option>
          <option value="4">P1 · 紧急</option>
          <option value="3">P2 · 高优先级</option>
          <option value="2">P3 · 普通</option>
          <option value="1">P4 · 低优先级</option>
        </select>
        <select v-model="filters.type" class="field-control">
          <option value="">全部类型</option>
          <option value="INCIDENT">故障事件</option>
          <option value="TASK">处理任务</option>
          <option value="QUESTION">问题咨询</option>
        </select>
        <div class="filter-actions">
          <button class="ghost-button" type="button" @click="resetFilters">重置</button>
          <button class="primary-button" type="submit">应用筛选</button>
        </div>
      </form>

      <div class="ticket-filter-summary">
        <span>当前工单数：{{ tickets.length }}</span>
        <span>待处理：{{ processingCount }}</span>
        <span>本地工单：{{ localTicketCount }}</span>
        <span>待沉淀：{{ knowledgeGapCount }}</span>
        <span v-if="loading">正在拉取最新列表...</span>
        <ErrorTraceNotice
          v-else-if="errorMessage"
          inline
          :message="`${errorMessage}${usedFallbackData ? '，已回退到本地演示数据' : ''}`"
          :trace-id="errorTraceId"
        />
      </div>

      <div class="state-box ticket-scope-banner">
        {{ ticketScopeMessage }}
      </div>

      <div class="ticket-quick-filters">
        <button
          v-for="filter in quickFilterOptions"
          :key="filter.value"
          class="chip quick-filter-chip"
          :class="activeQuickFilter === filter.value ? 'chip-blue active-chip' : 'chip-default'"
          type="button"
          @click="activeQuickFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>

      <div class="ticket-view-toggle">
        <button
          class="ghost-button"
          :class="{ 'toggle-active': viewMode === 'board' }"
          type="button"
          @click="viewMode = 'board'"
        >
          卡片视图
        </button>
        <button
          class="ghost-button"
          :class="{ 'toggle-active': viewMode === 'compact' }"
          type="button"
          @click="viewMode = 'compact'"
        >
          紧凑视图
        </button>
      </div>

      <div v-if="displayedTickets.length && viewMode === 'board'" class="ticket-board">
        <RouterLink v-for="ticket in displayedTickets" :key="ticket.id" class="ticket-board-item" :to="`/tickets/${ticket.id}`">
          <div class="ticket-board-head">
            <div>
              <strong>{{ ticket.title }}</strong>
              <p>{{ ticket.ticketNo }} · {{ ticket.assignee }}</p>
            </div>
            <span class="chip" :class="ticket.priorityClass">{{ ticket.priorityLevel }}</span>
          </div>
          <p class="ticket-board-content">{{ ticket.content }}</p>
          <div class="ticket-board-meta">
            <span class="chip" :class="statusChipClass(ticket.status)">{{ ticket.status }}</span>
            <span>提交人：{{ ticket.submitter }}</span>
            <span>更新时间：{{ ticket.updatedAt }}</span>
            <span v-if="ticket.linkedKnowledgeArticleCount" class="chip chip-blue">已沉淀 {{ ticket.linkedKnowledgeArticleCount }} 篇</span>
            <span v-else-if="needsKnowledgeCapture(ticket)" class="chip chip-orange">待沉淀知识</span>
            <span v-if="'source' in ticket && ticket.source === 'local'" class="chip chip-orange">本地工作流</span>
          </div>
          <div class="ticket-board-tags">
            <span v-for="tag in ticket.tags" :key="tag" class="chip chip-blue">{{ tag }}</span>
          </div>
          <div v-if="ticket.latestLinkedKnowledgeArticle" class="ticket-knowledge-preview">
            <span class="muted">最近沉淀</span>
            <strong>{{ ticket.latestLinkedKnowledgeArticle.title }}</strong>
            <p>{{ ticket.latestLinkedKnowledgeArticle.status }} · {{ ticket.latestLinkedKnowledgeArticle.updatedAt }}</p>
          </div>
        </RouterLink>
      </div>
      <div v-else-if="displayedTickets.length && viewMode === 'compact'" class="ticket-compact-list">
        <RouterLink v-for="ticket in displayedTickets" :key="ticket.id" class="ticket-compact-item" :to="`/tickets/${ticket.id}`">
          <div class="ticket-compact-main">
            <strong>{{ ticket.title }}</strong>
            <p>{{ ticket.ticketNo }} · {{ ticket.content }}</p>
          </div>
          <div class="ticket-compact-side">
            <span class="chip" :class="ticket.priorityClass">{{ ticket.priorityLevel }}</span>
            <span class="chip" :class="statusChipClass(ticket.status)">{{ ticket.status }}</span>
            <span v-if="ticket.linkedKnowledgeArticleCount" class="chip chip-blue">知识 {{ ticket.linkedKnowledgeArticleCount }}</span>
            <span v-else-if="needsKnowledgeCapture(ticket)" class="chip chip-orange">待沉淀</span>
          </div>
        </RouterLink>
      </div>
      <div v-else class="state-box">当前筛选条件下没有匹配的工单。</div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchTickets, type TicketApiItem } from '../api/ticket'
import { buildTicketListScopeMessage } from '../access-policy'
import { authState } from '../auth'
import { canViewAllTickets } from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import { tickets as fallbackTickets } from '../mock/dashboard'
import { mergeTickets } from '../mock/ticketWorkspace'
import type { TicketItem } from '../types/dashboard'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { countArticlesBySourceTicket } from '../utils/knowledgeSourceTicket'
import { formatTicketListItem } from '../utils/ticketPresentation'

const router = useRouter()
const tickets = ref<TicketItem[]>(fallbackTickets)
const showFilters = ref(true)
const loading = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')
const usedFallbackData = ref(false)
const filters = reactive({
  keyword: '',
  status: '',
  priority: '',
  type: '',
})
const activeQuickFilter = ref<'all' | 'urgent' | 'mine' | 'resolved' | 'knowledge-gap' | 'local'>('all')
const viewMode = ref<'board' | 'compact'>('board')
const canSeeAllTickets = computed(() => canViewAllTickets())
const currentUserId = computed(() => authState.user?.id ?? null)
const currentUserName = computed(() => authState.user?.nickname || authState.user?.realName || authState.user?.username || '当前用户')

const processingCount = computed(() => tickets.value.filter((item) => ['新建', '处理中', '排查中'].includes(item.status)).length)
const localTicketCount = computed(() => tickets.value.filter((item) => 'source' in item && item.source === 'local').length)
const knowledgeGapCount = computed(() => tickets.value.filter((item) => needsKnowledgeCapture(item)).length)
const ticketScopeMessage = computed(() => buildTicketListScopeMessage({
  canViewAllTickets: canSeeAllTickets.value,
}))
const quickFilters = [
  { value: 'all', label: '全部' },
  { value: 'urgent', label: '高优先级' },
  { value: 'mine', label: '待我处理' },
  { value: 'resolved', label: '已解决' },
  { value: 'knowledge-gap', label: '待沉淀知识' },
  { value: 'local', label: '本地工单' },
] as const
const quickFilterOptions = computed(() => quickFilters.map((item) => (
  item.value === 'mine'
    ? {
        ...item,
        label: canSeeAllTickets.value ? '待我处理' : '我的工单',
      }
    : item
)))
const displayedTickets = computed(() => {
  if (activeQuickFilter.value === 'urgent') {
    return tickets.value.filter((item) => item.priorityLevel === 'P1' || item.priorityLevel === 'P2')
  }
  if (activeQuickFilter.value === 'mine') {
    return tickets.value.filter((item) => isMyTicket(item))
  }
  if (activeQuickFilter.value === 'resolved') {
    return tickets.value.filter((item) => item.status === '已解决')
  }
  if (activeQuickFilter.value === 'knowledge-gap') {
    return tickets.value.filter((item) => needsKnowledgeCapture(item))
  }
  if (activeQuickFilter.value === 'local') {
    return tickets.value.filter((item) => item.source === 'local')
  }
  return tickets.value
})

function isMyTicket(ticket: TicketItem) {
  const currentId = currentUserId.value
  const currentName = currentUserName.value
  if (canSeeAllTickets.value) {
    return ticket.assigneeUserId === currentId || ticket.assignee === currentName
  }
  return ticket.submitUserId === currentId
    || ticket.submitter === currentName
    || ticket.assigneeUserId === currentId
    || ticket.assignee === currentName
}

function applyRoleScopedTickets(items: TicketItem[]) {
  if (canSeeAllTickets.value) {
    return items
  }
  return items.filter((item) => isMyTicket(item))
}

function enrichTicket(ticket: TicketItem) {
  return {
    ...ticket,
    linkedKnowledgeArticleCount: ticket.linkedKnowledgeArticleCount ?? 0,
  }
}

function needsKnowledgeCapture(ticket: TicketItem) {
  return ['已解决', '已关闭'].includes(ticket.status) && !ticket.linkedKnowledgeArticleCount
}

async function syncKnowledgeArticleCounts(items: TicketItem[]) {
  const scopedItems = applyRoleScopedTickets(items)
  if (!scopedItems.length) {
    tickets.value = scopedItems
    return
  }

  tickets.value = scopedItems.map((item) => {
    const localCount = countArticlesBySourceTicket(item.id)
    const remoteCount = item.linkedKnowledgeArticleCount ?? 0
    return enrichTicket({
      ...item,
      linkedKnowledgeArticleCount: Math.max(remoteCount, localCount),
    })
  })
}

function statusChipClass(status: string) {
  if (status === '已解决') {
    return 'chip-green'
  }
  if (status === '已关闭') {
    return 'chip-blue'
  }
  return 'chip-orange'
}

function toggleFilters() {
  showFilters.value = !showFilters.value
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.priority = ''
  filters.type = ''
  activeQuickFilter.value = 'all'
  loadTickets()
}

async function loadTickets() {
  loading.value = true
  errorMessage.value = ''
  errorTraceId.value = ''
  usedFallbackData.value = false
  try {
    const data = await fetchTickets({
      keyword: filters.keyword.trim() || undefined,
      status: filters.status ? Number(filters.status) : undefined,
      priority: filters.priority ? Number(filters.priority) : undefined,
      type: filters.type || undefined,
    })
    await syncKnowledgeArticleCounts(mergeTickets(data.map(formatTicketListItem as (ticket: TicketApiItem) => TicketItem)))
  } catch (error) {
    await syncKnowledgeArticleCounts(mergeTickets(fallbackTickets))
    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: '筛选接口暂时不可用',
      defaultMessage: '工单列表加载失败，请稍后重试。',
    })
    usedFallbackData.value = result.shouldUseFallbackData
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadTickets()
})
</script>

<style scoped>
.ticket-filter-bar {
  display: grid;
  grid-template-columns: 1.6fr repeat(3, minmax(0, 1fr)) auto;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.ticket-filter-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  color: #64748b;
  font-size: 0.92rem;
}

.ticket-scope-banner {
  margin-bottom: 1rem;
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

.filter-actions {
  display: flex;
  gap: 0.75rem;
}

.ticket-quick-filters,
.ticket-view-toggle {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.quick-filter-chip {
  border: 0;
  cursor: pointer;
}

.chip-default {
  color: #475569;
  background: rgba(148, 163, 184, 0.14);
}

.active-chip,
.toggle-active {
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.14);
}

.ticket-compact-list {
  display: grid;
  gap: 0.85rem;
}

.ticket-compact-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.1rem;
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.ticket-compact-item:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.ticket-compact-main strong,
.ticket-compact-main p {
  display: block;
  margin: 0;
}

.ticket-compact-main p {
  margin-top: 0.45rem;
  color: #64748b;
  line-height: 1.7;
}

.ticket-compact-side {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ticket-knowledge-preview {
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.ticket-knowledge-preview strong,
.ticket-knowledge-preview p {
  display: block;
  margin: 0.2rem 0 0;
}

.ticket-knowledge-preview p {
  color: #64748b;
}

@media (max-width: 1100px) {
  .ticket-filter-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .ticket-filter-bar {
    grid-template-columns: 1fr;
  }

  .ticket-filter-summary,
  .filter-actions,
  .ticket-compact-item {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
