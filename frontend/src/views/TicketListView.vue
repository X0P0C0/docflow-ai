<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />

    <main class="main-content">
      <AppTopbar />

      <section class="ticket-page">
        <section class="ticket-hero">
          <div class="ticket-hero__copy">
            <el-tag effect="plain" round type="primary">Ticket Workspace</el-tag>
            <div class="ticket-hero__heading">
              <h2>工单工作台</h2>
              <p>
                把待处理、跟进中、已解决和待沉淀知识的工单放在同一张工作台里，
                用统一筛选和更清晰的列表结构来管理日常协作。
              </p>
            </div>
            <div class="ticket-hero__actions">
              <el-button plain @click="toggleFilters">
                {{ showFilters ? '收起筛选' : '展开筛选' }}
              </el-button>
              <el-button type="primary" @click="navigateTo('/tickets/create')">
                新建工单
              </el-button>
            </div>
          </div>

          <div class="ticket-stats">
            <el-card
              v-for="stat in statCards"
              :key="stat.label"
              shadow="hover"
              class="ticket-stat-card"
            >
              <span class="ticket-stat-card__label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.description }}</p>
            </el-card>
          </div>
        </section>

        <el-card shadow="never" class="ticket-toolbar-card">
          <div class="ticket-toolbar-card__content">
            <el-alert
              :closable="false"
              type="info"
              show-icon
              class="ticket-scope-alert"
              :title="ticketScopeMessage"
            />

            <div class="ticket-toolbar">
              <div class="ticket-toolbar__group">
                <span class="ticket-toolbar__label">快速筛选</span>
                <el-radio-group v-model="activeQuickFilter" class="ticket-filter-switch">
                  <el-radio-button
                    v-for="filter in quickFilterOptions"
                    :key="filter.value"
                    :value="filter.value"
                  >
                    {{ filter.label }}
                  </el-radio-button>
                </el-radio-group>
              </div>

              <div class="ticket-toolbar__group ticket-toolbar__group--end">
                <span class="ticket-toolbar__label">展示模式</span>
                <el-radio-group v-model="viewMode">
                  <el-radio-button value="board">看板视图</el-radio-button>
                  <el-radio-button value="compact">表格视图</el-radio-button>
                </el-radio-group>
              </div>
            </div>

            <el-form
              v-if="showFilters"
              class="ticket-filter-form"
              label-position="top"
              @submit.prevent="handleSearch"
            >
              <div class="ticket-filter-form__grid">
                <el-form-item label="关键词" class="ticket-filter-form__item ticket-filter-form__item--wide">
                  <el-input
                    v-model="filters.keyword"
                    clearable
                    placeholder="按工单标题、编号或内容搜索"
                    @keyup.enter="handleSearch"
                  />
                </el-form-item>

                <el-form-item label="状态" class="ticket-filter-form__item">
                  <el-select v-model="filters.status" clearable placeholder="全部状态">
                    <el-option
                      v-for="option in statusOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="优先级" class="ticket-filter-form__item">
                  <el-select v-model="filters.priority" clearable placeholder="全部优先级">
                    <el-option
                      v-for="option in priorityOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item label="类型" class="ticket-filter-form__item">
                  <el-select v-model="filters.type" clearable placeholder="全部类型">
                    <el-option
                      v-for="option in typeOptions"
                      :key="option.value"
                      :label="option.label"
                      :value="option.value"
                    />
                  </el-select>
                </el-form-item>

                <div class="ticket-filter-form__actions">
                  <el-button @click="resetFilters">重置</el-button>
                  <el-button type="primary" native-type="submit">应用筛选</el-button>
                </div>
              </div>
            </el-form>
          </div>
        </el-card>

        <el-card shadow="never" class="ticket-list-card">
          <div class="ticket-list-card__head">
            <div>
              <h3>工单列表</h3>
              <p>用统一的列表骨架查看当前可见工单，并在看板与表格之间切换。</p>
            </div>
            <el-tag type="primary" effect="light" round>可见 {{ displayedTickets.length }}</el-tag>
          </div>

          <div class="ticket-summary">
            <el-tag effect="plain" round>已显示 {{ displayedTickets.length }} 条</el-tag>
            <el-tag effect="plain" round>全部 {{ tickets.length }} 条</el-tag>
            <el-tag effect="plain" round type="warning">待处理 {{ processingCount }}</el-tag>
            <el-tag effect="plain" round type="danger">待沉淀 {{ knowledgeGapCount }}</el-tag>
            <el-tag v-if="localTicketCount" effect="plain" round type="info">本地草稿 {{ localTicketCount }}</el-tag>
            <el-tag v-if="loading" effect="light" round type="info">正在加载...</el-tag>
            <ErrorTraceNotice
              v-else-if="errorMessage"
              inline
              :message="`${errorMessage}${usedFallbackData ? '，当前展示的是可用兜底数据。' : ''}`"
              :trace-id="errorTraceId"
            />
          </div>

          <div v-if="displayedTickets.length">
            <div v-if="viewMode === 'board'" class="ticket-board">
              <RouterLink
                v-for="ticket in displayedTickets"
                :key="ticket.id"
                class="ticket-board-link"
                :to="`/tickets/${ticket.id}`"
              >
                <el-card shadow="hover" class="ticket-board-card">
                  <div class="ticket-board-card__head">
                    <div>
                      <strong>{{ ticket.title }}</strong>
                      <p>{{ ticket.ticketNo }} · {{ ticket.assignee || '待分配' }}</p>
                    </div>
                    <el-tag :type="priorityTagType(ticketPriorityLabel(ticket))" effect="light" round>
                      {{ ticketPriorityLabel(ticket) }}
                    </el-tag>
                  </div>

                  <p class="ticket-board-card__content">{{ ticket.content }}</p>

                  <div class="ticket-board-card__meta">
                    <el-tag :type="statusTagType(ticket.status)" effect="light" round>
                      {{ ticket.status }}
                    </el-tag>
                    <span>提交人 {{ ticket.submitter }}</span>
                    <span>更新于 {{ ticket.updatedAt }}</span>
                  </div>

                  <div class="ticket-board-card__tags">
                    <el-tag
                      v-for="tag in ticket.tags"
                      :key="tag"
                      effect="plain"
                      round
                    >
                      {{ tag }}
                    </el-tag>
                    <el-tag
                      v-if="ticket.linkedKnowledgeArticleCount"
                      effect="plain"
                      round
                      type="primary"
                    >
                      已关联知识 {{ ticket.linkedKnowledgeArticleCount }} 篇
                    </el-tag>
                    <el-tag
                      v-else-if="needsKnowledgeCapture(ticket)"
                      effect="plain"
                      round
                      type="danger"
                    >
                      待沉淀知识
                    </el-tag>
                    <el-tag
                      v-if="ticket.source === 'local'"
                      effect="plain"
                      round
                      type="warning"
                    >
                      本地草稿
                    </el-tag>
                  </div>

                  <div v-if="ticket.latestLinkedKnowledgeArticle" class="ticket-board-card__knowledge">
                    <span>最新关联知识</span>
                    <strong>{{ ticket.latestLinkedKnowledgeArticle.title }}</strong>
                    <p>
                      {{ ticket.latestLinkedKnowledgeArticle.status }}
                      · {{ ticket.latestLinkedKnowledgeArticle.updatedAt }}
                    </p>
                  </div>
                </el-card>
              </RouterLink>
            </div>

            <el-table
              v-else
              :data="displayedTickets"
              stripe
              class="ticket-table"
              @row-click="handleRowClick"
            >
              <el-table-column label="工单" min-width="280">
                <template #default="{ row }">
                  <div class="ticket-table-cell">
                    <strong>{{ row.title }}</strong>
                    <p>{{ row.ticketNo }} · {{ row.content }}</p>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="状态" width="140">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light" round>
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="优先级" width="120">
                <template #default="{ row }">
                    <el-tag :type="priorityTagType(ticketPriorityLabel(row))" effect="light" round>
                      {{ ticketPriorityLabel(row) }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="负责人" width="160">
                <template #default="{ row }">
                  {{ row.assignee || '待分配' }}
                </template>
              </el-table-column>

              <el-table-column label="知识沉淀" min-width="180">
                <template #default="{ row }">
                  <el-tag
                    v-if="row.linkedKnowledgeArticleCount"
                    type="primary"
                    effect="plain"
                    round
                  >
                    已关联 {{ row.linkedKnowledgeArticleCount }} 篇
                  </el-tag>
                  <el-tag
                    v-else-if="needsKnowledgeCapture(row)"
                    type="danger"
                    effect="plain"
                    round
                  >
                    待沉淀知识
                  </el-tag>
                  <el-tag
                    v-else-if="row.source === 'local'"
                    type="warning"
                    effect="plain"
                    round
                  >
                    本地草稿
                  </el-tag>
                  <span v-else class="ticket-table-muted">暂无</span>
                </template>
              </el-table-column>

              <el-table-column label="更新时间" width="180">
                <template #default="{ row }">
                  {{ row.updatedAt }}
                </template>
              </el-table-column>

              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="navigateTo(`/tickets/${row.id}`)">
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <el-empty v-else description="暂无符合条件的工单" />
        </el-card>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchTickets, type TicketApiItem } from '../api/ticket'
