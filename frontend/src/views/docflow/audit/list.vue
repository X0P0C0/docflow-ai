<script setup lang="ts">
import { onMounted, ref } from "vue";
import dayjs from "dayjs";
import { getAuditLogs, type AuditLogEntry } from "@/api/audit";

defineOptions({ name: "DocflowAuditLogs" });

const loading = ref(false);
const logs = ref<AuditLogEntry[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const selectedLog = ref<AuditLogEntry | null>(null);

const filters = ref({
  module: "",
  username: "",
  success: undefined as boolean | undefined
});

async function loadLogs() {
  loading.value = true;
  try {
    const { code, data } = await getAuditLogs({
      page: currentPage.value,
      size: pageSize.value,
      module: filters.value.module || undefined,
      username: filters.value.username || undefined,
      success: filters.value.success
    });
    if (code === 200) {
      logs.value = Array.isArray(data) ? data : (data?.records || []);
      total.value = data.total;
    }
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

function formatTime(d?: string) {
  return d ? dayjs(d).format("MM-DD HH:mm:ss") : "-";
}

function methodColor(method?: string) {
  const map: Record<string, string> = { GET: "#10b981", POST: "#3b82f6", PUT: "#f59e0b", DELETE: "#ef4444" };
  return map[method || ""] || "#64748d";
}

function statusColor(code?: number) {
  if (!code) return "#64748d";
  if (code < 300) return "#10b981";
  if (code < 400) return "#3b82f6";
  if (code < 500) return "#f59e0b";
  return "#ef4444";
}

onMounted(loadLogs);
</script>

<template>
  <div class="p-4">
    <div class="al-top">
      <h1 class="al-title">审计日志</h1>
      <span class="al-count">共 {{ total }} 条</span>
    </div>

    <div class="al-filters">
      <el-input v-model="filters.username" placeholder="用户名" clearable style="width: 160px" @clear="loadLogs" @keyup.enter="loadLogs" />
      <el-input v-model="filters.module" placeholder="模块" clearable style="width: 130px" @clear="loadLogs" @keyup.enter="loadLogs" />
      <el-select v-model="filters.success" placeholder="结果" clearable style="width: 110px" @change="loadLogs">
        <el-option label="成功" :value="true" /><el-option label="失败" :value="false" />
      </el-select>
      <el-button @click="loadLogs">查询</el-button>
    </div>

    <div class="al-card" v-loading="loading">
      <el-table :data="logs" style="width: 100%" size="small" @row-click="(row: AuditLogEntry) => selectedLog = row" highlight-current-row>
        <el-table-column label="时间" width="130">
          <template #default="{ row }"><span class="text-xs">{{ formatTime(row.operateTime) }}</span></template>
        </el-table-column>
        <el-table-column label="用户" prop="username" width="100" />
        <el-table-column label="模块" prop="module" width="100">
          <template #default="{ row }"><el-tag type="info" size="small" effect="plain">{{ row.module }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" prop="action" width="140" />
        <el-table-column label="请求" min-width="200">
          <template #default="{ row }">
            <span class="al-method" :style="{ color: methodColor(row.method) }">{{ row.method }}</span>
            <span class="al-uri">{{ row.uri }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="al-status" :style="{ color: statusColor(row.statusCode) }">{{ row.statusCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">
            <span class="text-xs" :class="{ 'text-orange-500': row.durationMs > 1000 }">{{ row.durationMs }}ms</span>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="clientIp" width="120">
          <template #default="{ row }"><span class="text-xs text-gray-400">{{ row.clientIp }}</span></template>
        </el-table-column>
        <el-table-column label="结果" width="70">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small" effect="light">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > pageSize" class="al-pagination">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="total"
          layout="total, prev, pager, next" background @current-change="loadLogs" />
      </div>
    </div>

    <!-- Detail drawer -->
    <el-drawer :model-value="!!selectedLog" @update:model-value="val => { if (!val) selectedLog = null }" title="审计详情" size="480px" :with-header="true">
      <template v-if="selectedLog">
        <div class="al-detail">
          <div class="al-detail-row"><span>时间</span><span>{{ formatTime(selectedLog.operateTime) }}</span></div>
          <div class="al-detail-row"><span>用户</span><span>{{ selectedLog.username }} (ID: {{ selectedLog.userId }})</span></div>
          <div class="al-detail-row"><span>模块</span><span>{{ selectedLog.module }}</span></div>
          <div class="al-detail-row"><span>操作</span><span>{{ selectedLog.action }}</span></div>
          <div class="al-detail-row"><span>请求</span><span>{{ selectedLog.method }} {{ selectedLog.uri }}</span></div>
          <div class="al-detail-row"><span>状态</span><span :style="{ color: statusColor(selectedLog.statusCode) }">{{ selectedLog.statusCode }}</span></div>
          <div class="al-detail-row"><span>耗时</span><span>{{ selectedLog.durationMs }}ms</span></div>
          <div class="al-detail-row"><span>IP</span><span>{{ selectedLog.clientIp }}</span></div>
          <div class="al-detail-row"><span>TraceId</span><span class="al-trace">{{ selectedLog.traceId || '-' }}</span></div>
          <div v-if="selectedLog.errorMessage" class="al-detail-row">
            <span>错误</span><span class="text-red-500">{{ selectedLog.errorMessage }}</span>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.al-top { display: flex; align-items: baseline; gap: 8px; margin-bottom: 12px; }
.al-title { font-size: 22px; font-weight: 800; color: #0d253d; margin: 0; }
.al-count { font-size: 13px; color: #94a3b8; }
.al-filters { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.al-card { background: #fff; border: 1px solid #e8ecf1; border-radius: 10px; overflow: hidden; }
.al-method { font-family: monospace; font-size: 11px; font-weight: 700; margin-right: 6px; }
.al-uri { font-family: monospace; font-size: 12px; color: #475569; }
.al-status { font-family: monospace; font-size: 12px; font-weight: 600; }
.al-pagination { display: flex; justify-content: center; padding: 12px; border-top: 1px solid #f1f5f9; }
.al-detail { display: flex; flex-direction: column; gap: 12px; }
.al-detail-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.al-detail-row span:first-child { font-size: 13px; color: #94a3b8; white-space: nowrap; }
.al-detail-row span:last-child { font-size: 13px; color: #0d253d; text-align: right; word-break: break-all; }
.al-trace { font-family: monospace; font-size: 11px; color: #667eea; }
.dark .al-title { color: #f1f5f9; }
.dark .al-card { background: #1e293b; border-color: #334155; }
.dark .al-uri { color: #94a3b8; }
.dark .al-detail-row span:last-child { color: #f1f5f9; }
</style>
