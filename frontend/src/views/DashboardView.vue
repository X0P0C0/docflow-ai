<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav" :notice="authNotice">
      <HeroPanel
        :pending-count="pendingTicketCount"
        :knowledge-coverage="knowledgeCoverage"
        :local-count="localWorkspaceCount"
        runtime-mode=""
        :hero-headline="heroCopy.headline"
        :hero-description="heroCopy.description"
        :capability-note="heroCopy.note"
        :primary-action="heroCopy.primaryAction"
        :secondary-action="heroCopy.secondaryAction"
      />
      <MetricsGrid :items="dashboardMetrics" />

      <section class="content-grid">
        <div class="content-column">
          <TicketPanel
            :items="dashboardTickets"
            :error-message="ticketErrorMessage"
            :error-trace-id="ticketErrorTraceId"
            :title="ticketPanelCopy.title"
            :description="ticketPanelCopy.description"
            :action-text="ticketPanelCopy.actionText"
          />
          <KnowledgePanel
            :items="knowledgeArticles"
            :loading="articleLoading"
            :error-message="articleErrorMessage"
            :error-trace-id="articleErrorTraceId"
            :title="knowledgePanelCopy.title"
            :description="knowledgePanelCopy.description"
            :action-text="knowledgePanelCopy.actionText"
          />
        </div>
        <SidePanels :activities="activities" />
      </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  buildDashboardHeroCopy,
  buildDashboardKnowledgePanelCopy,
  buildDashboardMetricsCopy,
  buildDashboardTicketPanelCopy,
} from '../access-policy'
import { canAccessAiCenter, canManageKnowledgeArticles, canViewAllTickets } from '../authz'
import AppShell from '../components/layout/AppShell.vue'
import HeroPanel from '../components/dashboard/HeroPanel.vue'
import KnowledgePanel from '../components/dashboard/KnowledgePanel.vue'
import MetricsGrid from '../components/dashboard/MetricsGrid.vue'
import SidePanels from '../components/dashboard/SidePanels.vue'
import TicketPanel from '../components/dashboard/TicketPanel.vue'
import { fetchKnowledgeArticles } from '../api/knowledge'
import { fetchTickets, type TicketApiItem } from '../api/ticket'
import { listKnowledgeDrafts, mergeKnowledgeArticles } from '../mock/knowledgeDrafts'
import { activities, fallbackArticles, manageNav, tickets, workspaceNav } from '../mock/dashboard'
import { listLocalTickets, mergeTickets } from '../mock/ticketWorkspace'
import type { KnowledgeArticleApiItem, KnowledgeArticleItem, MetricItem, TicketItem } from '../types/dashboard'
import { resolveListLoadFailure } from '../utils/listLoadFailure'
import { formatTicketListItem } from '../utils/ticketPresentation'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

const route = useRoute()
const knowledgeArticles = ref<KnowledgeArticleItem[]>(fallbackArticles)
const articleLoading = ref(false)
const articleErrorMessage = ref('')
const articleErrorTraceId = ref('')
const dashboardTickets = ref<TicketItem[]>(tickets)
const ticketErrorMessage = ref('')
const ticketErrorTraceId = ref('')
const usedArticleFallbackData = ref(false)
const usedTicketFallbackData = ref(false)
let articleLoadRequestId = 0
let ticketLoadRequestId = 0
const _runtimeModeText = computed(() => getRuntimeModeText())
const _runtimeHeadline = computed(() => getRuntimeModeHeadline())
const _runtimeDataSourceMessage = computed(() => {
  if (usedTicketFallbackData.value || usedArticleFallbackData.value) {
    return '当前工作台里至少有一部分信息已回退到兜底数据，验收时请优先确认真实接口状态。'
  }

  return getRuntimeDataSourceMessage({
    usedFallbackData: false,
    subject: '工作台',
    localOnlyLabel: localWorkspaceCount.value ? '草稿内容' : '',
  })
})
const authNotice = computed(() => {
  if (route.query.reason !== 'forbidden') {
    return ''
  }
  return '当前账号没有访问刚才那个页面的权限，已为你返回可访问的工作台。'
})
const heroCopy = computed(() => buildDashboardHeroCopy({
  canViewAllTickets: canViewAllTickets(),
  canManageKnowledge: canManageKnowledgeArticles(),
  canAccessAiCenter: canAccessAiCenter(),
}))
const ticketPanelCopy = computed(() => buildDashboardTicketPanelCopy({
  canViewAllTickets: canViewAllTickets(),
}))
const knowledgePanelCopy = computed(() => buildDashboardKnowledgePanelCopy({
  canManageKnowledge: canManageKnowledgeArticles(),
}))
const pendingTicketCount = computed(() => dashboardTickets.value.filter((item) => ['新建', '处理中', '排查中'].includes(item.status)).length)
const resolvedTicketCount = computed(() => dashboardTickets.value.filter((item) => item.status === '已解决').length)
const unassignedTicketCount = computed(() => dashboardTickets.value.filter((item) => !item.assignee || item.assignee === '待分配' || item.assignee === 'Unassigned').length)
const localWorkspaceCount = computed(() => {
  const localTickets = dashboardTickets.value.filter((item) => item.source === 'local').length
  const localDrafts = listKnowledgeDrafts().length
  return localTickets + localDrafts
})
const knowledgeCoverage = computed(() => {
  const count = knowledgeArticles.value.length
  if (count >= 8) {
    return 92
  }
  if (count >= 5) {
    return 87
  }
  if (count >= 3) {
    return 78
  }
  return 64
})
const dashboardMetrics = computed<MetricItem[]>(() => buildDashboardMetricsCopy({
  ticketCount: dashboardTickets.value.length,
  resolvedCount: resolvedTicketCount.value,
  unassignedCount: unassignedTicketCount.value,
  knowledgeCount: knowledgeArticles.value.length,
  canViewAllTickets: canViewAllTickets(),
  canManageKnowledge: canManageKnowledgeArticles(),
}))

