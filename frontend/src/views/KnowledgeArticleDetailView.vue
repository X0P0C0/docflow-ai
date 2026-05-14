<template>
  <div class="detail-page">
    <header class="detail-topbar">
      <RouterLink class="back-link" to="/knowledge/articles">返回知识库</RouterLink>
      <div class="detail-topbar-actions">
        <button v-if="canManage" class="ghost-button" type="button" @click="handleEdit">编辑文章</button>
        <button
          v-if="article && canManage && canArchiveArticle"
          class="ghost-button"
          type="button"
          :disabled="archiving"
          @click="handleArchive"
        >
          {{ archiving ? '归档中...' : '归档文章' }}
        </button>
        <button
          v-if="article && canManage"
          class="ghost-button danger-button"
          type="button"
          :disabled="deleting"
          @click="handleDelete"
        >
          {{ deleting ? '删除中...' : '删除文章' }}
        </button>
        <button class="primary-button" type="button" @click="toggleAiSummary">
          {{ showAiSummary ? '隐藏 AI 摘要' : 'AI 生成摘要' }}
        </button>
      </div>
    </header>

    <main class="detail-layout">
      <section class="detail-main panel">
        <div v-if="loading" class="state-box">正在加载文章详情...</div>
        <ErrorTraceNotice v-else-if="errorMessage" :message="errorMessage" :trace-id="errorTraceId" />
        <template v-else-if="article">
          <div class="detail-chip-row">
            <span class="chip chip-blue">知识文章</span>
            <span class="chip" :class="statusClass(article.status)">{{ statusText(article.status) }}</span>
            <span v-if="'source' in article && article.source === 'local'" class="chip chip-orange">本地草稿</span>
          </div>
          <h1 class="detail-title">{{ article.title }}</h1>
          <p class="detail-summary">
            {{ article.summary || '这篇文章暂时还没有摘要，后面可以把 AI 摘要能力真正接进来。' }}
          </p>

          <div class="detail-meta">
            <span>作者 ID：{{ article.authorUserId }}</span>
            <span>分类：{{ categoryText(article.categoryId) }}</span>
            <span>发布时间：{{ formatDate(article.publishTime || article.createTime) }}</span>
            <span>浏览量：{{ article.viewCount }}</span>
          </div>

          <div v-if="article.sourceTicket" class="state-box source-ticket-banner">
            <div class="source-ticket-copy">
              <span>来源工单：{{ article.sourceTicket.ticketNo || `#${article.sourceTicket.id}` }}</span>
              <p>{{ article.sourceTicket.title || '来源处理记录' }}</p>
            </div>
            <div class="source-ticket-actions">
              <RouterLink
                class="source-ticket-link"
                :to="buildSourceTicketRoute('timeline')"
              >
                查看处理记录
              </RouterLink>
              <RouterLink
                class="source-ticket-link"
                :to="buildSourceTicketRoute('comments')"
              >
                查看评论上下文
              </RouterLink>
            </div>
          </div>

          <div class="ticket-insight-strip">
            <div class="insight-card">
              <span class="muted">阅读时长</span>
              <strong>{{ readingMinutes }} 分钟</strong>
            </div>
            <div class="insight-card">
              <span class="muted">核心段落</span>
              <strong>{{ articleParagraphs.length }} 段</strong>
            </div>
            <div class="insight-card">
              <span class="muted">适用场景</span>
              <strong>{{ articleScenario }}</strong>
            </div>
          </div>

          <div class="knowledge-outline">
            <button
              v-for="(paragraph, index) in articleParagraphs"
              :key="paragraph"
              class="outline-item"
              type="button"
              @click="activeParagraphIndex = index"
            >
              <span>{{ index + 1 }}</span>
              <strong>{{ paragraph.slice(0, 24) }}</strong>
            </button>
          </div>

          <article class="detail-article">
            <p
              v-for="(paragraph, index) in articleParagraphs"
              :key="paragraph"
              :class="{ 'active-paragraph': activeParagraphIndex === index }"
            >
              {{ paragraph }}
            </p>
          </article>
        </template>
      </section>

      <aside class="detail-side">
        <article class="panel panel-dark">
          <div class="panel-head panel-head-dark">
            <div>
              <h3>AI 阅读辅助</h3>
              <p>这里后面会接真实 AI 摘要、问答和相关文章推荐。</p>
            </div>
          </div>
          <div class="ai-summary">{{ aiSummary }}</div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>阅读要点</h3>
              <p>先快速建立判断，再决定要不要展开全文。</p>
            </div>
          </div>
          <div class="mini-list">
            <div v-for="point in keyTakeaways" :key="point" class="mini-item">
              <span>{{ point }}</span>
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>版本记录</h3>
              <p>每次保存都会留下历史快照，先把可追溯性补起来。</p>
            </div>
          </div>
          <div class="mini-list">
            <div v-if="articleVersions.length === 0" class="mini-item">
              <span>当前还没有版本记录。</span>
            </div>
            <div v-for="version in articleVersions" :key="version.id" class="mini-item version-item">
              <div class="version-copy">
                <span>v{{ version.versionNo }} · {{ formatDate(version.createTime) }}</span>
                <span>{{ version.remark || version.title }}</span>
              </div>
              <button
                v-if="canRestoreVersion(version.id)"
                class="ghost-button version-action"
                type="button"
                :disabled="restoringVersionId === version.id"
                @click="handleRestoreVersion(version.id)"
              >
                {{ restoringVersionId === version.id ? '恢复中...' : '恢复此版本' }}
              </button>
            </div>
          </div>
        </article>

        <article class="panel" :class="{ 'panel-pinned': pinSourceTicketSummary && sourceTicketPreview }">
          <div v-if="sourceTicketPreview" class="source-ticket-summary">
            <div class="panel-head">
              <div>
                <h3>来源工单摘要</h3>
                <p>不离开知识页面，也先快速回看工单处理状态和最近动作。</p>
              </div>
              <button class="ghost-button" type="button" @click="pinSourceTicketSummary = !pinSourceTicketSummary">
                {{ pinSourceTicketSummary ? '取消固定' : '固定到顶部' }}
              </button>
            </div>
            <div class="mini-list">
              <div class="mini-item"><span>当前状态</span><span>{{ sourceTicketPreview.knowledgeContextSummary?.status }}</span></div>
              <div class="mini-item"><span>当前处理人</span><span>{{ sourceTicketPreview.knowledgeContextSummary?.assignee }}</span></div>
              <div class="mini-item"><span>评论数量</span><span>{{ sourceTicketPreview.knowledgeContextSummary?.commentCount }}</span></div>
              <div class="mini-item"><span>处理记录</span><span>{{ sourceTicketPreview.knowledgeContextSummary?.timelineCount }} 条</span></div>
            </div>
            <div v-if="sourceTicketPreview.knowledgeContextSummary?.latestTimelineTitle" class="state-box source-ticket-summary-note">
              <span class="muted">最近处理记录</span>
              <strong>{{ sourceTicketPreview.knowledgeContextSummary.latestTimelineTitle }}</strong>
              <p>{{ sourceTicketPreview.knowledgeContextSummary.latestTimelineAt || '刚刚更新' }}</p>
            </div>

            <div class="preview-actions">
              <button class="ghost-button" type="button" @click="showSourceCommentPreview = !showSourceCommentPreview">
                {{ showSourceCommentPreview ? '收起最新评论' : '展开最新评论' }}
              </button>
              <button class="ghost-button" type="button" @click="showSourceTimelinePreview = !showSourceTimelinePreview">
                {{ showSourceTimelinePreview ? '收起时间线' : '展开最新记录' }}
              </button>
            </div>

            <div v-if="showSourceCommentPreview" class="preview-block">
              <div class="preview-block-head">
                <strong>最新评论</strong>
                <span>{{ sourceCommentPreview.length }} 条</span>
              </div>
              <div v-if="sourceCommentPreview.length" class="mini-list">
                <div v-for="comment in sourceCommentPreview" :key="comment.author + comment.createdAt + comment.content" class="mini-item preview-item">
                  <span>{{ comment.author }} · {{ comment.typeLabel }}</span>
                  <span>{{ comment.content.slice(0, 40) }}</span>
                </div>
              </div>
              <div v-else class="mini-item">
                <span>当前还没有评论记录。</span>
              </div>
            </div>

            <div v-if="showSourceTimelinePreview" class="preview-block">
              <div class="preview-block-head">
                <strong>最新处理记录</strong>
                <span>{{ sourceTimelinePreview.length }} 条</span>
              </div>
              <div v-if="sourceTimelinePreview.length" class="mini-list">
                <div v-for="item in sourceTimelinePreview" :key="item.title + item.createdAt + item.desc" class="mini-item preview-item">
                  <span>{{ item.title }}</span>
                  <span>{{ item.createdAt || '刚刚更新' }}</span>
                </div>
              </div>
              <div v-else class="mini-item">
                <span>当前还没有处理记录。</span>
              </div>
            </div>
          </div>

          <div v-if="sourceTicketPreview" class="panel-divider"></div>

          <div class="panel-head">
            <div>
              <h3>文章信息</h3>
              <p>详情页要兼顾阅读体验和辅助信息结构。</p>
            </div>
          </div>
          <div class="mini-list">
            <div class="mini-item"><span>点赞数</span><span>{{ article?.likeCount ?? '-' }}</span></div>
            <div class="mini-item"><span>收藏数</span><span>{{ article?.collectCount ?? '-' }}</span></div>
            <div class="mini-item"><span>最后更新时间</span><span>{{ article ? formatDate(article.updateTime) : '-' }}</span></div>
            <div class="mini-item"><span>阅读状态</span><span>{{ readingStatus }}</span></div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>相关文章</h3>
              <p>围绕相似场景延展阅读，而不是只看单篇内容。</p>
            </div>
          </div>
          <div class="related-toolbar">
            <div class="knowledge-filter-chips">
              <button
                v-for="filter in relatedSourceFilters"
                :key="filter.value"
                class="chip quick-filter-chip"
                :class="activeRelatedSourceFilter === filter.value ? 'chip-blue active-chip' : 'chip-default'"
                type="button"
                @click="activeRelatedSourceFilter = filter.value"
              >
                {{ filter.label }}
              </button>
            </div>
            <select v-model="relatedSortMode" class="knowledge-sort-select">
              <option value="match">按匹配优先</option>
              <option value="title">按标题</option>
              <option value="status">按状态</option>
            </select>
          </div>
          <div class="mini-list">
            <RouterLink
              v-for="item in relatedArticles"
              :key="item.id"
              class="mini-item related-link"
              :to="`/knowledge/articles/${item.id}`"
            >
              <div class="related-link-main">
                <strong>{{ item.title }}</strong>
                <span>{{ item.metric }}</span>
              </div>
              <div v-if="item.reasons.length" class="related-reasons">
                <span
                  v-for="reason in item.reasons"
                  :key="`${item.id}-${reason}`"
                  class="chip chip-default related-reason-chip"
                >
                  {{ reason }}
                </span>
              </div>
            </RouterLink>
            <div v-if="!relatedArticles.length" class="mini-item">
              <span>当前筛选下还没有可展示的相关文章，试试切换来源或排序方式。</span>
            </div>
          </div>
        </article>
      </aside>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchTicketDetail } from '../api/ticket'