import { buildTicketListScopeMessage } from '../access-policy'
import { authState } from '../auth'
import { canViewAllTickets } from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopbar from '../components/layout/AppTopbar.vue'
import { manageNav, tickets as fallbackTickets, workspaceNav } from '../mock/dashboard'
import { listLocalTickets, mergeTickets } from '../mock/ticketWorkspace'
import type { TicketItem } from '../types/dashboard'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { countArticlesBySourceTicket } from '../utils/knowledgeSourceTicket'
import { formatTicketListItem } from '../utils/ticketPresentation'

type QuickFilter = 'all' | 'urgent' | 'mine' | 'resolved' | 'knowledge-gap' | 'local'
type ViewMode = 'board' | 'compact'

const router = useRouter()
const route = useRoute()
const tickets = ref<TicketItem[]>(fallbackTickets)
const showFilters = ref(true)
const loading = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')
const usedFallbackData = ref(false)
let ticketLoadRequestId = 0
let skipNextRouteDrivenLoad = false
let skipNextQuickFilterRefresh = false
let skipNextViewModeRefresh = false

const filters = reactive({
  keyword: '',
  status: '',
  priority: '',
  type: '',
})

const activeQuickFilter = ref<QuickFilter>('all')
const viewMode = ref<ViewMode>('board')

