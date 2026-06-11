<script setup lang="ts">
import { computed, onMounted, ref, nextTick } from "vue";
import { useRouter } from "vue-router";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { getTickets, getTicketStats, getDashboard, type TicketListItem, type TicketStats, type DashboardData } from "@/api/tickets";
import { getKnowledgeArticles, type KnowledgeArticle } from "@/api/knowledge";
import { getTicketStatusLabel } from "@/constants/tickets";
import { extractArray } from "@/utils/api-helper";
import * as echarts from "echarts";
import dayjs from "dayjs";

defineOptions({ name: "DocflowDashboard" });

const router = useRouter();
const loading = ref(false);
const tickets = ref<TicketListItem[]>([]);
const articles = ref<KnowledgeArticle[]>([]);
const ticketStats = ref<TicketStats | null>(null);
const dashboardData = ref<DashboardData | null>(null);
const statusChartRef = ref<HTMLDivElement>();
const priorityChartRef = ref<HTMLDivElement>();
let statusChart: echarts.ECharts | null = null;
let priorityChart: echarts.ECharts | null = null;

// ── Stripe Design Tokens ──
const colors = {
  primary: "#533afd",
  primarySoft: "#665efd",
  ink: "#0d253d",
  inkSecondary: "#273951",
  inkMute: "#64748d",
  canvas: "#ffffff",
  canvasSoft: "#f6f9fc",
  hairline: "#e3e8ee",
  ruby: "#ea2261",
  success: "#10b981",
  warning: "#f59e0b",
  info: "#3b82f6",
};

// ── Stats Cards ──
const stats = computed(() => {
  const s = ticketStats.value;
  return [
    {
      label: "全部工单",
      value: s?.total ?? tickets.value.length,
      icon: "ri:inbox-line",
      accent: colors.primary,
      bg: "#f5f3ff",
      link: "/tickets/list",
      change: s ? `+${s.newCount ?? 0} 今日新增` : null,
    },
    {
      label: "处理中",
      value: s?.inProgress ?? tickets.value.filter((t) => t.status === 2).length,
      icon: "ri:loader-4-line",
      accent: colors.warning,
      bg: "#fffbeb",
      link: "/tickets/list?status=2",
      change: null,
    },
    {
      label: "已解决",
      value: s?.resolved ?? tickets.value.filter((t) => t.status === 3 || t.status === 4).length,
      icon: "ri:checkbox-circle-line",
      accent: colors.success,
      bg: "#f0fdf4",
      link: "/tickets/list?status=3",
      change: s ? `${Math.round(((s.resolved / Math.max(s.total, 1)) * 100))}% 解决率` : null,
    },
    {
      label: "知识文章",
      value: articles.value.length,
      icon: "ri:book-open-line",
      accent: "#7c3aed",
      bg: "#faf5ff",
      link: "/knowledge/articles",
      change: null,
    },
  ];
});

// ── Urgency Bar ──
const urgencyStats = computed(() => {
  const max = Math.max(...[4, 3, 2, 1].map((p) => tickets.value.filter((t) => t.priority === p).length), 1);
  return [
    { label: "P4 紧急", value: tickets.value.filter((t) => t.priority === 4).length, color: "#ef4444", pct: 0 },
    { label: "P3 高", value: tickets.value.filter((t) => t.priority === 3).length, color: "#f97316", pct: 0 },
    { label: "P2 普通", value: tickets.value.filter((t) => t.priority === 2).length, color: "#3b82f6", pct: 0 },
    { label: "P1 低", value: tickets.value.filter((t) => t.priority === 1).length, color: "#9ca3af", pct: 0 },
  ].map((item) => ({ ...item, pct: Math.round((item.value / max) * 100) }));
});

// ── Today Stats ──
const todayStats = computed(() => {
  const today = dayjs().format("YYYY-MM-DD");
  return [
    { label: "今日新建", value: tickets.value.filter((t) => t.createTime?.startsWith(today)).length, icon: "ri:add-circle-line", color: colors.primary },
    { label: "今日解决", value: tickets.value.filter((t) => t.updateTime?.startsWith(today) && (t.status === 3 || t.status === 4)).length, icon: "ri:check-double-line", color: colors.success },
    { label: "待分配", value: tickets.value.filter((t) => !t.assigneeUserId).length, icon: "ri:user-unfollow-line", color: colors.ruby },
  ];
});

// Recent Items
const recentTickets = computed(() => tickets.value.slice(0, 4));
const recentArticles = computed(() => articles.value.slice(0, 2));

