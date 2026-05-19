<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
    <section class="knowledge-page workspace-page">
      <el-card shadow="never" class="knowledge-header-card workspace-card">
        <div class="knowledge-header workspace-header">
          <div class="knowledge-header__main workspace-header-main">
            <div class="knowledge-header__title workspace-title">
              <h2>{{ knowledgeHeroCopy.title }}</h2>
              <p>{{ knowledgeHeroCopy.description }}</p>
            </div>
            <div class="knowledge-header__actions workspace-actions">
              <el-button @click="toggleFilters">
                {{ showFilters ? '收起筛选' : '展开筛选' }}
              </el-button>
              <el-button
                v-if="canManage"
                type="primary"
                @click="navigateTo('/knowledge/articles/create')"
              >
                {{ knowledgeHeroCopy.ctaLabel }}
              </el-button>
            </div>
          </div>

          <div class="knowledge-stats workspace-stats">
            <div
              v-for="stat in statCards"
              :key="stat.label"
              class="knowledge-stat workspace-stat"
            >
              <span class="knowledge-stat__label workspace-stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.description }}</p>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="knowledge-filter-card workspace-card">
        <div class="knowledge-filter-card__head workspace-section-head">
          <div class="workspace-section-title">
            <h3>查询条件</h3>
            <p>统一管理关键字、来源工单、分类和发布状态，保持和工单列表一致的后台筛选节奏。</p>
          </div>
          <div class="knowledge-filter-card__mode workspace-section-mode">
            <span>显示模式</span>
            <el-segmented
              v-model="viewMode"
              :options="viewModeOptions"
            />
          </div>
        </div>

        <div v-if="authNotice" class="state-box state-warning knowledge-filter-banner">
          {{ authNotice }}
        </div>
        <div class="state-box knowledge-runtime-banner" :class="{ 'state-warning': isDemoMode() || usedFallbackData }">
          <strong>{{ runtimeHeadline }} · {{ runtimeModeText }}</strong>
          <p>{{ runtimeDataSourceMessage }}</p>
        </div>
        <div v-if="filters.sourceTicketNo" class="state-box knowledge-filter-banner">
          <span>当前正在按来源工单筛选：</span>
          <strong>{{ filters.sourceTicketNo }}</strong>
          <el-button text type="primary" @click="clearSourceTicketFilter">清除</el-button>
        </div>

        <el-form
          v-if="showFilters"
          class="knowledge-filter-form workspace-filter-form"
          label-position="top"
          @submit.prevent="handleSearch"
        >
          <div class="knowledge-filter-form__grid">
            <el-form-item label="关键字" class="knowledge-filter-form__item knowledge-filter-form__item--keyword">
              <el-input
                v-model.trim="filters.keyword"
                clearable
                placeholder="搜索标题或摘要"
                @keyup.enter="handleSearch"
              />
            </el-form-item>

            <el-form-item label="来源工单号" class="knowledge-filter-form__item">
              <el-input
                v-model.trim="filters.sourceTicketNo"
                clearable
                placeholder="例如 INC-20260511"
                @keyup.enter="handleSearch"
              />
            </el-form-item>

            <el-form-item label="分类" class="knowledge-filter-form__item">
              <el-select v-model="filters.categoryId" placeholder="全部分类">
                <el-option
                  v-for="option in categoryOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="状态" class="knowledge-filter-form__item">
              <el-select v-model="filters.status" placeholder="全部状态">
                <el-option
                  v-for="option in statusOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="knowledge-filter-form__footer workspace-filter-footer">
            <div class="knowledge-quick-filters">
              <span class="knowledge-section-label workspace-section-label">快速筛选</span>
              <el-radio-group v-model="activeQuickFilter">
                <el-radio-button
                  v-for="filter in quickFilters"
                  :key="filter.value"
                  :value="filter.value"
                >
                  {{ filter.label }}
                </el-radio-button>
              </el-radio-group>
            </div>

            <div class="knowledge-filter-form__actions workspace-filter-actions">
              <el-button @click="handleReset">重置</el-button>
              <el-button type="primary" native-type="submit">查询</el-button>
            </div>
          </div>
        </el-form>
      </el-card>

      <el-card shadow="never" class="knowledge-list-card workspace-card">
        <div class="knowledge-list-card__head workspace-section-head workspace-section-head-spaced">
          <div class="knowledge-list-card__title workspace-section-title">
            <h3>知识文章列表</h3>
            <p>共 {{ displayedArticles.length }} 条结果，支持卡片和紧凑列表两种查看方式。</p>
          </div>
          <div class="knowledge-list-card__summary workspace-summary">
            <el-tag effect="plain" round>全部 {{ articles.length }}</el-tag>
            <el-tag effect="plain" round type="success">已发布 {{ publishedCount }}</el-tag>
            <el-tag effect="plain" round type="warning">草稿 {{ draftCount }}</el-tag>
            <el-tag effect="plain" round type="info">来源关联 {{ sourceLinkedCount }}</el-tag>
            <el-tag v-if="localDraftCount" effect="plain" round type="warning">本地草稿 {{ localDraftCount }}</el-tag>
            <el-tag v-if="loading" effect="plain" round type="info">加载中</el-tag>
          </div>
        </div>

        <ErrorTraceNotice
          v-if="errorMessage"
          class="knowledge-error workspace-error"
          inline
          :message="`${errorMessage}${usedFallbackData ? '，当前先回退到本地演示数据。' : ''}`"
          :trace-id="errorTraceId"
        />

        <div v-if="loading" class="state-box">正在拉取知识文章列表...</div>
        <div v-else-if="displayedArticles.length === 0" class="state-box">{{ knowledgeHeroCopy.emptyHint }}</div>

        <div v-else-if="viewMode === 'grid'" class="knowledge-grid">
          <RouterLink
            v-for="article in displayedArticles"
            :key="article.id"
            class="knowledge-card"
            :to="`/knowledge/articles/${article.id}`"
          >
            <div class="knowledge-card__head">
              <div class="knowledge-card__chips">
                <span class="chip" :class="statusClass(article.status)">{{ statusText(article.status) }}</span>
                <span class="chip chip-blue">{{ categoryText(article.categoryId) }}</span>
                <span v-if="'source' in article && article.source === 'local'" class="chip chip-orange">本地草稿</span>
              </div>
              <span class="knowledge-card__date">{{ formatDate(article.publishTime || article.createTime) }}</span>
            </div>

            <strong>{{ article.title }}</strong>
            <p>{{ article.summary || article.content.slice(0, 96) }}</p>

            <div class="knowledge-card__meta">
              <span>作者 ID：{{ article.authorUserId }}</span>
              <span>浏览 {{ article.viewCount }}</span>
              <span>点赞 {{ article.likeCount }}</span>
              <span>收藏 {{ article.collectCount }}</span>
            </div>

            <div v-if="article.sourceTicket" class="knowledge-card__source">
              <span>来源工单：{{ article.sourceTicket.ticketNo || `#${article.sourceTicket.id}` }}</span>
              <span>{{ article.sourceTicket.title || '来源处理记录' }}</span>
            </div>
          </RouterLink>
        </div>

        <div v-else class="knowledge-list-view">
          <RouterLink
            v-for="article in displayedArticles"
            :key="article.id"
            class="knowledge-list-item"
            :to="`/knowledge/articles/${article.id}`"
          >
            <div class="knowledge-list-item__main">
              <div class="knowledge-card__chips">
                <span class="chip" :class="statusClass(article.status)">{{ statusText(article.status) }}</span>
                <span class="chip chip-blue">{{ categoryText(article.categoryId) }}</span>
                <span v-if="'source' in article && article.source === 'local'" class="chip chip-orange">本地草稿</span>
              </div>
              <strong>{{ article.title }}</strong>
              <p>{{ article.summary || article.content.slice(0, 140) }}</p>
            </div>
            <div class="knowledge-list-item__side">
              <span>{{ formatDate(article.publishTime || article.createTime) }}</span>
              <span v-if="article.sourceTicket">来源 {{ article.sourceTicket.ticketNo || `#${article.sourceTicket.id}` }}</span>
              <span>浏览 {{ article.viewCount }}</span>
              <span>点赞 {{ article.likeCount }}</span>
            </div>
          </RouterLink>
        </div>
      </el-card>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { buildKnowledgeHeroCopy } from '../access-policy'