const canSeeAllTickets = computed(() => canViewAllTickets())
const currentUserId = computed(() => authState.user?.id ?? null)
const currentUserName = computed(() => (
  authState.user?.nickname
  || authState.user?.realName
  || authState.user?.username
  || '当前用户'
))

const statusOptions = [
  { value: '1', label: '待处理' },
  { value: '2', label: '处理中' },
  { value: '3', label: '已解决' },
  { value: '4', label: '已关闭' },
]

const priorityOptions = [
  { value: '4', label: 'P1 紧急' },
  { value: '3', label: 'P2 较高' },
  { value: '2', label: 'P3 普通' },
  { value: '1', label: 'P4 低优先级' },
]

const typeOptions = [
  { value: 'INCIDENT', label: '故障事件' },
  { value: 'TASK', label: '任务处理' },
  { value: 'QUESTION', label: '咨询问题' },
]

const processingCount = computed(() => (
  tickets.value.filter((item) => ['待处理', '处理中', '待确认'].includes(item.status)).length
))

const localTicketCount = computed(() => (
  tickets.value.filter((item) => item.source === 'local').length
))

const knowledgeGapCount = computed(() => (
  tickets.value.filter((item) => needsKnowledgeCapture(item)).length
))

const ticketScopeMessage = computed(() => buildTicketListScopeMessage({
  canViewAllTickets: canSeeAllTickets.value,
}))

const quickFilters = [
  { value: 'all', label: '全部工单' },
  { value: 'urgent', label: '高优先级' },
  { value: 'mine', label: '与我相关' },
  { value: 'resolved', label: '已解决' },
  { value: 'knowledge-gap', label: '待沉淀知识' },
  { value: 'local', label: '本地草稿' },
] as const satisfies ReadonlyArray<{ value: QuickFilter; label: string }>

