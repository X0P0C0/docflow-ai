<script setup lang="ts">
import { computed, onMounted, ref, nextTick } from "vue";
import { useRouter } from "vue-router";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { getTickets, getTicketStats, type TicketListItem, type TicketStats } from "@/api/tickets";
import { getKnowledgeArticles, type KnowledgeArticle } from "@/api/knowledge";
import { getTicketStatusLabel } from "@/constants/tickets";
import * as echarts from "echarts";
import dayjs from "dayjs";

defineOptions({ name: "DocflowDashboard" });

const router = useRouter();
const loading = ref(false);
const tickets = ref<TicketListItem[]>([]);
const articles = ref<KnowledgeArticle[]>([]);
const ticketStats = ref<TicketStats | null>(null);
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
      change: s ? + 今日新增 : null,
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
      change: s ? ${Math.round(((s.resolved / Math.max(s.total, 1)) * 100))}% 解决率 : null,
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

// ── Recent Items ──
const recentTickets = computed(() => tickets.value.slice(0, 6));
const recentArticles = computed(() => articles.value.slice(0, 4));

// ── Helpers ──
function statusTagType(status?: number | null) {
  if (status === 3) return "success";
  if (status === 4) return "info";
  if (status === 2) return "warning";
  return "";
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format("MM-DD HH:mm") : "-";
}

function formatRelative(value?: string | null) {
  if (!value) return "";
  const d = dayjs(value);
  const diff = dayjs().diff(d, "hour");
  if (diff < 1) return "刚刚";
  if (diff < 24) return ${diff}h;
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
    xAxis: { type: "value", minInterval: 1, splitLine: { lineStyle: { type: "dashed", color: colors.hairline } }, axisLabel: { fontSize: 10 } },
    yAxis: { type: "category", data: priorityData.map((d) => d.name), axisLabel: { fontSize: 10, fontWeight: 600 }, axisTick: { show: false }, axisLine: { show: false } },
    series: [{
      type: "bar",
      data: priorityData.map((d) => d.value),
      barWidth: 14,
      itemStyle: { borderRadius: [0, 4, 4, 0], color: (p: any) => ["#9ca3af", "#3b82f6", "#f97316", "#ef4444"][p.dataIndex] },
      label: { show: true, position: "right", fontSize: 11, fontWeight: 600 },
    }],
  });
}