import { canManageKnowledgeArticles } from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import AppShell from '../components/layout/AppShell.vue'
import { fetchKnowledgeArticles } from '../api/knowledge'
import { listKnowledgeDrafts, mergeKnowledgeArticles } from '../mock/knowledgeDrafts'
import { manageNav, workspaceNav } from '../mock/dashboard'
import type { KnowledgeArticleApiItem, KnowledgeArticleDraft } from '../types/dashboard'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { attachArticleSourceTicket } from '../utils/knowledgeSourceTicket'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

type QuickFilter = 'all' | 'published' | 'draft' | 'archived' | 'popular' | 'source-linked' | 'local'
type ViewMode = 'grid' | 'list'

const router = useRouter()
const route = useRoute()
const canManage = computed(() => canManageKnowledgeArticles())
const runtimeModeText = computed(() => getRuntimeModeText())
const runtimeHeadline = computed(() => getRuntimeModeHeadline())
const knowledgeHeroCopy = computed(() => buildKnowledgeHeroCopy({
  canManageKnowledge: canManage.value,
}))
const authNotice = computed(() => {
  if (route.query.reason !== 'forbidden') {
    return ''
  }
  return '当前账号没有执行该知识管理操作的权限，已为你返回可访问的知识列表。'
})

const fallbackArticles: KnowledgeArticleApiItem[] = [
  {
    id: 1,
    title: '支付回调失败排查手册',
    summary: '用于指导支持人员快速定位支付回调失败问题。',
    content: '第一步检查签名配置。第二步检查回调地址连通性。第三步检查幂等控制键是否异常。第四步检查日志链路与消息重试记录。',
    categoryId: 3,
    authorUserId: 2,
    status: 1,
    viewCount: 1284,
    likeCount: 36,
    collectCount: 19,
    publishTime: '2026-05-11T09:20:00',
    createTime: '2026-05-11T09:20:00',
    updateTime: '2026-05-11T09:20:00',
  },
  {
    id: 2,
    title: '生产环境发布异常应急处理流程',
    summary: '发布失败、回滚和异常上报的标准操作流程。',
    content: '当发布异常发生时，先确认影响范围，再执行回滚或降级方案，同时记录事件时间线并同步相关负责人。',
    categoryId: 2,
    authorUserId: 2,
    status: 1,
    viewCount: 926,
    likeCount: 21,
    collectCount: 14,
    publishTime: '2026-05-10T18:40:00',
    createTime: '2026-05-10T18:40:00',
    updateTime: '2026-05-10T18:40:00',
  },
]

