<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { http } from "@/utils/http";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";

defineOptions({ name: "DocflowSystemDepts" });

const loading = ref(false);
const depts = ref<any[]>([]);

const stats = computed(() => ({
  total: depts.value.length,
  active: depts.value.filter(d => d.status === 1).length,
  disabled: depts.value.filter(d => d.status !== 1).length
}));

async function loadDepts() {
  loading.value = true;
  try {
    const { code, data } = await http.request("get", "/api/system/depts");
    if (code === 200) depts.value = Array.isArray(data) ? data : (data?.records || data?.content || []);
  } catch { /* ignore */ }
  finally { loading.value = false; }
}

function statusLabel(s: number) { return s === 1 ? "正常" : "禁用"; }
function statusType(s: number) { return s === 1 ? "success" : "danger"; }

onMounted(() => loadDepts());
</script>

<template>
  <div class="sys-page">
    <div class="sys-top">
      <div class="sys-top-left">
        <h2 class="sys-hero-title">部门管理</h2>
        <span class="sys-hero-count">{{ stats.total }} 个部门</span>
      </div>
      <div class="sys-top-stats">
        <span class="sys-chip sys-chip-active">
          <span class="sys-chip-dot sys-chip-dot-active"></span>
          正常 {{ stats.active }}
        </span>
        <span class="sys-chip sys-chip-disabled">
          <span class="sys-chip-dot sys-chip-dot-disabled"></span>
          禁用 {{ stats.disabled }}
        </span>
      </div>
    </div>

    <div class="sys-table-card" v-loading="loading">
      <el-table :data="depts" style="width: 100%" row-key="id" default-expand-all>
        <el-table-column prop="id" label="ID" width="56" />
        <el-table-column label="部门名称" min-width="200">
          <template #default="{ row }">
            <div class="sys-dept-info">
              <div class="sys-dept-icon">
                <component :is="useRenderIcon('ep:office-building', { width: '16px', height: '16px' })" />
              </div>
              <span class="sys-dept-name">{{ row.deptName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center">
          <template #default="{ row }">
            <span class="sys-sort-badge">{{ row.sortOrder ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="sys-status-badge" :class="row.status === 1 ? 'sys-status-active' : 'sys-status-disabled'">
              <span class="sys-status-dot"></span>
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && depts.length === 0" description="暂无部门数据" />
    </div>
  </div>
</template>

<style scoped>
.sys-page { display: flex; flex-direction: column; gap: 10px; }
.sys-top { display: flex; align-items: center; justify-content: space-between; }
.sys-top-left { display: flex; align-items: baseline; gap: 8px; }
.sys-hero-title { font-size: 22px; font-weight: 800; letter-spacing: -0.3px; color: #0d253d; margin: 0; line-height: 1; }
.sys-hero-count { font-size: 13px; color: #94a3b8; }
.sys-top-stats { display: flex; gap: 6px; }
.sys-chip { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; font-weight: 500; padding: 3px 10px; border-radius: 6px; }
.sys-chip-active { color: #16a34a; background: #ecfdf5; }
.sys-chip-disabled { color: #dc2626; background: #fef2f2; }
.sys-chip-dot { width: 6px; height: 6px; border-radius: 50%; }
.sys-chip-dot-active { background: #16a34a; }
.sys-chip-dot-disabled { background: #dc2626; }

.sys-status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 20px;
  font-size: 12px; font-weight: 600;
}
.sys-status-active { background: #ecfdf5; color: #059669; }
.sys-status-active .sys-status-dot { background: #10b981; }
.sys-status-disabled { background: #fef2f2; color: #dc2626; }
.sys-status-disabled .sys-status-dot { background: #ef4444; }
.sys-status-dot { width: 6px; height: 6px; border-radius: 50%; }
.dark .sys-status-active { background: rgba(16, 185, 129, 0.15); color: #34d399; }
.dark .sys-status-disabled { background: rgba(239, 68, 68, 0.15); color: #f87171; }

.sys-table-card { background: #fff; border: 1px solid #e8ecf1; border-radius: 10px; overflow: hidden; }
.sys-table-card :deep(.el-table) { --el-table-border-color: #f1f5f9; --el-table-header-bg-color: #f8fafc; --el-table-header-text-color: #64748d; font-size: 13px; }
.sys-table-card :deep(.el-table th.el-table__cell) { font-weight: 600; font-size: 12px; }
.sys-table-card :deep(.el-table__row:hover > td) { background-color: #f8fafc !important; }

.sys-dept-info { display: flex; align-items: center; gap: 10px; }
.sys-dept-icon { width: 28px; height: 28px; border-radius: 6px; background: #eff6ff; color: #2563eb; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sys-dept-name { font-size: 13px; font-weight: 600; color: #0d253d; }
.sys-sort-badge { font-size: 12px; font-weight: 600; color: #64748d; background: #f1f5f9; padding: 2px 8px; border-radius: 4px; }

.dark .sys-hero-title { color: #f1f5f9; }
.dark .sys-table-card { background: #1e293b; border-color: #334155; }
.dark .sys-table-card :deep(.el-table__row:hover > td) { background-color: rgba(255,255,255,0.02) !important; }
.dark .sys-dept-name { color: #f1f5f9; }
.dark .sys-dept-icon { background: #1e3a5f; color: #60a5fa; }
.dark .sys-sort-badge { background: #334155; color: #94a3b8; }
</style>