// ── Load Data ──
async function loadData() {
  loading.value = true;
  try {
    const [tRes, aRes, sRes] = await Promise.all([
      getTickets({ size: 50 }),
      getKnowledgeArticles({ size: 50 }),
      getTicketStats(),
    ]);
    if (tRes.code === 200) tickets.value = tRes.data.records;
    if (aRes.code === 200) articles.value = aRes.data.records;
    if (sRes.code === 200) ticketStats.value = sRes.data;
    await nextTick();
    renderCharts();
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
  <div class="dashboard-stripe" v-loading="loading">
    <!-- ── Hero ── -->
    <div class="hero">
      <div>
        <h1 class="hero-title">控制台</h1>
        <p class="hero-subtitle">{{ dayjs().format("YYYY年M月D日 dddd") }} · 系统运行概况</p>
      </div>
      <el-button class="refresh-btn" text @click="loadData" :loading="loading">
        <component :is="useRenderIcon('ri:refresh-line')" />
        刷新
      </el-button>
    </div>

    <!-- ── Stat Cards ── -->
    <div class="stat-grid">
      <div
        v-for="(card, i) in stats"
        :key="i"
        class="stat-card"
        :style="{ borderTopColor: card.accent }"
        @click="goTo(card.link)"
      >
        <div class="stat-icon-box" :style="{ background: card.bg, color: card.accent }">
          <component :is="useRenderIcon(card.icon, { width: '20px', height: '20px' })" />
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ card.value }}</span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
        <span v-if="card.change" class="stat-change">{{ card.change }}</span>
      </div>
    </div>

    <!-- ── Main Grid ── -->
    <div class="main-grid">
      <!-- Left: Urgency + Charts -->
      <div class="left-col">
        <!-- Urgency -->
        <div class="card">
          <div class="card-header">
            <span class="card-title">优先级分布</span>
            <el-button text size="small" class="card-link" @click="goTo('/tickets/list')">全部</el-button>
          </div>
          <div class="urgency-list">
            <div v-for="item in urgencyStats" :key="item.label" class="urgency-row">
              <span class="urgency-label">{{ item.label }}</span>
              <div class="urgency-bar-track">
                <div class="urgency-bar-fill" :style="{ width: item.pct + '%', background: item.color }" />
              </div>
              <span class="urgency-value" :style="{ color: item.color }">{{ item.value }}</span>
            </div>
          </div>
        </div>

        <!-- Charts Row -->
        <div class="charts-row">
          <div class="card chart-card">
            <div class="card-header"><span class="card-title">工单状态</span></div>
            <div ref="statusChartRef" class="chart-box" />
          </div>
          <div class="card chart-card">
            <div class="card-header"><span class="card-title">优先级排行</span></div>
            <div ref="priorityChartRef" class="chart-box" />
          </div>
        </div>
      </div>

      <!-- Right: Today + Recent -->
      <div class="right-col">
        <!-- Today -->
        <div class="card">
          <div class="card-header"><span class="card-title">今日动态</span></div>
          <div class="today-grid">
            <div v-for="item in todayStats" :key="item.label" class="today-item">
              <div class="today-icon" :style="{ background: item.color + '15', color: item.color }">
                <component :is="useRenderIcon(item.icon, { width: '18px', height: '18px' })" />
              </div>
              <div>
                <div class="today-value">{{ item.value }}</div>
                <div class="today-label">{{ item.label }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Tickets -->
        <div class="card">
          <div class="card-header">
            <span class="card-title">最近工单</span>
            <el-button text size="small" class="card-link" @click="goTo('/tickets/list')">全部</el-button>
          </div>
          <div v-if="recentTickets.length" class="recent-list">
            <div
              v-for="ticket in recentTickets"
              :key="ticket.id"
              class="recent-item"
              @click="goTo(/tickets/)"
            >
              <div class="recent-left">
                <p class="recent-title">{{ ticket.title }}</p>
                <p class="recent-meta">{{ ticket.ticketNo }} · {{ formatRelative(ticket.updateTime) }}</p>
              </div>
              <el-tag :type="statusTagType(ticket.status)" size="small">{{ getTicketStatusLabel(ticket.status) }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无工单" :image-size="48" />
        </div>

        <!-- Recent Articles -->
        <div class="card">
          <div class="card-header">
            <span class="card-title">知识库</span>
            <el-button text size="small" class="card-link" @click="goTo('/knowledge/articles')">全部</el-button>
          </div>
          <div v-if="recentArticles.length" class="recent-list">
            <div
              v-for="article in recentArticles"
              :key="article.id"
              class="recent-item"
              @click="goTo(/knowledge/articles/)"
            >
              <div class="recent-left">
                <p class="recent-title">{{ article.title }}</p>
                <p class="recent-meta">{{ article.status === 1 ? '已发布' : '草稿' }} · {{ formatDate(article.publishTime || article.createTime) }}</p>
              </div>
              <el-tag :type="article.status === 1 ? 'success' : 'info'" size="small">{{ article.status === 1 ? '已发布' : '草稿' }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无文章" :image-size="48" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Stripe Design System: Dashboard ── */

.dashboard-stripe {
  padding: 0;
  min-height: 100%;
}

/* Hero */
.hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
}
.hero-title {
  font-size: 26px;
  font-weight: 300;
  color: #0d253d;
  letter-spacing: -0.26px;
  margin: 0 0 6px 0;
}
.hero-subtitle {
  font-size: 14px;
  color: #64748d;
  margin: 0;
}
.refresh-btn {
  color: #64748d;
  font-size: 13px;
}

/* Stat Grid */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #ffffff;
  border: 1px solid #e3e8ee;
  border-top: 3px solid #533afd;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: box-shadow 0.15s ease, transform 0.15s ease;
  position: relative;
}
.stat-card:hover {
  box-shadow: 0 4px 20px rgba(83, 58, 253, 0.08);
  transform: translateY(-1px);
}
.stat-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-body {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #0d253d;
  line-height: 1.1;
  letter-spacing: -0.5px;
}
.stat-label {
  font-size: 13px;
  color: #64748d;
  margin-top: 2px;
}
.stat-change {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 11px;
  color: #64748d;
  background: #f6f9fc;
  padding: 2px 8px;
  border-radius: 999px;
}

/* Main Grid */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
}
.left-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.right-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Cards */
.card {
  background: #ffffff;
  border: 1px solid #e3e8ee;
  border-radius: 12px;
  padding: 20px 24px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #0d253d;
}
.card-link {
  color: #533afd;
  font-size: 13px;
  font-weight: 500;
}

/* Urgency */
.urgency-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.urgency-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.urgency-label {
  font-size: 12px;
  font-weight: 500;
  color: #64748d;
  width: 56px;
  flex-shrink: 0;
}
.urgency-bar-track {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}
.urgency-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.5s ease;
}
.urgency-value {
  font-size: 13px;
  font-weight: 700;
  width: 24px;
  text-align: right;
  flex-shrink: 0;
}

/* Charts */
.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.chart-card {
  min-height: 240px;
}
.chart-box {
  width: 100%;
  height: 200px;
}

/* Today */
.today-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.today-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  background: #f6f9fc;
}
.today-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.today-value {
  font-size: 20px;
  font-weight: 700;
  color: #0d253d;
  line-height: 1.2;
}
.today-label {
  font-size: 11px;
  color: #64748d;
}

/* Recent List */
.recent-list {
  display: flex;
  flex-direction: column;
}
.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.1s;
  border-radius: 4px;
  padding-left: 4px;
  padding-right: 4px;
}
.recent-item:last-child {
  border-bottom: none;
}
.recent-item:hover {
  background: #f6f9fc;
}
.recent-left {
  flex: 1;
  min-width: 0;
  margin-right: 10px;
}
.recent-title {
  font-size: 13px;
  font-weight: 500;
  color: #0d253d;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.recent-meta {
  font-size: 11px;
  color: #94a3b8;
  margin: 3px 0 0 0;
}

/* Responsive */
@media (max-width: 1024px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-grid {
    grid-template-columns: 1fr;
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
  .today-grid {
    grid-template-columns: 1fr;
  }
}
</style>