<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { http } from "@/utils/http";
import dayjs from "dayjs";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";

defineOptions({ name: "DocflowSystemRoles" });

const loading = ref(false);
const roles = ref<any[]>([]);

const stats = computed(() => ({
  total: roles.value.length,
  active: roles.value.filter(r => r.status === 1).length,
  disabled: roles.value.filter(r => r.status !== 1).length
}));

const roleColors: Record<string, { color: string; bg: string; icon: string }> = {
  ADMIN: { color: "#7c3aed", bg: "#faf5ff", icon: "ep:setting" },
  SUPPORT: { color: "#2563eb", bg: "#eff6ff", icon: "ep:headset" },
  USER: { color: "#10b981", bg: "#ecfdf5", icon: "ep:user" }
};

async function loadRoles() {
  loading.value = true;
  try {
    const { code, data } = await http.request("get", "/api/system/roles");
    if (code === 200) roles.value = Array.isArray(data) ? data : (data?.records || data?.content || []);
  } catch { /* ignore */ }
  finally { loading.value = false; }
}

function statusLabel(s: number) { return s === 1 ? "正常" : "禁用"; }
function statusType(s: number) { return s === 1 ? "success" : "danger"; }
function formatTime(t: string | null) { return t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "-"; }

function getRoleStyle(code: string) {
  return roleColors[code] || { color: "#64748d", bg: "#f8fafc", icon: "ep:user" };
}

onMounted(() => loadRoles());
</script>

<template>
  <div class="sys-page">
    <div class="sys-top">
      <div class="sys-top-left">
        <h2 class="sys-hero-title">角色管理</h2>
        <span class="sys-hero-count">{{ stats.total }} 个角色</span>
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
      <el-table :data="roles" style="width: 100%">
        <el-table-column prop="id" label="ID" width="56" />
        <el-table-column label="角色信息" min-width="200">
          <template #default="{ row }">
            <div class="sys-role-info">
              <div class="sys-role-icon" :style="{ background: getRoleStyle(row.roleCode).bg, color: getRoleStyle(row.roleCode).color }">
                <component :is="useRenderIcon(getRoleStyle(row.roleCode).icon, { width: '16px', height: '16px' })" />
              </div>
              <div>
                <div class="sys-role-name">{{ row.roleName }}</div>
                <div class="sys-role-meta">{{ row.roleCode }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240">
          <template #default="{ row }">
            <span class="sys-desc">{{ row.description || "暂无描述" }}</span>
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
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">
            <span class="text-xs text-gray-400">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && roles.length === 0" description="暂无角色数据" />
    </div>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
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

.sys-role-info { display: flex; align-items: center; gap: 10px; }
.sys-role-icon { width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sys-role-name { font-size: 13px; font-weight: 600; color: #0d253d; }
.sys-role-meta { font-size: 11px; color: #94a3b8; margin-top: 1px; font-family: monospace; }
.sys-desc { font-size: 13px; color: #64748d; }

.dark .sys-hero-title { color: #f1f5f9; }
.dark .sys-table-card { background: #1e293b; border-color: #334155; }
.dark .sys-table-card :deep(.el-table__row:hover > td) { background-color: rgba(255,255,255,0.02) !important; }
.dark .sys-role-name { color: #f1f5f9; }
.dark .sys-desc { color: #94a3b8; }

/* Page animations */
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
}

</style>
