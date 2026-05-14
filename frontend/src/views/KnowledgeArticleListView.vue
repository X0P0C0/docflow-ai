<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />

    <main class="main-content">
      <AppTopbar />

      <section class="panel knowledge-hero">
        <div>
          <span class="hero-tag">Knowledge Base</span>
          <h2>{{ knowledgeHeroCopy.title }}</h2>
          <p>{{ knowledgeHeroCopy.description }}</p>
        </div>
        <div class="hero-stats">
          <div class="stat-row">
            <div>
              <p>当前结果</p>
              <strong>{{ filteredCount }}</strong>
            </div>
            <span class="chip chip-blue">Articles</span>
          </div>
          <div class="stat-row">
            <div>
              <p>已发布</p>
              <strong>{{ publishedCount }}</strong>
            </div>
            <span class="chip chip-green">Live</span>
          </div>
          <div class="stat-row">
            <div>
              <p>分类数</p>
              <strong>{{ categoryOptions.length - 1 }}</strong>
            </div>
            <span class="chip chip-orange">Catalog</span>
          </div>
        </div>
      </section>

      <section class="panel knowledge-toolbar">
        <div v-if="authNotice" class="state-box state-warning source-filter-banner">
          {{ authNotice }}
        </div>
        <div v-if="filters.sourceTicketNo" class="state-box source-filter-banner">
          <span>当前正在按来源工单筛选：</span>
          <strong>{{ filters.sourceTicketNo }}</strong>
          <button class="ghost-button" type="button" @click="clearSourceTicketFilter">清除</button>
        </div>
        <form class="filter-grid" @submit.prevent="handleSearch">
          <label class="filter-field filter-field-wide">
            <span>关键词</span>
            <input v-model.trim="filters.keyword" type="text" placeholder="搜索标题或摘要" />
          </label>

          <label class="filter-field">
            <span>来源工单号</span>
            <input v-model.trim="filters.sourceTicketNo" type="text" placeholder="例如 INC-20260511" />
          </label>

          <label class="filter-field">
            <span>分类</span>
            <select v-model="filters.categoryId">
              <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <label class="filter-field">
            <span>状态</span>
            <select v-model="filters.status">
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>

          <div class="filter-actions">
            <button class="ghost-button" type="button" @click="handleReset">重置</button>
            <button class="primary-button" type="submit">应用筛选</button>
          </div>
        </form>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>知识文章列表</h3>
            <p>真实接口优先，当前已经和后端列表接口参数联动。</p>
          </div>
          <button
            v-if="canManage"
            class="primary-button"
            type="button"
            @click="router.push('/knowledge/articles/create')"
          >
            {{ knowledgeHeroCopy.ctaLabel }}
          </button>
        </div>

        <div class="knowledge-toolbar-row">
          <div class="ticket-quick-filters">
            <button
              v-for="filter in quickFilters"
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
              :class="{ 'toggle-active': viewMode === 'grid' }"
              type="button"
              @click="viewMode = 'grid'"
            >
              卡片视图
            </button>
            <button
              class="ghost-button"
              :class="{ 'toggle-active': viewMode === 'list' }"
              type="button"
              @click="viewMode = 'list'"
            >
              列表视图
            </button>
          </div>
        </div>

        <div v-if="loading" class="state-box">正在拉取知识文章列表...</div>
        <ErrorTraceNotice
          v-else-if="errorMessage"
          :message="`${errorMessage}${usedFallbackData ? '，当前先回退到本地演示数据。' : ''}`"
          :trace-id="errorTraceId"
        />
        <div v-if="!loading && displayedArticles.length === 0" class="state-box">{{ knowledgeHeroCopy.emptyHint }}</div>

        <div v-else-if="!loading && viewMode === 'grid'" class="knowledge-grid">
          <RouterLink
            v-for="article in displayedArticles"
            :key="article.id"
            class="article-card article-link knowledge-card"
            :to="`/knowledge/articles/${article.id}`"
          >
            <div class="knowledge-card-head">
              <span class="chip" :class="statusClass(article.status)">{{ statusText(article.status) }}</span>
              <span class="chip chip-blue">{{ categoryText(article.categoryId) }}</span>
              <span v-if="'source' in article && article.source === 'local'" class="chip chip-orange">本地草稿</span>
            </div>
            <h4>{{ article.title }}</h4>
            <p>{{ article.summary || article.content.slice(0, 96) }}</p>
            <div class="article-meta">
              <span>作者 ID：{{ article.authorUserId }}</span>
              <span>{{ formatDate(article.publishTime || article.createTime) }}</span>
            </div>
            <div v-if="article.sourceTicket" class="article-meta source-meta">
              <span>来源工单：{{ article.sourceTicket.ticketNo || `#${article.sourceTicket.id}` }}</span>
              <span>{{ article.sourceTicket.title || '来源处理记录' }}</span>
            </div>
            <div class="article-meta">
              <span>浏览 {{ article.viewCount }}</span>
              <span>点赞 {{ article.likeCount }}</span>
              <span>收藏 {{ article.collectCount }}</span>
            </div>
          </RouterLink>
        </div>

        <div v-else-if="!loading" class="knowledge-list-view">
          <RouterLink
            v-for="article in displayedArticles"
            :key="article.id"
            class="knowledge-list-item"
            :to="`/knowledge/articles/${article.id}`"
          >
            <div class="knowledge-list-main">
              <div class="knowledge-card-head">
                <span class="chip" :class="statusClass(article.status)">{{ statusText(article.status) }}</span>
                <span class="chip chip-blue">{{ categoryText(article.categoryId) }}</span>
                <span v-if="'source' in article && article.source === 'local'" class="chip chip-orange">本地草稿</span>
              </div>
              <strong>{{ article.title }}</strong>
              <p>{{ article.summary || article.content.slice(0, 140) }}</p>
            </div>
            <div class="knowledge-list-side">
              <span>{{ formatDate(article.publishTime || article.createTime) }}</span>
              <span v-if="article.sourceTicket">来源 {{ article.sourceTicket.ticketNo || `#${article.sourceTicket.id}` }}</span>
              <span>浏览 {{ article.viewCount }}</span>
              <span>点赞 {{ article.likeCount }}</span>
            </div>
          </RouterLink>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { buildKnowledgeHeroCopy } from '../access-policy'