import { canManageKnowledgeArticles } from '../authz'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import {
  archiveKnowledgeArticle,
  deleteKnowledgeArticle,
  fetchKnowledgeArticles,
  fetchKnowledgeArticleDetail,
  restoreKnowledgeArticleVersion,
} from '../api/knowledge'
import { getKnowledgeDraft, removeKnowledgeDraft, updateKnowledgeDraftStatus } from '../mock/knowledgeDrafts'
import { getLocalTicket } from '../mock/ticketWorkspace'
import { fallbackArticles } from '../mock/dashboard'
import type { KnowledgeArticleApiItem, KnowledgeArticleDraft, TicketItem } from '../types/dashboard'
import { getApiErrorDisplay } from '../utils/apiErrorDisplay'
import { attachArticleSourceTicket, saveArticleSourceTicket } from '../utils/knowledgeSourceTicket'
import { formatTicketDetailItem } from '../utils/ticketPresentation'

const route = useRoute()
const router = useRouter()
const canManage = computed(() => canManageKnowledgeArticles())
const article = ref<KnowledgeArticleApiItem | KnowledgeArticleDraft | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')
const showAiSummary = ref(true)
const activeParagraphIndex = ref(0)
const restoringVersionId = ref<number | null>(null)
const archiving = ref(false)
const deleting = ref(false)
const relatedRemoteArticles = ref<Array<KnowledgeArticleApiItem | KnowledgeArticleDraft>>([])
const sourceTicketPreview = ref<TicketItem | null>(null)
const pinSourceTicketSummary = ref(false)
const showSourceCommentPreview = ref(false)
const showSourceTimelinePreview = ref(false)
const relatedSortMode = ref<'match' | 'title' | 'status'>('match')
const activeRelatedSourceFilter = ref<'all' | 'ticket-linked' | 'same-category'>('all')
let articleLoadRequestId = 0
const sourceCommentPreview = computed(() => sourceTicketPreview.value?.comments?.slice(0, 2) || [])
const sourceTimelinePreview = computed(() => sourceTicketPreview.value?.timeline?.slice(0, 3) || [])
const relatedSourceFilters = [
  { value: 'all', label: '全部' },
  { value: 'ticket-linked', label: '同来源工单' },
  { value: 'same-category', label: '同分类' },
] as const