const quickFilterOptions = computed(() => quickFilters.map((item) => (
  item.value === 'mine'
    ? {
        ...item,
        label: canSeeAllTickets.value ? '与我相关' : '我提交的工单',
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

const statCards = computed(() => [
  {
    label: '当前工单总数',
    value: tickets.value.length,
    description: '当前权限范围内可见的全部工单规模。',
  },
  {
    label: '待处理队列',
    value: processingCount.value,
    description: '仍需跟进、处理或确认的工单数量。',
  },
  {
    label: '本地草稿',
    value: localTicketCount.value,
    description: '仅保存在本地工作流里的临时工单数据。',
  },
  {
    label: '待沉淀知识',
    value: knowledgeGapCount.value,
    description: '已解决或已关闭但还没有知识沉淀的工单。',
  },
])

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

function statusTagType(status: string) {
  if (status === '已解决') {
    return 'success'
  }
  if (status === '已关闭') {
    return 'info'
  }
  return 'warning'
}

function priorityTagType(priorityLevel: string) {
  if (priorityLevel === 'P1') {
    return 'danger'
  }
  if (priorityLevel === 'P2') {
    return 'warning'
  }
  if (priorityLevel === 'P3') {
    return 'primary'
  }
  return 'info'
}

function ticketPriorityLabel(ticket: TicketItem) {
  return ticket.priorityLevel || ticket.priority || '未分级'
}

function toggleFilters() {
  showFilters.value = !showFilters.value
}

function navigateTo(path: string) {
  if (route.path === path) {
    return
  }
  router.push(path)
}

function handleRowClick(row: TicketItem) {
  navigateTo(`/tickets/${row.id}`)
}

function syncFiltersFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.status = typeof route.query.status === 'string' ? route.query.status : ''
  filters.priority = typeof route.query.priority === 'string' ? route.query.priority : ''
  filters.type = typeof route.query.type === 'string' ? route.query.type : ''

  skipNextQuickFilterRefresh = true
  activeQuickFilter.value = typeof route.query.quickFilter === 'string'
    && quickFilters.some((item) => item.value === route.query.quickFilter)
      ? route.query.quickFilter as QuickFilter
      : 'all'

  skipNextViewModeRefresh = true
  viewMode.value = route.query.viewMode === 'compact' ? 'compact' : 'board'
}

function updateRouteQuery() {
  router.replace({
    query: {
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      priority: filters.priority || undefined,
      type: filters.type || undefined,
      quickFilter: activeQuickFilter.value !== 'all' ? activeQuickFilter.value : undefined,
      viewMode: viewMode.value !== 'board' ? viewMode.value : undefined,
    },
  })
}

function refreshFromLocalState() {
  skipNextRouteDrivenLoad = true
  updateRouteQuery()
  loadTickets()
}

function handleSearch() {
  refreshFromLocalState()
}

function resetFilters() {
  filters.keyword = ''
  filters.status = ''
  filters.priority = ''
  filters.type = ''
  activeQuickFilter.value = 'all'
  refreshFromLocalState()
}

async function loadTickets() {
  const requestId = ++ticketLoadRequestId
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

    if (requestId !== ticketLoadRequestId) {
      return
    }

    await syncKnowledgeArticleCounts(mergeTickets(data.map(formatTicketListItem as (ticket: TicketApiItem) => TicketItem)))
  } catch (error) {
    if (requestId !== ticketLoadRequestId) {
      return
    }

    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: '工单列表暂时不可用，已切换为可用兜底数据。',
      defaultMessage: '工单列表加载失败，请稍后重试或联系管理员。',
    })

    await syncKnowledgeArticleCounts(result.shouldUseFallbackData ? mergeTickets(fallbackTickets) : listLocalTickets())
    usedFallbackData.value = result.shouldUseFallbackData
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    console.error(error)
  } finally {
    if (requestId === ticketLoadRequestId) {
      loading.value = false
    }
  }
}

onMounted(async () => {
  syncFiltersFromRoute()
  await loadTickets()
})