import { canManageKnowledgeArticles } from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopbar from '../components/layout/AppTopbar.vue'
import { fetchKnowledgeArticles } from '../api/knowledge'
import { listKnowledgeDrafts, mergeKnowledgeArticles } from '../mock/knowledgeDrafts'
import { manageNav, workspaceNav } from '../mock/dashboard'
import type { KnowledgeArticleApiItem, KnowledgeArticleDraft } from '../types/dashboard'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { attachArticleSourceTicket } from '../utils/knowledgeSourceTicket'

const router = useRouter()
const route = useRoute()
const canManage = computed(() => canManageKnowledgeArticles())
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
const activeQuickFilter = ref<'all' | 'published' | 'draft' | 'archived' | 'popular' | 'source-linked' | 'local'>('all')
const viewMode = ref<'grid' | 'list'>('grid')

const articles = ref<Array<KnowledgeArticleApiItem | KnowledgeArticleDraft>>([])
const loading = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')
const usedFallbackData = ref(false)
let skipNextRouteDrivenLoad = false
let skipNextQuickFilterRefresh = false
let articleLoadRequestId = 0

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

const filteredCount = computed(() => displayedArticles.value.length)
const publishedCount = computed(() => articles.value.filter((article) => article.status === 1).length)
const quickFilters = [
  { value: 'all', label: '全部' },
  { value: 'published', label: '已发布' },
  { value: 'draft', label: '草稿' },
  { value: 'archived', label: '已归档' },
  { value: 'popular', label: '高浏览' },
  { value: 'source-linked', label: '工单沉淀' },
  { value: 'local', label: '本地内容' },
] as const
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

function syncFiltersFromRoute() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.sourceTicketNo = typeof route.query.sourceTicketNo === 'string' ? route.query.sourceTicketNo : ''
  filters.categoryId = typeof route.query.categoryId === 'string' ? route.query.categoryId : 'all'
  filters.status = typeof route.query.status === 'string' ? route.query.status : 'all'
  skipNextQuickFilterRefresh = true
  activeQuickFilter.value = typeof route.query.quickFilter === 'string'
    && quickFilters.some((item) => item.value === route.query.quickFilter)
      ? route.query.quickFilter as typeof activeQuickFilter.value
      : 'all'
}

function updateRouteQuery() {
  router.replace({
    query: {
      keyword: filters.keyword || undefined,
      sourceTicketNo: filters.sourceTicketNo || undefined,
      categoryId: filters.categoryId !== 'all' ? filters.categoryId : undefined,
      status: filters.status !== 'all' ? filters.status : undefined,
      quickFilter: activeQuickFilter.value !== 'all' ? activeQuickFilter.value : undefined,
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
.knowledge-toolbar-row,
.ticket-quick-filters,
.ticket-view-toggle {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.knowledge-toolbar-row {
  align-items: center;
  justify-content: space-between;
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

.knowledge-list-view {
  display: grid;
  gap: 0.9rem;
}

.knowledge-list-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.knowledge-list-item:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.knowledge-list-main {
  display: grid;
  gap: 0.65rem;
}

.knowledge-list-main strong,
.knowledge-list-main p {
  margin: 0;
}

.knowledge-list-main p {
  color: #64748b;
  line-height: 1.75;
}

.knowledge-list-side {
  display: grid;
  gap: 0.5rem;
  justify-items: end;
  color: #64748b;
  font-size: 0.92rem;
}

.source-meta {
  color: #475569;
}

.source-filter-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  justify-content: space-between;
  margin-bottom: 1rem;
}

@media (max-width: 900px) {
  .knowledge-toolbar-row,
  .knowledge-list-item {
    grid-template-columns: 1fr;
  }

  .knowledge-list-side {
    justify-items: start;
  }

  .source-filter-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
