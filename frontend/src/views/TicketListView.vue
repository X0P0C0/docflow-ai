<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
      <section class="ticket-page workspace-page">
        <el-card shadow="never" class="ticket-header-card workspace-card">
          <div class="ticket-header workspace-header">
            <div class="ticket-header__main workspace-header-main">
              <div class="ticket-header__title workspace-title">
                <h2>工单中心</h2>
                <p>集中查看当前权限范围内的工单，统一处理筛选、跟进和知识沉淀。</p>
              </div>
              <div class="ticket-header__actions workspace-actions">
                <el-button @click="toggleFilters">
                  {{ showFilters ? '收起筛选' : '展开筛选' }}
                </el-button>
                <el-button type="primary" @click="navigateTo('/tickets/create')">
                  新建工单
                </el-button>
              </div>
            </div>

            <div class="ticket-stats workspace-stats">
              <div
                v-for="stat in statCards"
                :key="stat.label"
                class="ticket-stat workspace-stat"
              >
                <span class="ticket-stat__label workspace-stat-label">{{ stat.label }}</span>
                <strong>{{ stat.value }}</strong>
                <p>{{ stat.description }}</p>
              </div>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="ticket-filter-card workspace-card">
          <div class="ticket-filter-card__head workspace-section-head">
            <div class="workspace-section-title">
              <h3>查询条件</h3>
              <p>{{ ticketScopeMessage }}</p>
            </div>
            <div class="ticket-filter-card__mode workspace-section-mode">
              <span>显示模式</span>
              <el-segmented
                v-model="viewMode"
                :options="viewModeOptions"
              />
            </div>
          </div>

          <div class="state-box ticket-runtime-banner" :class="{ 'state-warning': isDemoMode() || usedFallbackData }">
            <strong>{{ runtimeHeadline }} · {{ runtimeModeText }}</strong>
            <p>{{ runtimeDataSourceMessage }}</p>
          </div>

          <el-form
            v-if="showFilters"
            class="ticket-filter-form workspace-filter-form"
            label-position="top"
            @submit.prevent="handleSearch"
          >
            <div class="ticket-filter-form__grid">
              <el-form-item label="关键词" class="ticket-filter-form__item ticket-filter-form__item--keyword">
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
            </div>

            <div class="ticket-filter-form__footer workspace-filter-footer">
              <div class="ticket-quick-filters">
                <span class="ticket-section-label workspace-section-label">快速筛选</span>
                <el-radio-group v-model="activeQuickFilter">
                  <el-radio-button
                    v-for="filter in quickFilterOptions"
                    :key="filter.value"
                    :value="filter.value"
                  >
                    {{ filter.label }}
                  </el-radio-button>
                </el-radio-group>
              </div>

              <div class="ticket-filter-form__actions workspace-filter-actions">
                <el-button @click="resetFilters">重置</el-button>
                <el-button type="primary" native-type="submit">查询</el-button>
              </div>
            </div>
          </el-form>
        </el-card>

        <el-card shadow="never" class="ticket-list-card workspace-card">
          <div class="ticket-list-card__head workspace-section-head workspace-section-head-spaced">
            <div class="ticket-list-card__title workspace-section-title">
              <h3>工单列表</h3>
              <p>共 {{ displayedTickets.length }} 条结果，支持看板和表格两种查看方式。</p>
            </div>
            <div class="ticket-list-card__summary workspace-summary">
              <el-tag effect="plain" round>全部 {{ tickets.length }}</el-tag>
              <el-tag effect="plain" round type="warning">待处理 {{ processingCount }}</el-tag>
              <el-tag effect="plain" round type="danger">待沉淀 {{ knowledgeGapCount }}</el-tag>
              <el-tag v-if="localTicketCount" effect="plain" round type="info">本地草稿 {{ localTicketCount }}</el-tag>
              <el-tag v-if="loading" effect="plain" round type="info">加载中</el-tag>
            </div>
          </div>

          <ErrorTraceNotice
            v-if="errorMessage"
            class="ticket-error workspace-error"
            inline
            :message="`${errorMessage}${usedFallbackData ? '，当前展示的是兜底数据。' : ''}`"
            :trace-id="errorTraceId"
          />

          <template v-if="displayedTickets.length">
            <div v-if="viewMode === 'board'" class="ticket-board">
              <RouterLink
                v-for="ticket in displayedTickets"
                :key="ticket.id"
                class="ticket-board-link"
                :to="`/tickets/${ticket.id}`"
              >
                <article class="ticket-board-item">
                  <div class="ticket-board-item__head">
                    <div>
                      <strong>{{ ticket.title }}</strong>
                      <p>{{ ticket.ticketNo }} · {{ ticket.assignee || '待分配' }}</p>
                    </div>
                    <el-tag :type="priorityTagType(ticketPriorityLevel(ticket))" effect="plain" round>
                      {{ ticketPriorityLabel(ticket) }}
                    </el-tag>
                  </div>

                  <p class="ticket-board-item__content">{{ ticket.content }}</p>

                  <div class="ticket-board-item__meta">
                    <el-tag :type="statusTagType(ticket.status)" effect="plain" round>
                      {{ ticket.status }}
                    </el-tag>
                    <span>提交人 {{ ticket.submitter }}</span>
                    <span>更新于 {{ ticket.updatedAt }}</span>
                  </div>

                  <div class="ticket-board-item__tags">
                    <el-tag
                      v-for="tag in ticket.tags"
                      :key="tag"
                      effect="plain"
                      round
                    >
                      {{ tag }}
                    </el-tag>
                    <el-tag v-if="ticket.linkedKnowledgeArticleCount" type="primary" effect="plain" round>
                      已关联 {{ ticket.linkedKnowledgeArticleCount }} 篇知识
                    </el-tag>
                    <el-tag v-else-if="needsKnowledgeCapture(ticket)" type="danger" effect="plain" round>
                      待沉淀知识
                    </el-tag>
                    <el-tag v-if="ticket.source === 'local'" type="warning" effect="plain" round>
                      本地草稿
                    </el-tag>
                  </div>
                </article>
              </RouterLink>
            </div>

            <el-table
              v-else
              :data="displayedTickets"
              stripe
              class="ticket-table"
              @row-click="handleRowClick"
            >
              <el-table-column label="工单信息" min-width="320">
                <template #default="{ row }">
                  <div class="ticket-table-cell">
                    <strong>{{ row.title }}</strong>
                    <p>{{ row.ticketNo }} · {{ row.content }}</p>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="plain" round>
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="优先级" width="120">
                <template #default="{ row }">
                  <el-tag :type="priorityTagType(ticketPriorityLevel(row))" effect="plain" round>
                    {{ ticketPriorityLabel(row) }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column label="负责人" width="150">
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

              <el-table-column label="操作" width="110" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="navigateTo(`/tickets/${row.id}`)">
                    查看
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <el-empty v-else description="暂无符合条件的工单" />
        </el-card>
      </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchTickets, type TicketApiItem } from '../api/ticket'
