<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import dayjs from "dayjs";
import { useRoute, useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { getTickets, getTicketStats, getTicketAssignees, updateTicketStatus, assignTicket, type TicketListItem, type TicketAssigneeOption, type TicketStats } from "@/api/tickets";
import {
  getTicketPriorityLabel,
  getTicketStatusLabel,
  ticketPriorityOptions,
  ticketStatusOptions,
  ticketTypeOptions
} from "@/constants/tickets";

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
      { label: "全部", value: s?.total ?? totalTickets.value, color: "#6366f1", filter: "all" },
      { label: "处理中", value: s?.inProgress ?? 0, color: "#d97706", filter: "in-progress" },
      { label: "待分配", value: s?.unassigned ?? 0, color: "#ef4444", filter: "unassigned" },
      { label: "已解决", value: s?.resolved ?? 0, color: "#16a34a", filter: "resolved" }
    ];
  });

const displayedTickets = computed(() => tickets.value);

const allSelected = computed({
  get: () => displayedTickets.value.length > 0 && selectedIds.value.length === displayedTickets.value.length,
  set: (val: boolean) => {
    selectedIds.value = val ? displayedTickets.value.map(t => t.id) : [];
  }
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
  if (level === 2) return "";
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
    if ([1, 2, 3, 4].includes(p)) {
      filters.value.priority = p;
      showFilters.value = true;
    }
  }
}

async function loadTickets() {
  loading.value = true;
  try {
    const apiStatus = activeQuickFilter.value === "in-progress" ? 2
      : activeQuickFilter.value === "resolved" ? 3
      : filters.value.status;
    const apiAssignee = activeQuickFilter.value === "unassigned" ? -1
      : undefined;
    const { code, message: errorMessage, data } = await getTickets({
      keyword: filters.value.keyword.trim() || undefined,
      status: apiStatus,
      priority: filters.value.priority,
      type: filters.value.type,
      assigneeUserId: apiAssignee,
      page: currentPage.value,
      size: pageSize.value
    });
    if (code !== 200) throw new Error(errorMessage || "加载失败");
    tickets.value = data.records;
    totalTickets.value = data.total;
    selectedIds.value = [];
  } catch (error) {
    tickets.value = [];
    message(error instanceof Error ? error.message : "加载失败", { type: "error" });
  } finally { loading.value = false; }
}

async function loadAssignees() {
  try {
    const { code, data } = await getTicketAssignees();
    if (code === 200) assignees.value = data;
  } catch { /* silently fail */ }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  loadTickets();
}

function handleSizeChange(size: number) {
  pageSize.value = size;
  currentPage.value = 1;
  loadTickets();
}

function handleSearch() {
  currentPage.value = 1;
  activeQuickFilter.value = "all";
  activeQuickFilter.value = "all";
  loadTickets();
}

function resetFilters() {
  currentPage.value = 1;
  filters.value = { keyword: "", status: undefined, priority: undefined, type: undefined };
  activeQuickFilter.value = "all";
  loadTickets();
}

function openBatchDialog(action: "status" | "assign") {
  batchAction.value = action;
  batchStatus.value = 2;
  batchAssignee.value = null;
  batchDialogVisible.value = true;
}

async function handleBatch() {
  if (!selectedIds.value.length) return;
  batchLoading.value = true;
  let success = 0;
  let fail = 0;
  try {
    for (const id of selectedIds.value) {
      try {
        if (batchAction.value === "status") {
          await updateTicketStatus(id, { status: batchStatus.value, remark: "批量操作" });
        } else if (batchAction.value === "assign" && batchAssignee.value) {
          await assignTicket(id, { assigneeUserId: batchAssignee.value });
        }
        success++;
      } catch { fail++; }
    }
    batchDialogVisible.value = false;
    if (fail === 0) {
      message(`批量操作成功：${success} 张工单已更新`, { type: "success" });
    } else {
      message(`操作完成：${success} 成功，${fail} 失败`, { type: "warning" });
    }
    loadTickets();
  } finally { batchLoading.value = false; }
}