function formatApiArticle(article: KnowledgeArticleApiItem): KnowledgeArticleItem {
  return {
    id: article.id,
    tag: article.categoryId ? `知识分类 · ${article.categoryId}` : '知识文章 · 文档',
    title: article.title,
    summary: article.summary || article.content.slice(0, 72),
    author: `作者 ID：${article.authorUserId}`,
    time: article.publishTime || article.updateTime || article.createTime,
    metric: `浏览量：${article.viewCount}`,
    tagClass: article.viewCount > 1000 ? 'chip-blue' : 'chip-green',
  }
}

function buildFallbackKnowledgeArticles() {
  const merged = new Map<number, KnowledgeArticleItem>()

  mergeKnowledgeArticles([], listKnowledgeDrafts())
    .map(formatApiArticle)
    .forEach((item) => {
      merged.set(item.id, item)
    })

  fallbackArticles.forEach((item) => {
    if (!merged.has(item.id)) {
      merged.set(item.id, item)
    }
  })

  return Array.from(merged.values())
}

function buildLocalKnowledgeArticles() {
  return mergeKnowledgeArticles([], listKnowledgeDrafts()).map(formatApiArticle)
}

async function loadKnowledgeArticles() {
  const requestId = ++articleLoadRequestId
  articleLoading.value = true
  articleErrorMessage.value = ''
  articleErrorTraceId.value = ''
  usedArticleFallbackData.value = false

  try {
    const data = await fetchKnowledgeArticles()
    if (requestId !== articleLoadRequestId) {
      return
    }
    knowledgeArticles.value = mergeKnowledgeArticles(data, listKnowledgeDrafts()).map(formatApiArticle)
  } catch (error) {
    if (requestId !== articleLoadRequestId) {
      return
    }
    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: '后端接口暂时不可用',
      defaultMessage: '知识文章加载失败，请稍后重试。',
    })
    articleErrorMessage.value = result.message
    articleErrorTraceId.value = result.traceId
    usedArticleFallbackData.value = result.shouldUseFallbackData
    knowledgeArticles.value = result.shouldUseFallbackData
      ? buildFallbackKnowledgeArticles()
      : buildLocalKnowledgeArticles()
    console.error(error)
  } finally {
    if (requestId === articleLoadRequestId) {
      articleLoading.value = false
    }
  }
}

async function loadTickets() {
  const requestId = ++ticketLoadRequestId
  ticketErrorMessage.value = ''
  ticketErrorTraceId.value = ''
  usedTicketFallbackData.value = false

  try {
    const data = await fetchTickets()
    if (requestId !== ticketLoadRequestId) {
      return
    }
    dashboardTickets.value = mergeTickets(data.map(formatTicketListItem as (ticket: TicketApiItem) => TicketItem))
  } catch (error) {
    if (requestId !== ticketLoadRequestId) {
      return
    }
    const result = resolveListLoadFailure(error, {
      networkFallbackMessage: '工单接口暂时不可用',
      defaultMessage: '工单数据加载失败，请稍后重试。',
    })
    ticketErrorMessage.value = result.message
    ticketErrorTraceId.value = result.traceId
    usedTicketFallbackData.value = result.shouldUseFallbackData
    dashboardTickets.value = result.shouldUseFallbackData
      ? mergeTickets(tickets.length ? tickets : listLocalTickets())
      : listLocalTickets()
    console.error(error)
  }
}

onMounted(() => {
  loadKnowledgeArticles()
  loadTickets()
})

watch(
  () => route.fullPath,
  () => {
    loadKnowledgeArticles()
    loadTickets()
  },
)
</script>

<style scoped>


</style>