import { buildTicketListScopeMessage } from '../access-policy'
import { authState } from '../auth'
import { canViewAllTickets } from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import AppShell from '../components/layout/AppShell.vue'
import { manageNav, tickets as fallbackTickets, workspaceNav } from '../mock/dashboard'
import { listLocalTickets, mergeTickets } from '../mock/ticketWorkspace'
import type { TicketItem } from '../types/dashboard'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { countArticlesBySourceTicket } from '../utils/knowledgeSourceTicket'
import { formatTicketListItem } from '../utils/ticketPresentation'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

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
const viewMode = ref<ViewMode>('compact')

const viewModeOptions = [
  { label: '表格视图', value: 'compact' },
  { label: '看板视图', value: 'board' },
]

const canSeeAllTickets = computed(() => canViewAllTickets())
const runtimeModeText = computed(() => getRuntimeModeText())
const runtimeHeadline = computed(() => getRuntimeModeHeadline())
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
const runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: usedFallbackData.value,
  subject: '工单列表',
  localOnlyLabel: localTicketCount.value ? '工单草稿' : '',
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
    label: '当前工单',
    value: tickets.value.length,
    description: '权限范围内可见',
  },
  {
    label: '待处理',
    value: processingCount.value,
    description: '仍需继续跟进',
  },
  {
    label: '本地草稿',
    value: localTicketCount.value,
    description: '仅保存在本地',
  },
  {
    label: '待沉淀知识',
    value: knowledgeGapCount.value,
    description: '已解决但未沉淀',
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

function ticketPriorityLevel(ticket: TicketItem) {
  return ticket.priorityLevel || 'P4'
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
  viewMode.value = route.query.viewMode === 'board' ? 'board' : 'compact'
}

function updateRouteQuery() {
  router.replace({
    query: {
      keyword: filters.keyword.trim() || undefined,
      status: filters.status || undefined,
      priority: filters.priority || undefined,
      type: filters.type || undefined,
      quickFilter: activeQuickFilter.value !== 'all' ? activeQuickFilter.value : undefined,
      viewMode: viewMode.value !== 'compact' ? viewMode.value : undefined,
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
.ticket-board {
  display: grid;
}

.ticket-runtime-banner {
  display: grid;
  gap: 6px;
  margin-top: 16px;
}

.ticket-runtime-banner strong,
.ticket-runtime-banner p {
  margin: 0;
}

.ticket-filter-form__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.ticket-filter-form__item {
  margin-bottom: 0;
}

.ticket-quick-filters {
  display: grid;
  gap: 8px;
}

.ticket-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.ticket-board-link {
  display: block;
}

.ticket-board-item {
  height: 100%;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.ticket-board-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.ticket-board-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.ticket-board-item__head strong,
.ticket-table-cell strong {
  display: block;
  color: #0f172a;
}

.ticket-board-item__head p,
.ticket-board-item__content,
.ticket-table-cell p {
  margin: 4px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.ticket-board-item__meta,
.ticket-board-item__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 14px;
  color: #64748b;
  font-size: 13px;
}

.ticket-table-cell {
  display: grid;
  gap: 4px;
}

.ticket-table-muted {
  color: #94a3b8;
}

@media (max-width: 1240px) {
  .ticket-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ticket-filter-form__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .ticket-board,
  .ticket-filter-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