// Helpers
function statusTagType(status?: number | null) {
  if (status === 3) return "success";
  if (status === 4) return "info";
  if (status === 2) return "warning";
  return "info";
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format("MM-DD HH:mm") : "-";
}

function formatRelative(value?: string | null) {
  if (!value) return "";
  const d = dayjs(value);
  const diff = dayjs().diff(d, "hour");
  if (diff < 1) return "刚刚";
  if (diff < 24) return `${diff}h`;
  return d.format("MM-DD");
}

// ── Charts ──
function renderCharts() {
  if (!statusChartRef.value || !priorityChartRef.value) return;

  if (!statusChart) statusChart = echarts.init(statusChartRef.value);
  const statusData = [
    { value: tickets.value.filter((t) => t.status === 1).length, name: "新建" },
    { value: tickets.value.filter((t) => t.status === 2).length, name: "处理中" },
    { value: tickets.value.filter((t) => t.status === 3).length, name: "已解决" },
    { value: tickets.value.filter((t) => t.status === 4).length, name: "已关闭" },
  ].filter((d) => d.value > 0);

  statusChart.setOption({
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    series: [{
      type: "pie",
      radius: ["60%", "82%"],
      center: ["50%", "50%"],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 3, borderColor: colors.canvas, borderWidth: 2 },
      label: { show: false },
      emphasis: { scaleSize: 8 },
      data: statusData,
      color: ["#94a3b8", "#f59e0b", "#10b981", "#533afd"],
    }],
  });

  if (!priorityChart) priorityChart = echarts.init(priorityChartRef.value);
  const priorityData = [
    { name: "P4", value: tickets.value.filter((t) => t.priority === 4).length },
    { name: "P3", value: tickets.value.filter((t) => t.priority === 3).length },
    { name: "P2", value: tickets.value.filter((t) => t.priority === 2).length },
    { name: "P1", value: tickets.value.filter((t) => t.priority === 1).length },
  ];

  priorityChart.setOption({
    tooltip: { trigger: "axis" },
    grid: { left: 4, right: 16, top: 4, bottom: 4, containLabel: true },
    xAxis: { type: "value", minInterval: 1, splitLine: { lineStyle: { type: "dashed", color: colors.hairline } }, axisLabel: { fontSize: 11 } },
    yAxis: { type: "category", data: priorityData.map((d) => d.name), axisLabel: { fontSize: 12, fontWeight: 600 }, axisTick: { show: false }, axisLine: { show: false } },
    series: [{
      type: "bar",
      data: priorityData.map((d) => d.value),
      barWidth: 14,
      itemStyle: { borderRadius: [0, 4, 4, 0], color: (p: any) => ["#9ca3af", "#3b82f6", "#f97316", "#ef4444"][p.dataIndex] },
      label: { show: true, position: "right", fontSize: 12, fontWeight: 600 },
    }],
  });
}

// ── Load Data ──
async function loadData() {
  loading.value = true;
  try {
    const [tRes, aRes, sRes, dRes] = await Promise.all([
      getTickets({ size: 50 }),
      getKnowledgeArticles({ size: 50 }),
      getTicketStats(),
      getDashboard(),
    ]);
    if (tRes.code === 200) tickets.value = extractArray(tRes.data);
    if (dRes) dashboardData.value = dRes;
    if (aRes.code === 200) articles.value = extractArray(aRes.data);
    if (sRes.code === 200) ticketStats.value = sRes.data;
    await nextTick();
    // Delay chart rendering to ensure DOM has dimensions
    setTimeout(() => renderCharts(), 100);
  } finally {
    loading.value = false;
  }
}

function goTo(path: string) {
  router.push(path);
}

function handleResize() {
  statusChart?.resize();
  priorityChart?.resize();
}

onMounted(() => {
  loadData();
  window.addEventListener("resize", handleResize);
});
</script>

