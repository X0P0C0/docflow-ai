<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import dayjs from "dayjs";
import { useRoute, useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import {
  getTickets, getTicketStats, getTicketAssignees,
  batchUpdateStatus, batchAssign, exportTickets,
  type TicketListItem, type TicketAssigneeOption, type TicketStats,
  type BatchOperationResponse
} from "@/api/tickets";
import { getTicketPriorityLabel, getTicketStatusLabel, getTicketTypeLabel } from "@/constants/tickets";
import { extractPaginated } from "@/utils/api-helper";

defineOptions({ name: "DocflowTicketList" });

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const batchLoading = ref(false);
const tickets = ref<TicketListItem[]>([]);
const assignees = ref<TicketAssigneeOption[]>([]);
const showFilters = ref(false);
const selectedIds = ref<number[]>([]);
const batchDialogVisible = ref(false);
const batchAction = ref<"status" | "assign">("status");
const batchStatus = ref<number>(2);
const batchAssignee = ref<number | null>(null);
const currentPage = ref(1);
const pageSize = ref(20);
const totalTickets = ref(0);
const ticketStats = ref<TicketStats | null>(null);

const filters = ref({
  keyword: "",
  status: undefined as number | undefined,
  priority: undefined as number | undefined,
  type: undefined as string | undefined
});

const activeQuickFilter = ref("all");

const quickFilters = [
  { label: "全部", value: "all" },
  { label: "待分配", value: "unassigned" },
  { label: "处理中", value: "in-progress" },
  { label: "已解决", value: "resolved" }
];

const statCards = computed(() => {
  const s = ticketStats.value;
  return [
    { label: "全部工单", value: s?.total ?? totalTickets.value, color: "#533afd", bg: "#f5f3ff", icon: "ri:inbox-line", filter: "all" },
    { label: "处理中", value: s?.inProgress ?? 0, color: "#d97706", bg: "#fffbeb", icon: "ri:loader-4-line", filter: "in-progress" },
    { label: "待分配", value: s?.unassigned ?? 0, color: "#ef4444", bg: "#fef2f2", icon: "ri:user-unfollow-line", filter: "unassigned" },
    { label: "已解决", value: s?.resolved ?? 0, color: "#10b981", bg: "#f0fdf4", icon: "ri:checkbox-circle-line", filter: "resolved" }
  ];
});

const displayedTickets = computed(() => tickets.value);

const allSelected = computed({
  get: () => displayedTickets.value.length > 0 && selectedIds.value.length === displayedTickets.value.length,
  set: (val: boolean) => { selectedIds.value = val ? displayedTickets.value.map(t => t.id) : []; }
});

const isIndeterminate = computed(() =>
  selectedIds.value.length > 0 && selectedIds.value.length < displayedTickets.value.length
);

function statusTagType(status?: number | null) {
  if (status === 3) return "success";
  if (status === 4) return "info";
  if (status === 2) return "warning";
  if (status === 1) return "info";
  return "danger";
}

function priorityTagType(level?: number | null) {
  if (level === 4) return "danger";
  if (level === 3) return "warning";
  if (level === 2) return "info";
  return "info";
}

function typeTagType(type?: string | null) {
  if (type === "INCIDENT") return "danger";
  if (type === "TASK") return "primary";
  if (type === "QUESTION") return "success";
  return "info";
}

function formatDate(value?: string | null) {
  return value ? dayjs(value).format("MM-DD HH:mm") : "-";
}

function goTo(path: string) { router.push(path); }

function applyQueryParams() {
  const q = route.query;
  if (q.status) {
    const s = Number(q.status);
    if ([1, 2, 3, 4].includes(s)) {
      filters.value.status = s;
      showFilters.value = true;
      if (s === 2) activeQuickFilter.value = "in-progress";
      else if (s === 3 || s === 4) activeQuickFilter.value = "resolved";
    }
  }
  if (q.priority) {
    const p = Number(q.priority);
    if ([1, 2, 3, 4].includes(p)) { filters.value.priority = p; showFilters.value = true; }
  }
}

async function loadTickets() {
  loading.value = true;
  try {
    const apiStatus = activeQuickFilter.value === "in-progress" ? 2
      : activeQuickFilter.value === "resolved" ? 3
      : filters.value.status;
    const apiAssignee = activeQuickFilter.value === "unassigned" ? -1 : undefined;
    const { code, data } = await getTickets({
      keyword: filters.value.keyword.trim() || undefined,
      status: apiStatus, priority: filters.value.priority,
      type: filters.value.type, assigneeUserId: apiAssignee,
      page: currentPage.value, size: pageSize.value
    });
    if (code !== 200) throw new Error("加载失败");
    tickets.value = Array.isArray(data) ? data : (data?.records || []);
    totalTickets.value = Array.isArray(data) ? data.length : (data?.total || 0);
    selectedIds.value = [];
  } catch (error) {
    tickets.value = [];
    message(error instanceof Error ? error.message : "加载失败", { type: "error" });
  } finally { loading.value = false; }
}

async function loadAssignees() {
  try { const { code, data } = await getTicketAssignees(); if (code === 200) assignees.value = data; } catch {}
}

async function loadStats() {
  try { const { code, data } = await getTicketStats(); if (code === 200) ticketStats.value = data; } catch {}
}

function handlePageChange(page: number) { currentPage.value = page; loadTickets(); }
function handleSizeChange(size: number) { pageSize.value = size; currentPage.value = 1; loadTickets(); }

function applyQuickFilter(val: string) {
  activeQuickFilter.value = val; currentPage.value = 1; filters.value.status = undefined; loadTickets();
}

function clearFilters() {
  filters.value = { keyword: "", status: undefined, priority: undefined, type: undefined };
  activeQuickFilter.value = "all"; currentPage.value = 1; loadTickets();
}

function openBatchDialog(action: "status" | "assign") {
  if (!selectedIds.value.length) { message("请先选择工单", { type: "warning" }); return; }
  batchAction.value = action; batchDialogVisible.value = true;
}

async function handleBatch() {
  batchLoading.value = true;
  try {
    let result: BatchOperationResponse;
    if (batchAction.value === "status") {
      const { code, data } = await batchUpdateStatus({ ticketIds: selectedIds.value, status: batchStatus.value });
      if (code !== 200) throw new Error("批量操作失败");
      result = data;
    } else {
      if (!batchAssignee.value) return;
      const { code, data } = await batchAssign({ ticketIds: selectedIds.value, assigneeUserId: batchAssignee.value });
      if (code !== 200) throw new Error("批量指派失败");
      result = data;
    }
    message(`成功 ${result.successCount} 条，失败 ${result.failCount} 条`, { type: "success" });
    batchDialogVisible.value = false; selectedIds.value = []; loadTickets(); loadStats();
  } catch (error) {
    message(error instanceof Error ? error.message : "操作失败", { type: "error" });
  } finally { batchLoading.value = false; }
}

async function handleExport() {
  try {
    const blob = await exportTickets({ keyword: filters.value.keyword.trim() || undefined, status: filters.value.status, priority: filters.value.priority, type: filters.value.type });
    const url = URL.createObjectURL(blob as Blob);
    const a = document.createElement("a"); a.href = url;
    a.download = `工单导出_${dayjs().format("YYYYMMDD_HHmmss")}.csv`;
    a.click(); URL.revokeObjectURL(url);
    message("导出成功", { type: "success" });
  } catch { message("导出失败", { type: "error" }); }
}

function toggleSelect(id: number) {
  const idx = selectedIds.value.indexOf(id);
  if (idx >= 0) selectedIds.value.splice(idx, 1); else selectedIds.value.push(id);
}
function isSelected(id: number) { return selectedIds.value.includes(id); }

onMounted(() => { loadAssignees(); loadStats(); applyQueryParams(); loadTickets(); });
watch(() => route.query, () => { applyQueryParams(); loadTickets(); });
</script>

<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <div>
        <h1 class="text-xl font-bold">工单列表</h1>
        <span class="text-sm text-gray-400">共 {{ totalTickets }} 条</span>
      </div>
      <div class="flex gap-2">
        <el-button @click="handleExport" :icon="useRenderIcon('ep:download')">导出</el-button>
        <el-button type="primary" @click="goTo('/tickets/create')" :icon="useRenderIcon('ep:plus')">创建工单</el-button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-4 gap-4 mb-4">
      <div v-for="card in statCards" :key="card.filter"
        class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-all"
        :class="activeQuickFilter === card.filter ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-gray-300'"
        @click="applyQuickFilter(card.filter)">
        <div class="w-10 h-10 rounded-lg flex items-center justify-center" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="20"><component :is="useRenderIcon(card.icon)" /></el-icon>
        </div>
        <div>
          <div class="text-2xl font-bold" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="text-xs text-gray-500">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <el-input v-model="filters.keyword" placeholder="搜索工单标题、编号" clearable style="width: 260px"
          @clear="clearFilters" @keyup.enter="loadTickets"
          :prefix-icon="useRenderIcon('ep:search')" />
        <el-button @click="showFilters = !showFilters" :icon="useRenderIcon('ep:filter')">
          {{ showFilters ? '隐藏筛选' : '筛选' }}
        </el-button>
      </div>
      <div class="flex items-center gap-2" v-if="selectedIds.length">
        <span class="text-sm text-blue-600">已选 {{ selectedIds.length }} 条</span>
        <el-button size="small" @click="openBatchDialog('status')">批量状态</el-button>
        <el-button size="small" @click="openBatchDialog('assign')">批量指派</el-button>
        <el-button size="small" @click="selectedIds = []">取消选择</el-button>
      </div>
    </div>

    <!-- Advanced Filters -->
    <el-collapse-transition>
      <div v-if="showFilters" class="flex gap-3 mb-3 p-3 bg-gray-50 rounded-lg">
        <el-select v-model="filters.status" placeholder="状态" clearable style="width: 130px" @change="loadTickets">
          <el-option label="新建" :value="1" /><el-option label="处理中" :value="2" />
          <el-option label="已解决" :value="3" /><el-option label="已关闭" :value="4" />
        </el-select>
        <el-select v-model="filters.priority" placeholder="优先级" clearable style="width: 130px" @change="loadTickets">
          <el-option label="紧急" :value="4" /><el-option label="高" :value="3" />
          <el-option label="普通" :value="2" /><el-option label="低" :value="1" />
        </el-select>
        <el-select v-model="filters.type" placeholder="类型" clearable style="width: 130px" @change="loadTickets">
          <el-option label="故障" value="INCIDENT" /><el-option label="任务" value="TASK" />
          <el-option label="咨询" value="QUESTION" />
        </el-select>
        <el-button @click="clearFilters">重置</el-button>
      </div>
    </el-collapse-transition>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="displayedTickets" v-loading="loading" @row-click="(row: any) => goTo(`/tickets/detail/${row.id}`)" row-class-name="cursor-pointer">
        <el-table-column width="40">
          <template #header><el-checkbox :model-value="allSelected" @update:model-value="(v: boolean) => allSelected = v" :indeterminate="isIndeterminate" /></template>
          <template #default="{ row }"><el-checkbox :model-value="isSelected(row.id)" @click.stop @update:model-value="() => toggleSelect(row.id)" /></template>
        </el-table-column>
        <el-table-column prop="ticketNo" label="编号" width="160" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }"><el-tag :type="typeTagType(row.type)" size="small">{{ getTicketTypeLabel(row.type) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }"><el-tag :type="priorityTagType(row.priority)" size="small">{{ getTicketPriorityLabel(row.priority) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="statusTagType(row.status)" size="small">{{ getTicketStatusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" width="100">
          <template #default="{ row }"><span :class="!row.assigneeName ? 'text-red-400' : ''">{{ row.assigneeName || '未分配' }}</span></template>
        </el-table-column>
        <el-table-column label="创建时间" width="130">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalTickets"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- Batch Dialog -->
    <el-dialog v-model="batchDialogVisible" :title="batchAction === 'status' ? '批量修改状态' : '批量指派'" width="400px">
      <el-form v-if="batchAction === 'status'" label-width="80px">
        <el-form-item label="目标状态">
          <el-select v-model="batchStatus" style="width: 100%">
            <el-option label="新建" :value="1" /><el-option label="处理中" :value="2" />
            <el-option label="已解决" :value="3" /><el-option label="已关闭" :value="4" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-form v-else label-width="80px">
        <el-form-item label="指派给">
          <el-select v-model="batchAssignee" placeholder="选择处理人" style="width: 100%">
            <el-option v-for="a in assignees" :key="a.id" :label="a.displayName" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" :disabled="batchAction === 'assign' && !batchAssignee" @click="handleBatch">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";

:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
}

:deep(.el-table) {
  animation: fadeInUp 0.5s ease-out forwards;
  animation-delay: 0.1s;
  opacity: 0;
}

:deep(.el-table__row) {
  transition: all 0.2s ease;
}

:deep(.el-table__row:hover) {
  transform: scale(1.002);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
</style>