const filters = reactive({
  keyword: '',
  sourceTicketNo: '',
  categoryId: 'all',
  status: 'all',
})
const showFilters = ref(true)
const activeQuickFilter = ref<QuickFilter>('all')
const viewMode = ref<ViewMode>('grid')

const articles = ref<Array<KnowledgeArticleApiItem | KnowledgeArticleDraft>>([])
const loading = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')
const usedFallbackData = ref(false)
let skipNextRouteDrivenLoad = false
let skipNextQuickFilterRefresh = false
let skipNextViewModeRefresh = false
let articleLoadRequestId = 0

const viewModeOptions = [
  { label: '卡片视图', value: 'grid' },
  { label: '列表视图', value: 'list' },
]

const categoryOptions = [
  { value: 'all', label: '全部分类' },
  { value: '1', label: '系统使用指南' },
  { value: '2', label: '故障排查' },
  { value: '3', label: '支付与订单' },
]

const statusOptions = [
  { value: 'all', label: '全部状态' },
  { value: '0', label: '草稿' },
  { value: '1', label: '已发布' },
  { value: '2', label: '已归档' },
]

const quickFilters = [
  { value: 'all', label: '全部' },
  { value: 'published', label: '已发布' },
  { value: 'draft', label: '草稿' },
  { value: 'archived', label: '已归档' },
  { value: 'popular', label: '高浏览' },
  { value: 'source-linked', label: '工单沉淀' },
  { value: 'local', label: '本地内容' },
] as const satisfies ReadonlyArray<{ value: QuickFilter; label: string }>

