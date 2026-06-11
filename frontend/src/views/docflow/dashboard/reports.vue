<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { http } from "@/utils/http";
import dayjs from "dayjs";
import * as echarts from "echarts";

defineOptions({ name: "DocflowReports" });

const loading = ref(false);
const reportType = ref<"daily" | "weekly">("daily");
const report = ref<any>(null);
const trendChartRef = ref<HTMLDivElement>();
const categoryChartRef = ref<HTMLDivElement>();
let trendChart: echarts.ECharts | null = null;
let categoryChart: echarts.ECharts | null = null;

async function loadReport() {
  loading.value = true;
  try {
    const { code, data } = await http.request("get", `/api/reports/${reportType.value}`);
    if (code === 200) report.value = data;
  } finally { loading.value = false; }
}

function renderCharts() {
  if (!report.value) return;

  // Trend chart
  if (trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value);
    trendChart.setOption({
      title: { text: "工单趋势", left: "center", textStyle: { fontSize: 14 } },
      tooltip: { trigger: "axis" },
      legend: { bottom: 0 },
      xAxis: { type: "category", data: [report.value.period] },
      yAxis: { type: "value" },
      series: [
        { name: "新建", type: "bar", data: [report.value.createdCount], itemStyle: { color: "#533afd" } },
        { name: "解决", type: "bar", data: [report.value.resolvedCount], itemStyle: { color: "#10b981" } },
        { name: "关闭", type: "bar", data: [report.value.closedCount], itemStyle: { color: "#64748d" } }
      ]
    });
  }

  // Category breakdown
  if (categoryChartRef.value && report.value.categoryBreakdown?.length) {
    if (!categoryChart) categoryChart = echarts.init(categoryChartRef.value);
    categoryChart.setOption({
      title: { text: "类型分布", left: "center", textStyle: { fontSize: 14 } },
      tooltip: { trigger: "item" },
      series: [{
        type: "pie",
        radius: ["40%", "70%"],
        data: report.value.categoryBreakdown.map((c: any) => ({
          name: c.category, value: c.count
        })),
        label: { formatter: "{b}: {c} ({d}%)" }
      }]
    });
  }
}

onMounted(async () => {
  await loadReport();
  setTimeout(renderCharts, 100);
});
</script>

<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold">报表分析</h2>
      <el-radio-group v-model="reportType" @change="loadReport">
        <el-radio-button value="daily">日报</el-radio-button>
        <el-radio-button value="weekly">周报</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading">
      <!-- Summary Cards -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6" v-if="report">
        <el-card shadow="never">
          <div class="text-center">
            <div class="text-3xl font-bold text-blue-600">{{ report.createdCount }}</div>
            <div class="text-sm text-gray-500 mt-1">新建工单</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="text-center">
            <div class="text-3xl font-bold text-green-600">{{ report.resolvedCount }}</div>
            <div class="text-sm text-gray-500 mt-1">已解决</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="text-center">
            <div class="text-3xl font-bold text-orange-500">{{ report.breachedCount }}</div>
            <div class="text-sm text-gray-500 mt-1">SLA 违约</div>
          </div>
        </el-card>
        <el-card shadow="never">
          <div class="text-center">
            <div class="text-3xl font-bold text-purple-600">{{ report.avgResolutionHours?.toFixed(1) || '-' }}h</div>
            <div class="text-sm text-gray-500 mt-1">平均解决时长</div>
          </div>
        </el-card>
      </div>

      <!-- Charts -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <el-card shadow="never">
          <div ref="trendChartRef" style="height: 300px"></div>
        </el-card>
        <el-card shadow="never">
          <div ref="categoryChartRef" style="height: 300px"></div>
        </el-card>
      </div>

      <!-- Top Assignees -->
      <el-card shadow="never" v-if="report?.topAssignees?.length" class="mb-4">
        <template #header><span class="font-semibold">处理人排行</span></template>
        <el-table :data="report.topAssignees" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="assigneeName" label="处理人" />
          <el-table-column prop="resolvedCount" label="解决数" width="100" />
          <el-table-column label="平均耗时" width="120">
            <template #default="{ row }">{{ row.avgHours?.toFixed(1) || '-' }}h</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- AI Summary -->
      <el-card shadow="never" v-if="report?.summary">
        <template #header>
          <span class="font-semibold">
            <el-icon class="mr-1"><component :is="useRenderIcon('ep:document')" /></el-icon>
            AI 摘要
          </span>
        </template>
        <p class="text-gray-700 leading-relaxed">{{ report.summary }}</p>
      </el-card>
    </div>
  </div>
</template>