function exportCSV() {
  const list = displayedTickets.value;
  if (!list.length) {
    message("没有可导出的数据", { type: "warning" });
    return;
  }
  const BOM = "\uFEFF";
  const headers = ["工单编号", "标题", "内容", "优先级", "状态", "提交人", "处理人", "创建时间", "更新时间"];
  const rows = list.map(t => [
    t.ticketNo,
    t.title,
    (t.content || "").replace(/"/g, '""'),
    getTicketPriorityLabel(t.priority),
    getTicketStatusLabel(t.status),
    t.submitterName || "",
    t.assigneeName || "待分配",
    t.createTime ? dayjs(t.createTime).format("YYYY-MM-DD HH:mm") : "",
    t.updateTime ? dayjs(t.updateTime).format("YYYY-MM-DD HH:mm") : ""
  ]);

  const csv = BOM + [headers, ...rows]
    .map(row => row.map(cell => `"${cell}"`).join(","))
    .join("\n");

  const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `工单导出_${dayjs().format("YYYYMMDD_HHmm")}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  message(`已导出 ${list.length} 条工单`, { type: "success" });
}

watch(() => route.query, () => { applyQueryParams(); loadTickets(); });

watch(activeQuickFilter, () => {
  currentPage.value = 1;
  loadTickets();
});

async function loadStats() {
  try {
    const { code, data } = await getTicketStats();
    if (code === 200) ticketStats.value = data;
  } catch { /* ignore */ }
}

onMounted(() => { applyQueryParams(); loadTickets(); loadAssignees(); loadStats(); });
</script>

<template>
  <div>
    <div class="mb-5 flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">工单中心</h2>
        <p class="mt-0.5 text-sm text-gray-400">{{ totalTickets }} 张工单</p>
      </div>
      <div class="flex items-center gap-2">
        <el-button @click="showFilters = !showFilters">
          <component :is="useRenderIcon(showFilters ? 'ep:arrow-up' : 'ep:arrow-down', { width: '14px', height: '14px' })" class="mr-1" />
          筛选
        </el-button>
        <el-button @click="exportCSV">
          <component :is="useRenderIcon('ep:download', { width: '14px', height: '14px' })" class="mr-1" />
          导出
        </el-button>
        <el-button type="primary" @click="goTo('/tickets/create')">
          <component :is="useRenderIcon('ep:plus', { width: '16px', height: '16px' })" class="mr-1" />
          新建工单
        </el-button>
      </div>
    </div>

    <!-- Stat bar -->
    <div class="grid grid-cols-4 gap-3 mb-4">
      <div v-for="s in statCards" :key="s.label"
        class="bg-white dark:bg-gray-800 rounded-lg border border-gray-100 dark:border-gray-700 p-3 text-center cursor-pointer hover:border-gray-300 dark:hover:border-gray-600 transition-colors"
        @click="activeQuickFilter = s.filter">
        <p class="text-2xl font-bold" :style="{ color: s.color }">{{ s.value }}</p>
        <p class="text-xs text-gray-400 mt-0.5">{{ s.label }}</p>
      </div>
    </div>

    <!-- Filters -->
    <div v-if="showFilters" class="bg-white dark:bg-gray-800 rounded-lg border border-gray-100 dark:border-gray-700 p-4 mb-4">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-3">
        <el-input v-model="filters.keyword" placeholder="搜索标题或内容..." clearable size="default" />
        <el-select v-model="filters.status" placeholder="状态" clearable size="default">
          <el-option v-for="o in ticketStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="filters.priority" placeholder="优先级" clearable size="default">
          <el-option v-for="o in ticketPriorityOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-select v-model="filters.type" placeholder="类型" clearable size="default">
          <el-option v-for="o in ticketTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </div>
      <div class="flex items-center gap-2">
        <el-radio-group v-model="activeQuickFilter" size="small">
          <el-radio-button v-for="f in quickFilters" :key="f.value" :value="f.value">{{ f.label }}</el-radio-button>
        </el-radio-group>
        <div class="flex-1" />
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <!-- Batch bar -->
    <div v-if="selectedIds.length" class="flex items-center gap-3 mb-3 px-4 py-2.5 bg-blue-50 dark:bg-blue-900/20 rounded-lg text-sm">
      <span class="text-blue-700 dark:text-blue-300 font-medium">已选 {{ selectedIds.length }} 项</span>
      <div class="flex-1" />
      <el-button size="small" @click="openBatchDialog('status')">
        <component :is="useRenderIcon('ep:set-up', { width: '14px', height: '14px' })" class="mr-1" />
        批量改状态
      </el-button>
      <el-button size="small" @click="openBatchDialog('assign')">
        <component :is="useRenderIcon('ep:user', { width: '14px', height: '14px' })" class="mr-1" />
        批量指派
      </el-button>
      <el-button size="small" text type="danger" @click="selectedIds = []">取消选择</el-button>
    </div>

    <!-- Table -->
    <el-card shadow="never" class="overflow-hidden">
      <el-table
        :data="displayedTickets"
        stripe
        v-loading="loading"
        class="ticket-table"
        @selection-change="(rows: TicketListItem[]) => selectedIds = rows.map(r => r.id)"
        @row-click="(row: TicketListItem) => goTo(`/tickets/${row.id}`)"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column label="工单信息" min-width="320">
          <template #default="{ row }">
            <div class="py-1">
              <div class="flex items-center gap-2">
                <span class="font-mono text-xs text-gray-400 shrink-0">{{ row.ticketNo }}</span>
                <span class="font-medium">{{ row.title }}</span>
              </div>
              <p class="mt-1 text-xs text-gray-400 truncate max-w-sm">{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" size="small" effect="light">P{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">
              {{ getTicketStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="处理人" width="95" align="center">
          <template #default="{ row }">
            <span v-if="row.assigneeName" class="text-sm">{{ row.assigneeName }}</span>
            <span v-else class="text-xs text-orange-400">待分配</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="105">
          <template #default="{ row }">
            <span class="text-xs text-gray-400">{{ formatDate(row.updateTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !displayedTickets.length" description="暂无工单">
        <el-button type="primary" @click="goTo('/tickets/create')">创建第一张工单</el-button>
      </el-empty>

      <div v-if="totalTickets > 0" class="flex justify-center pt-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalTickets"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- Batch dialog -->
    <el-dialog v-model="batchDialogVisible" :title="batchAction === 'status' ? '批量修改状态' : '批量指派'" width="420px">
      <template v-if="batchAction === 'status'">
        <el-select v-model="batchStatus" placeholder="选择目标状态" class="w-full">
          <el-option v-for="o in ticketStatusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </template>
      <template v-else>
        <el-select v-model="batchAssignee" placeholder="选择处理人" class="w-full" filterable>
          <el-option v-for="u in assignees" :key="u.id" :label="`${u.displayName} (@${u.username})`" :value="u.id" />
        </el-select>
      </template>
      <p class="text-xs text-gray-400 mt-3">将对选中的 {{ selectedIds.length }} 张工单执行此操作。</p>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="batchLoading"
          :disabled="batchAction === 'assign' && !batchAssignee"
          @click="handleBatch"
        >
          确认执行
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ticket-table :deep(.el-table__row:hover) { background-color: #f8fafc; }
.dark .ticket-table :deep(.el-table__row:hover) { background-color: rgba(255,255,255,0.02); }
.ticket-table :deep(.el-table__row) { cursor: pointer; }
</style>