const displayedArticles = computed(() => {
  if (activeQuickFilter.value === 'published') {
    return articles.value.filter((article) => article.status === 1)
  }
  if (activeQuickFilter.value === 'draft') {
    return articles.value.filter((article) => article.status === 0)
  }
  if (activeQuickFilter.value === 'archived') {
    return articles.value.filter((article) => article.status === 2)
  }
  if (activeQuickFilter.value === 'popular') {
    return articles.value.filter((article) => article.viewCount >= 500)
  }
  if (activeQuickFilter.value === 'source-linked') {
    return articles.value.filter((article) => !!article.sourceTicket || !!article.sourceTicketId)
  }
  if (activeQuickFilter.value === 'local') {
    return articles.value.filter((article) => 'source' in article && article.source === 'local')
  }
  return articles.value
})

const publishedCount = computed(() => articles.value.filter((article) => article.status === 1).length)
const draftCount = computed(() => articles.value.filter((article) => article.status === 0).length)
const localDraftCount = computed(() => articles.value.filter((article) => 'source' in article && article.source === 'local').length)
const sourceLinkedCount = computed(() => articles.value.filter((article) => !!article.sourceTicket || !!article.sourceTicketId).length)
const runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: usedFallbackData.value,
  subject: '知识列表',
  localOnlyLabel: localDraftCount.value ? '知识草稿' : '',
}))

const statCards = computed(() => [
  {
    label: '当前结果',
    value: displayedArticles.value.length,
    description: '筛选条件下可直接查看',
  },
  {
    label: '已发布',
    value: publishedCount.value,
    description: '可被团队检索复用',
  },
  {
    label: '本地草稿',
    value: localDraftCount.value,
    description: '尚未同步到远端',
  },
  {
    label: '来源关联',
    value: sourceLinkedCount.value,
    description: '来自工单沉淀链路',
  },
])

function categoryText(categoryId: number | null) {
  if (!categoryId) {
    return '未分类'
  }
  return categoryOptions.find((item) => item.value === String(categoryId))?.label || `分类 ${categoryId}`
}

function statusText(status: number) {
  return statusOptions.find((item) => item.value === String(status))?.label || '未知状态'
}

function statusClass(status: number) {
  if (status === 1) {
    return 'chip-green'
  }
  if (status === 2) {
    return 'chip-orange'
  }
  return 'chip-blue'
}

function formatDate(value: string | null) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ')
}

function navigateTo(path: string) {
  if (route.path === path) {
    return
  }
  router.push(path)
}

function toggleFilters() {
  showFilters.value = !showFilters.value
}

function syncFiltersFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.sourceTicketNo = typeof route.query.sourceTicketNo === 'string' ? route.query.sourceTicketNo : ''
  filters.categoryId = typeof route.query.categoryId === 'string' ? route.query.categoryId : 'all'
  filters.status = typeof route.query.status === 'string' ? route.query.status : 'all'
  skipNextQuickFilterRefresh = true
  activeQuickFilter.value = typeof route.query.quickFilter === 'string'
    && quickFilters.some((item) => item.value === route.query.quickFilter)
      ? route.query.quickFilter as QuickFilter
      : 'all'
  skipNextViewModeRefresh = true
  viewMode.value = route.query.viewMode === 'list' ? 'list' : 'grid'
}

function updateRouteQuery() {
  router.replace({
    query: {
      keyword: filters.keyword || undefined,
      sourceTicketNo: filters.sourceTicketNo || undefined,
      categoryId: filters.categoryId !== 'all' ? filters.categoryId : undefined,
      status: filters.status !== 'all' ? filters.status : undefined,
      quickFilter: activeQuickFilter.value !== 'all' ? activeQuickFilter.value : undefined,
      viewMode: viewMode.value !== 'grid' ? viewMode.value : undefined,
    },
  })
}

function refreshFromLocalState() {
  skipNextRouteDrivenLoad = true
  updateRouteQuery()
  loadArticles()
}