watch(activeQuickFilter, () => {
  if (skipNextQuickFilterRefresh) {
    skipNextQuickFilterRefresh = false
    return
  }
  refreshFromLocalState()
})

watch(viewMode, () => {
  if (skipNextViewModeRefresh) {
    skipNextViewModeRefresh = false
    return
  }
  skipNextRouteDrivenLoad = true
  updateRouteQuery()
})

watch(
  () => route.fullPath,
  async () => {
    if (skipNextRouteDrivenLoad) {
      skipNextRouteDrivenLoad = false
      return
    }
    syncFiltersFromRoute()
    await loadTickets()
  },
)
</script>

<style scoped>
.ticket-page {
  display: grid;
  gap: 20px;
}

.ticket-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 20px;
  align-items: stretch;
}

.ticket-hero__copy,
.ticket-toolbar-card__content {
  display: grid;
  gap: 16px;
}

.ticket-hero__copy {
  padding: 28px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(239, 246, 255, 0.94));
  border: 1px solid rgba(255, 255, 255, 0.86);
  box-shadow: var(--shadow);
}

.ticket-hero__heading {
  display: grid;
  gap: 12px;
}

.ticket-hero__heading h2 {
  margin: 0;
  font-size: 38px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.ticket-hero__heading p {
  margin: 0;
  max-width: 56ch;
  color: var(--text-muted);
  line-height: 1.85;
}

.ticket-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.ticket-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.ticket-stat-card {
  height: 100%;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.ticket-stat-card :deep(.el-card__body) {
  display: grid;
  gap: 8px;
}

.ticket-stat-card__label {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 600;
}

.ticket-stat-card strong {
  font-size: 32px;
  letter-spacing: -0.04em;
}

.ticket-stat-card p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.75;
}

.ticket-toolbar-card,
.ticket-list-card {
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.82);
}

.ticket-scope-alert {
  border-radius: 16px;
}

.ticket-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.ticket-toolbar__group {
  display: grid;
  gap: 10px;
}

.ticket-toolbar__group--end {
  justify-items: end;
}

.ticket-toolbar__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-soft);
}

.ticket-filter-switch {
  display: flex;
  flex-wrap: wrap;
}

.ticket-filter-form {
  padding-top: 4px;
}

.ticket-filter-form__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: end;
}

.ticket-filter-form__item {
  margin-bottom: 0;
}

.ticket-filter-form__item--wide {
  grid-column: span 1;
}

.ticket-filter-form__actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  padding-bottom: 2px;
}

.ticket-list-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.ticket-list-card__head h3 {
  margin: 0 0 6px;
}

.ticket-list-card__head p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.75;
}

.ticket-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 18px;
}

.ticket-board {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ticket-board-link {
  display: block;
}

.ticket-board-card {
  height: 100%;
}

.ticket-board-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.ticket-board-card__head strong,
.ticket-board-card__knowledge strong,
.ticket-table-cell strong {
  display: block;
}

.ticket-board-card__head p,
.ticket-board-card__content,
.ticket-board-card__knowledge p,
.ticket-table-cell p {
  margin: 0;
  color: var(--text-muted);
  line-height: 1.75;
}

.ticket-board-card__content {
  margin-top: 14px;
}

.ticket-board-card__meta,
.ticket-board-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
  color: var(--text-muted);
  font-size: 13px;
}

.ticket-board-card__knowledge {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.ticket-board-card__knowledge span {
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.ticket-table {
  width: 100%;
}

.ticket-table-cell {
  display: grid;
  gap: 6px;
}

.ticket-table-muted {
  color: var(--text-soft);
}

@media (max-width: 1240px) {
  .ticket-hero,
  .ticket-board {
    grid-template-columns: 1fr;
  }

  .ticket-filter-form__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .ticket-hero__copy {
    padding: 24px;
  }

  .ticket-hero__heading h2 {
    font-size: 30px;
  }

  .ticket-stats,
  .ticket-filter-form__grid {
    grid-template-columns: 1fr;
  }

  .ticket-toolbar__group--end {
    justify-items: start;
  }

  .ticket-filter-form__actions,
  .ticket-list-card__head {
    justify-content: flex-start;
  }
}
</style>
