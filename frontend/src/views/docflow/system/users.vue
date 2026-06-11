<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { http } from "@/utils/http";
import dayjs from "dayjs";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";

defineOptions({ name: "DocflowSystemUsers" });

const loading = ref(false);
const users = ref<any[]>([]);
const searchKeyword = ref("");

const filteredUsers = computed(() => {
  if (!searchKeyword.value.trim()) return users.value;
  const kw = searchKeyword.value.trim().toLowerCase();
  return users.value.filter(u =>
    (u.realName || u.nickname || u.username || "").toLowerCase().includes(kw) ||
    (u.username || "").toLowerCase().includes(kw) ||
    (u.email || "").toLowerCase().includes(kw)
  );
});

const stats = computed(() => ({
  total: users.value.length,
  active: users.value.filter(u => u.status === 1).length,
  disabled: users.value.filter(u => u.status !== 1).length
}));

async function loadUsers() {
  loading.value = true;
  try {
    const { code, data } = await http.request("get", "/api/system/users");
    if (code === 200) {
      users.value = Array.isArray(data) ? data : (data?.records || data?.content || []);
    }
  } catch { /* ignore */ }
  finally { loading.value = false; }
}

function statusLabel(s: number) { return s === 1 ? "正常" : "禁用"; }
function statusType(s: number): "success" | "danger" { return s === 1 ? "success" : "danger"; }
function formatTime(t: string | null) { return t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "-"; }

function roleBadge(r: string | null) {
  if (!r) return { label: "未分配", color: "#94a3b8", bg: "#f1f5f9" };
  const map: Record<string, { label: string; color: string; bg: string }> = {
    ADMIN: { label: "管理员", color: "#7c3aed", bg: "#faf5ff" },
    SUPPORT: { label: "技术", color: "#2563eb", bg: "#eff6ff" },
    USER: { label: "用户", color: "#10b981", bg: "#ecfdf5" }
  };
  return map[r] || { label: r, color: "#6b7280", bg: "#f9fafb" };
}

onMounted(loadUsers);
</script>

<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold">用户管理</h2>
      <el-input v-model="searchKeyword" placeholder="搜索用户..." clearable style="width: 260px"
        :prefix-icon="useRenderIcon('ep:search')" />
    </div>

    <div class="grid grid-cols-3 gap-4 mb-4">
      <el-card shadow="never" class="stat-card">
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-label">总用户</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-value text-green-600">{{ stats.active }}</div>
        <div class="stat-label">正常</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-value text-red-500">{{ stats.disabled }}</div>
        <div class="stat-label">禁用</div>
      </el-card>
    </div>

    <el-table :data="filteredUsers" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="用户信息" min-width="180">
        <template #default="{ row }">
          <div class="flex items-center gap-3">
            <el-avatar :size="36">{{ (row.realName || row.username || "?")[0] }}</el-avatar>
            <div>
              <div class="font-medium">{{ row.realName || row.nickname || row.username }}</div>
              <div class="text-xs text-gray-400">{{ row.username }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <span v-for="r in (row.roles || [row.role])" :key="r"
            class="inline-block px-2 py-0.5 rounded text-xs font-medium mr-1"
            :style="{ color: roleBadge(r).color, background: roleBadge(r).bg }">
            {{ roleBadge(r).label }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--el-text-color-secondary); margin-top: 4px; }

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