async function loadArticles() {
  const requestId = ++articleLoadRequestId
  loading.value = true
  errorMessage.value = ''
  errorTraceId.value = ''
  usedFallbackData.value = false

  try {
    const data = await fetchKnowledgeArticles({
      keyword: filters.keyword || undefined,
      sourceTicketNo: filters.sourceTicketNo || undefined,
      categoryId: filters.categoryId === 'all' ? undefined : Number(filters.categoryId),
      status: filters.status === 'all' ? undefined : Number(filters.status),
    })
    if (requestId !== articleLoadRequestId) {
      return
    }
    articles.value = mergeKnowledgeArticles(data).map((item) => attachArticleSourceTicket(item))
  } catch (error) {
    if (requestId !== articleLoadRequestId) {
      return
    }
    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: '知识库接口暂时不可用',
      defaultMessage: '知识文章列表加载失败，请稍后重试。',
    })
    usedFallbackData.value = result.shouldUseFallbackData
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    articles.value = mergeKnowledgeArticles(
      result.shouldUseFallbackData ? fallbackArticles : [],
      listKnowledgeDrafts(),
    ).map((item) => attachArticleSourceTicket(item))
    console.error(error)
  } finally {
    if (requestId === articleLoadRequestId) {
      loading.value = false
    }
  }
}

function handleSearch() {
  refreshFromLocalState()
}

function handleReset() {
  filters.keyword = ''
  filters.sourceTicketNo = ''
  filters.categoryId = 'all'
  filters.status = 'all'
  activeQuickFilter.value = 'all'
  refreshFromLocalState()
}

function clearSourceTicketFilter() {
  filters.sourceTicketNo = ''
  refreshFromLocalState()
}

onMounted(() => {
  syncFiltersFromRoute()
  loadArticles()
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
  () => {
    if (skipNextRouteDrivenLoad) {
      skipNextRouteDrivenLoad = false
      return
    }
    syncFiltersFromRoute()
    loadArticles()
  },
)
</script>

<style scoped>
.knowledge-filter-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: space-between;
  margin-top: 16px;
}

.knowledge-runtime-banner {
  display: grid;
  gap: 6px;
  margin-top: 16px;
}

.knowledge-runtime-banner strong,
.knowledge-runtime-banner p {
  margin: 0;
}

.knowledge-filter-form__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1.1fr) repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.knowledge-filter-form__item {
  margin-bottom: 0;
}

.knowledge-quick-filters {
  display: grid;
  gap: 8px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.knowledge-card,
.knowledge-list-item {
  display: block;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}

.knowledge-card {
  padding: 18px;
}

.knowledge-card:hover,
.knowledge-list-item:hover {
  border-color: #cbd5e1;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.knowledge-card__head,
.knowledge-card__meta,
.knowledge-card__source {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
}

.knowledge-card__chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.knowledge-card strong,
.knowledge-list-item__main strong {
  display: block;
  margin-top: 14px;
  color: #0f172a;
}

.knowledge-card p,
.knowledge-list-item__main p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.knowledge-card__date,
.knowledge-card__meta,
.knowledge-card__source,
.knowledge-list-item__side {
  color: #64748b;
  font-size: 13px;
}

.knowledge-card__meta,
.knowledge-card__source {
  margin-top: 14px;
}

.knowledge-list-view {
  display: grid;
  gap: 12px;
}

.knowledge-list-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  padding: 16px 18px;
}

.knowledge-list-item__main {
  display: grid;
  gap: 8px;
}

.knowledge-list-item__side {
  display: grid;
  gap: 6px;
  justify-items: end;
}

@media (max-width: 1240px) {
  .knowledge-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .knowledge-filter-form__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 840px) {
  .knowledge-list-item {
    grid-template-columns: 1fr;
    display: grid;
  }

  .knowledge-list-item__side {
    justify-items: start;
  }

  .knowledge-grid,
  .knowledge-filter-form__grid {
    grid-template-columns: 1fr;
  }

  .knowledge-filter-banner {
    align-items: flex-start;
  }
}
</style>