const categoryMap: Record<number, string> = {
  1: '系统使用指南',
  2: '故障排查',
  3: '支付与订单',
}

const articleParagraphs = computed(() => {
  if (!article.value?.content) {
    return []
  }

  return article.value.content
    .split(/[；。\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
})

const aiSummary = computed(() => {
  if (!article.value) {
    return '当前还没有可分析的文章内容。'
  }

  if (!showAiSummary.value) {
    return 'AI 摘要已收起。你可以随时再次展开，快速提炼核心排查思路。'
  }

  const firstParagraph = articleParagraphs.value[0] || article.value.summary || article.value.content
  const secondParagraph = articleParagraphs.value[1] || '适合作为工单处理时的标准操作参考，后续也可以继续接入问答与推荐能力。'
  return `${firstParagraph} ${secondParagraph}`.trim()
})
const readingMinutes = computed(() => {
  const totalChars = article.value?.content.replace(/\s/g, '').length || 0
  return Math.max(1, Math.ceil(totalChars / 220))
})
const articleScenario = computed(() => {
  const category = categoryText(article.value?.categoryId ?? null)
  if (category.includes('支付')) {
    return '支付排障'
  }
  if (category.includes('故障')) {
    return '异常处理'
  }
  return '系统使用'
})
const keyTakeaways = computed(() => {
  const paragraphs = articleParagraphs.value.slice(0, 3)
  if (!paragraphs.length) {
    return ['当前还没有可提炼的阅读要点。']
  }
  return paragraphs.map((item) => item.slice(0, 36))
})
const readingStatus = computed(() => {
  if (!articleParagraphs.value.length) {
    return '待补充'
  }
  if (activeParagraphIndex.value >= articleParagraphs.value.length - 1) {
    return '已浏览重点'
  }
  return '阅读中'
})
const relatedArticles = computed(() => {
  const currentId = article.value?.id
  const currentCategory = article.value?.categoryId
  const remoteMatches = relatedRemoteArticles.value
    .filter((item) => item.id !== currentId)
    .filter((item) => {
      if (activeRelatedSourceFilter.value === 'ticket-linked') {
        return !!article.value?.sourceTicket?.id && item.sourceTicket?.id === article.value.sourceTicket.id
      }
      if (activeRelatedSourceFilter.value === 'same-category') {
        return !!currentCategory && item.categoryId === currentCategory
      }
      return true
    })
    .map((item) => ({
      id: item.id,
      title: item.title,
      metric: item.sourceTicket ? `来源 ${item.sourceTicket.ticketNo || `#${item.sourceTicket.id}`}` : `${statusText(item.status)} · ${categoryText(item.categoryId)}`,
      sortScore: scoreRelatedArticle(article.value, item),
      status: item.status,
      reasons: buildRelatedReasons(article.value, item),
    }))

  const sortedRemoteMatches = remoteMatches.sort((a, b) => {
    if (relatedSortMode.value === 'title') {
      return a.title.localeCompare(b.title, 'zh-CN')
    }
    if (relatedSortMode.value === 'status') {
      return a.status - b.status || b.sortScore - a.sortScore
    }
    return b.sortScore - a.sortScore
  })

  if (sortedRemoteMatches.length) {
    return sortedRemoteMatches.slice(0, 3)
  }

  return fallbackArticles
    .filter((item) => item.id !== currentId && (!currentCategory || item.tag.includes(categoryText(currentCategory))))
    .slice(0, 3)
    .map((item) => ({
      ...item,
      reasons: [item.tag || '相似场景'],
    }))
})
const articleVersions = computed(() => article.value?.versions || [])
const latestVersionId = computed(() => articleVersions.value[0]?.id ?? null)
const canArchiveArticle = computed(() => article.value?.status !== 2)

function categoryText(categoryId: number | null) {
  if (!categoryId) {
    return '未分类'
  }
  return categoryMap[categoryId] || `分类 ${categoryId}`
}

function statusText(status: number) {
  if (status === 1) {
    return '已发布'
  }
  if (status === 2) {
    return '已归档'
  }
  return '草稿'
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

function buildSourceTicketRoute(focus: 'timeline' | 'comments' | 'knowledge') {
  if (!article.value?.sourceTicket) {
    return '/tickets'
  }
  return {
    path: `/tickets/${article.value.sourceTicket.id}`,
    query: {
      fromKnowledge: '1',
      focus,
      articleId: String(article.value.id),
      articleTitle: article.value.title,
    },
  }
}

async function loadArticle() {
  const requestId = ++articleLoadRequestId
  article.value = null
  sourceTicketPreview.value = null
  relatedRemoteArticles.value = []
  const id = Number(route.params.id)
  if (!id) {
    errorMessage.value = '文章 ID 不合法'
    errorTraceId.value = ''
    return
  }

  loading.value = true
  errorMessage.value = ''
  errorTraceId.value = ''
  showAiSummary.value = true
  activeParagraphIndex.value = 0
  pinSourceTicketSummary.value = false
  showSourceCommentPreview.value = false
  showSourceTimelinePreview.value = false
  relatedSortMode.value = 'match'
  activeRelatedSourceFilter.value = 'all'

  try {
    const localDraft = getKnowledgeDraft(id)
    if (localDraft) {
      if (requestId !== articleLoadRequestId) {
        return
      }
      article.value = attachArticleSourceTicket(localDraft)
      await loadSourceTicketSummary(requestId)
      await loadRelatedArticles(requestId)
      return
    }

    const detail = await fetchKnowledgeArticleDetail(id)
    if (requestId !== articleLoadRequestId) {
      return
    }
    article.value = attachArticleSourceTicket(detail)
    await loadSourceTicketSummary(requestId)
    await loadRelatedArticles(requestId)
  } catch (error) {
    if (requestId !== articleLoadRequestId) {
      return
    }
    const result = getApiErrorDisplay(error, '文章详情加载失败，请稍后重试。')
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    console.error(error)
  } finally {
    if (requestId === articleLoadRequestId) {
      loading.value = false
    }
  }
}

async function loadSourceTicketSummary(requestId = articleLoadRequestId) {
  if (!article.value?.sourceTicket?.id) {
    sourceTicketPreview.value = null
    return
  }

  const localTicket = getLocalTicket(article.value.sourceTicket.id)
  if (localTicket) {
    if (requestId !== articleLoadRequestId) {
      return
    }
    sourceTicketPreview.value = localTicket
    return
  }

  try {
    const ticket = formatTicketDetailItem(await fetchTicketDetail(article.value.sourceTicket.id))
    if (requestId !== articleLoadRequestId) {
      return
    }
    sourceTicketPreview.value = ticket
  } catch (error) {
    if (requestId !== articleLoadRequestId) {
      return
    }
    sourceTicketPreview.value = null
    console.error(error)
  }
}

watch(pinSourceTicketSummary, (pinned) => {
  if (pinned) {
    showSourceCommentPreview.value = false
    showSourceTimelinePreview.value = false
  }
})

async function loadRelatedArticles(requestId = articleLoadRequestId) {
  if (!article.value) {
    relatedRemoteArticles.value = []
    return
  }

  try {
    const remoteArticles = await fetchKnowledgeArticles({
      categoryId: article.value.categoryId ?? undefined,
      status: 1,
    })
    if (requestId !== articleLoadRequestId || !article.value) {
      return
    }
    relatedRemoteArticles.value = remoteArticles
      .map((item) => attachArticleSourceTicket(item))
      .filter((item) => item.id !== article.value?.id)
      .sort((a, b) => scoreRelatedArticle(article.value, b) - scoreRelatedArticle(article.value, a))
  } catch (error) {
    if (requestId !== articleLoadRequestId) {
      return
    }
    relatedRemoteArticles.value = []
    console.error(error)
  }
}

function scoreRelatedArticle(
  currentArticle: KnowledgeArticleApiItem | KnowledgeArticleDraft | null,
  candidate: KnowledgeArticleApiItem | KnowledgeArticleDraft,
) {
  if (!currentArticle) {
    return 0
  }

  let score = 0
  if (currentArticle.categoryId && currentArticle.categoryId === candidate.categoryId) {
    score += 3
  }
  if (currentArticle.sourceTicket?.id && currentArticle.sourceTicket.id === candidate.sourceTicket?.id) {
    score += 4
  }

  const titleTokens = tokenizeRelatedText(currentArticle.title)
  const summaryTokens = tokenizeRelatedText(`${currentArticle.summary} ${currentArticle.content}`)
  const haystack = `${candidate.title} ${candidate.summary} ${candidate.content}`.toLowerCase()

  titleTokens.forEach((token) => {
    if (haystack.includes(token)) {
      score += 2
    }
  })
  summaryTokens.slice(0, 6).forEach((token) => {
    if (haystack.includes(token)) {
      score += 1
    }
  })
  return score
}

function buildRelatedReasons(
  currentArticle: KnowledgeArticleApiItem | KnowledgeArticleDraft | null,
  candidate: KnowledgeArticleApiItem | KnowledgeArticleDraft,
) {
  if (!currentArticle) {
    return []
  }

  const reasons: string[] = []
  if (currentArticle.sourceTicket?.id && currentArticle.sourceTicket.id === candidate.sourceTicket?.id) {
    reasons.push('同来源工单')
  }
  if (currentArticle.categoryId && currentArticle.categoryId === candidate.categoryId) {
    reasons.push('同分类')
  }

  const sharedTokens = extractSharedTokens(currentArticle, candidate).slice(0, 3)
  if (sharedTokens.length) {
    reasons.push(`命中关键词：${sharedTokens.join(' / ')}`)
  }

  if (!reasons.length) {
    reasons.push('相似处理场景')
  }
  return reasons
}

function extractSharedTokens(
  currentArticle: KnowledgeArticleApiItem | KnowledgeArticleDraft,
  candidate: KnowledgeArticleApiItem | KnowledgeArticleDraft,
) {
  const currentTokens = tokenizeRelatedText(`${currentArticle.title} ${currentArticle.summary} ${currentArticle.content}`)
  const candidateTokens = new Set(tokenizeRelatedText(`${candidate.title} ${candidate.summary} ${candidate.content}`))
  return currentTokens.filter((token, index) => candidateTokens.has(token) && currentTokens.indexOf(token) === index)
}

function tokenizeRelatedText(value: string) {
  return value
    .toLowerCase()
    .split(/[^a-z0-9\u4e00-\u9fa5]+/)
    .map((item) => item.trim())
    .filter((item) => item.length >= 2)
}

function canRestoreVersion(versionId: number) {
  return canManage.value && !('source' in (article.value || {})) && latestVersionId.value !== versionId
}

async function handleRestoreVersion(versionId: number) {
  if (!article.value || 'source' in article.value || restoringVersionId.value !== null) {
    return
  }

  restoringVersionId.value = versionId
  errorMessage.value = ''
  errorTraceId.value = ''

  try {
    article.value = attachArticleSourceTicket(await restoreKnowledgeArticleVersion(article.value.id, versionId))
    activeParagraphIndex.value = 0
  } catch (error) {
    const result = getApiErrorDisplay(error, '版本恢复失败，请稍后重试。')
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    console.error(error)
  } finally {
    restoringVersionId.value = null
  }
}

async function handleArchive() {
  if (!article.value || archiving.value) {
    return
  }

  errorMessage.value = ''
  errorTraceId.value = ''
  archiving.value = true

  try {
    if ('source' in article.value) {
      const nextDraft = updateKnowledgeDraftStatus(article.value.id, 2)
      if (nextDraft) {
        article.value = attachArticleSourceTicket(nextDraft)
      }
      return
    }

    article.value = attachArticleSourceTicket(await archiveKnowledgeArticle(article.value.id))
  } catch (error) {
    const result = getApiErrorDisplay(error, '文章归档失败，请稍后重试。')
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    console.error(error)
  } finally {
    archiving.value = false
  }
}

async function handleDelete() {
  if (!article.value || deleting.value) {
    return
  }

  const confirmed = window.confirm('删除后文章将不再出现在知识库中，确认继续吗？')
  if (!confirmed) {
    return
  }

  errorMessage.value = ''
  errorTraceId.value = ''
  deleting.value = true

  try {
    if ('source' in article.value) {
      removeKnowledgeDraft(article.value.id)
    } else {
      await deleteKnowledgeArticle(article.value.id)
    }
    saveArticleSourceTicket(article.value.id, null)
    router.push('/knowledge/articles')
  } catch (error) {
    const result = getApiErrorDisplay(error, '文章删除失败，请稍后重试。')
    errorMessage.value = result.message
    errorTraceId.value = result.traceId
    console.error(error)
  } finally {
    deleting.value = false
  }
}

function handleEdit() {
  const id = Number(route.params.id)
  router.push(`/knowledge/articles/${id}/edit`)
}

function toggleAiSummary() {
  showAiSummary.value = !showAiSummary.value
}

onMounted(() => {
  loadArticle()
})

watch(
  () => route.params.id,
  () => {
    loadArticle()
  },
)
</script>

<style scoped>
.ticket-insight-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1.25rem;
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

.knowledge-outline {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.outline-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.75rem;
  align-items: center;
  padding: 0.85rem 0.95rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.82);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.outline-item:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.18);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.outline-item span {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  font-size: 0.85rem;
  font-weight: 700;
}

.outline-item strong {
  font-size: 0.95rem;
}

.active-paragraph {
  padding: 0.95rem 1rem;
  border-radius: 18px;
  background: rgba(239, 246, 255, 0.82);
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.related-link {
  display: grid;
  gap: 0.6rem;
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

.related-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
}

.related-reason-chip {
  border: 0;
  background: rgba(37, 99, 235, 0.08);
}

.danger-button {
  color: #b91c1c;
  border-color: rgba(185, 28, 28, 0.18);
  background: rgba(254, 242, 242, 0.92);
}

.danger-button:hover:enabled {
  box-shadow: 0 10px 24px rgba(185, 28, 28, 0.12);
}

.source-ticket-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.source-ticket-copy p {
  margin: 0.3rem 0 0;
  color: #64748b;
}

.source-ticket-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.source-ticket-summary {
  margin-bottom: 1rem;
}

.source-ticket-summary-note {
  display: grid;
  gap: 0.25rem;
  margin-top: 1rem;
}

.source-ticket-summary-note strong,
.source-ticket-summary-note p {
  margin: 0;
}

.preview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}

.preview-block {
  margin-top: 1rem;
  display: grid;
  gap: 0.75rem;
}

.preview-block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  color: #64748b;
}

.preview-item {
  gap: 0.5rem;
}

.related-toolbar {
  display: grid;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.knowledge-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.knowledge-sort-select {
  padding: 0.4rem 0.65rem;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font: inherit;
}

.panel-divider {
  height: 1px;
  margin-bottom: 1rem;
  background: rgba(15, 23, 42, 0.08);
}

.panel-pinned {
  position: sticky;
  top: 1.25rem;
}

.panel-pinned .mini-list {
  gap: 0.5rem;
}

.panel-pinned .mini-item {
  padding-top: 0.6rem;
  padding-bottom: 0.6rem;
}

.panel-pinned .preview-actions {
  margin-top: 0.75rem;
}

.panel-pinned .source-ticket-summary-note {
  margin-top: 0.75rem;
  padding: 0.8rem 0.9rem;
}

.source-ticket-link {
  color: #2563eb;
  font-weight: 600;
}

.version-item {
  align-items: center;
  gap: 0.75rem;
}

.version-copy {
  display: grid;
  gap: 0.25rem;
}

.version-action {
  white-space: nowrap;
}

@media (max-width: 900px) {
  .ticket-insight-strip {
    grid-template-columns: 1fr;
  }

  .version-item {
    align-items: start;
  }

  .source-ticket-banner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