<template>
  <div class="db" v-loading="loading">
    <div class="db-top">
      <div class="db-top-left">
        <h1 class="db-hero-title">控制台</h1>
        <span class="db-hero-date">{{ dayjs().format("YYYY年M月D日 dddd") }}</span>
      </div>
      <div class="db-today-inline">
        <span v-for="item in todayStats" :key="item.label" class="db-today-tag">
          <i class="db-today-dot" :style="{ background: item.color }"></i>
          {{ item.label }} <strong>{{ item.value }}</strong>
        </span>
      </div>
      <el-button class="db-refresh" text size="small" @click="loadData" :loading="loading">
        <component :is="useRenderIcon('ri:refresh-line', { width: '16px', height: '16px' })" />
      </el-button>
    </div>

    <!-- Stat Cards: chip next to number -->
    <div class="db-stats">
      <div v-for="(card, i) in stats" :key="i" class="db-stat-card" :style="{ '--accent': card.accent }" @click="goTo(card.link)">
        <div class="db-stat-icon" :style="{ background: card.bg, color: card.accent }">
          <component :is="useRenderIcon(card.icon, { width: '22px', height: '22px' })" />
        </div>
        <div class="db-stat-val">{{ card.value }}</div>
        <span v-if="card.change" class="db-stat-chip">{{ card.change }}</span>
        <div class="db-stat-lbl">{{ card.label }}</div>
      </div>
    </div>

    <div class="db-main">
      <div class="db-card db-list-card">
        <div class="db-card-hd">
          <span class="db-card-tt">最近工单</span>
          <el-button text size="small" class="db-card-link" @click="goTo('/tickets/list')">
            全部 <component :is="useRenderIcon('ri:arrow-right-s-line', { width: '14px', height: '14px' })" />
          </el-button>
        </div>
        <div v-if="recentTickets.length" class="db-items">
          <div v-for="ticket in recentTickets" :key="ticket.id" class="db-item" @click="goTo(`/tickets/${ticket.id}`)">
            <span class="db-item-prio" :class="'prio-' + (ticket.priority || 1)">P{{ ticket.priority || '-' }}</span>
            <span class="db-item-tt">{{ ticket.title }}</span>
            <el-tag :type="statusTagType(ticket.status)" size="small" effect="plain" round>
              {{ getTicketStatusLabel(ticket.status) }}
            </el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无工单" :image-size="20" />
      </div>

      <div class="db-card db-list-card">
        <div class="db-card-hd">
          <span class="db-card-tt">知识库</span>
          <el-button text size="small" class="db-card-link" @click="goTo('/knowledge/articles')">
            全部 <component :is="useRenderIcon('ri:arrow-right-s-line', { width: '14px', height: '14px' })" />
          </el-button>
        </div>
        <div v-if="recentArticles.length" class="db-items">
          <div v-for="article in recentArticles" :key="article.id" class="db-item" @click="goTo(`/knowledge/articles/${article.id}`)">
            <span class="db-item-icon-doc">
              <component :is="useRenderIcon('ri:file-text-line', { width: '14px', height: '14px' })" />
            </span>
            <span class="db-item-tt">{{ article.title }}</span>
            <el-tag :type="article.status === 1 ? 'success' : 'info'" size="small" effect="plain" round>
              {{ article.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </div>
        </div>
        <el-empty v-else description="暂无文章" :image-size="20" />
      </div>

      <div class="db-right-charts">
        <div class="db-card db-chart-card">
          <div class="db-card-hd"><span class="db-card-tt">工单状态</span></div>
          <div ref="statusChartRef" class="db-chart-box" />
        </div>
        <div class="db-card db-chart-card">
          <div class="db-card-hd"><span class="db-card-tt">优先级</span></div>
          <div ref="priorityChartRef" class="db-chart-box" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
/* === Dashboard: One-Page Compact === */

.db {
  height: calc(100vh - 132px);
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

/* ── Top Bar ── */
.db-top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.db-top-left { display: flex; align-items: baseline; gap: 8px; }
.db-hero-title { 
  font-size: 22px; font-weight: 800; color: #0d253d; margin: 0; line-height: 1; letter-spacing: -0.3px;
  animation: slideInDown 0.5s ease-out forwards;
}
.db-hero-date { font-size: 13px; color: #94a3b8; }
.db-hero-welcome { font-size: 13px; color: #533afd; font-weight: 600; margin-left: 12px; }
.db-today-inline { display: flex; gap: 4px; margin-left: auto; }
.db-today-tag {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 13px; color: #64748d; background: #f8fafc;
  padding: 4px 12px; border-radius: 8px; font-weight: 500; line-height: 1.4;
  border: 1px solid #e8ecf1;
}
.db-today-tag strong { color: #0d253d; font-weight: 700; }
.db-today-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; }
.db-refresh { color: #94a3b8; flex-shrink: 0; padding: 2px 4px; }

/* ── Stats (label on right, big+bold) ── */
.db-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  flex-shrink: 0;
}
.db-stat-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 10px;
  padding: 14px 16px; display: flex; align-items: center; gap: 12px;
  cursor: pointer; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); position: relative;
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}
.db-stat-card:nth-child(1) { animation-delay: 0.1s; }
.db-stat-card:nth-child(2) { animation-delay: 0.2s; }
.db-stat-card:nth-child(3) { animation-delay: 0.3s; }
.db-stat-card:nth-child(4) { animation-delay: 0.4s; }
.db-stat-card::before {
  content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, var(--accent), transparent); 
  opacity: 0; transition: opacity 0.25s;
}
.db-stat-card:hover::before { opacity: 1; }
.db-stat-card:hover {
  border-color: var(--accent);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.db-stat-card:hover::before { opacity: 1; }
.db-stat-icon {
  width: 36px; height: 36px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.db-stat-val {
  font-size: 28px; font-weight: 800; color: #0d253d; line-height: 1;
  flex-shrink: 0; letter-spacing: -0.5px;
  font-feature-settings: "tnum";
  transition: all 0.3s ease;
}
.db-stat-card:hover .db-stat-val {
  transform: scale(1.05);
  color: var(--accent);
}
.db-stat-lbl {
  font-size: 19px; font-weight: 700; color: #0d253d;
  margin-left: auto; padding-right: 4px;
  white-space: nowrap;
}
.db-stat-chip {
  display: inline-flex; align-items: center;
  font-size: 11px; font-weight: 700; color: #10b981;
  background: #ecfdf5; padding: 2px 8px; border-radius: 999px;
  white-space: nowrap; flex-shrink: 0; letter-spacing: 0.2px;
}

/* ── Main 3-Col ── */
.db-main {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  flex: 1;
  min-height: 0;
}

/* ── Cards ── */
.db-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 10px;
  padding: 12px 16px; display: flex; flex-direction: column; min-height: 0;
  transition: box-shadow 0.2s, transform 0.2s;
  animation: fadeInUp 0.6s ease-out forwards;
  opacity: 0;
  animation-delay: 0.3s;
}
.db-card:hover { 
  box-shadow: 0 4px 20px rgba(0,0,0,0.08); 
  transform: translateY(-2px);
}
.db-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.db-card-hd {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px; flex-shrink: 0;
}
.db-card-tt { font-size: 14px; font-weight: 600; color: #0d253d; }
.db-card-link { color: #533afd; font-size: 12px; font-weight: 600; }

/* ── Charts ── */
.db-chart-card { min-height: 0; }
.db-chart-box { width: 100%; flex: 1; min-height: 0; }

/* ── Right Charts Stack ── */
.db-right-charts { display: flex; flex-direction: column; gap: 8px; min-height: 0; }
.db-right-charts .db-chart-card { flex: 1; min-height: 0; }
.db-list-card { flex: 1; min-height: 0; overflow: hidden; }

/* ── Items ── */
.db-items { flex: 1; overflow: hidden; min-height: 0; }
.db-item {
  display: flex; align-items: center; gap: 8px; padding: 7px 4px;
  border-radius: 4px; cursor: pointer; transition: background 0.12s; flex-shrink: 0;
}
.db-item { border-radius: 6px; margin: 0 -4px; padding-left: 8px; padding-right: 8px; }
.db-item { transition: all 0.2s ease; }
.db-item:hover { 
  background: #f0f4ff; 
  transform: translateX(4px);
}
.db-item + .db-item { border-top: 1px solid #f1f5f9; }

.db-item-prio {
  width: 22px; height: 22px; border-radius: 4px;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; font-weight: 700; flex-shrink: 0; color: #fff;
}
.prio-1 { background: #cbd5e1; } .prio-2 { background: #3b82f6; }
.prio-3 { background: #f59e0b; } .prio-4 { background: #ef4444; }

.db-item-icon-doc {
  width: 22px; height: 22px; border-radius: 4px;
  background: #f5f3ff; color: #7c3aed;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.db-item-tt {
  flex: 1; font-size: 13px; font-weight: 500; color: #0d253d;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; min-width: 0;
}

/* ── Empty ── */
.db :deep(.el-empty) { padding: 8px 0; }

@media (max-width: 1100px) {
  .db-main { grid-template-columns: 1fr 1fr; }
  .db-right-charts { display: none; }
}
@media (max-width: 720px) {
  .db-stats { grid-template-columns: repeat(2, 1fr); }
  .db-main { grid-template-columns: 1fr; }
  .db-today-inline { display: none; }
}

/* Dark mode */
.dark .db-hero-title { color: #f1f5f9; }
.dark .db-hero-welcome { color: #818cf8; }
.dark .db-stat-card { background: #1e293b; border-color: #334155; }
.dark .db-stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
.dark .db-stat-val { color: #f1f5f9; }
.dark .db-stat-lbl { color: #94a3b8; }
.dark .db-card { background: #1e293b; border-color: #334155; }
.dark .db-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.15); }
.dark .db-card-tt { color: #f1f5f9; }
.dark .db-item-tt { color: #e2e8f0; }
.dark .db-item:hover { background: rgba(99, 102, 241, 0.08); }
</style>