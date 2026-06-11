<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "@/utils/message";
import { getSlaPolicies, createSlaPolicy, updateSlaPolicy, deleteSlaPolicy, type SlaPolicy, type SlaPolicyRequest } from "@/api/sla";

defineOptions({ name: "DocflowSla" });

const loading = ref(false);
const policies = ref<SlaPolicy[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = ref<SlaPolicyRequest>({
  name: "", description: "", priority: 2, responseHours: 4, resolveHours: 48, businessHoursOnly: false
});

const priorityLabels: Record<number, string> = { 0: "低", 1: "普通", 2: "高", 3: "紧急", 4: "严重" };

async function loadPolicies() {
  loading.value = true;
  try {
    const { code, data } = await getSlaPolicies();
    if (code === 200) policies.value = data;
  } finally { loading.value = false; }
}

function openCreate() {
  editingId.value = null;
  form.value = { name: "", description: "", priority: 2, responseHours: 4, resolveHours: 48, businessHoursOnly: false };
  dialogVisible.value = true;
}

function openEdit(p: SlaPolicy) {
  editingId.value = p.id;
  form.value = { name: p.name, description: p.description, priority: p.priority, responseHours: p.responseHours, resolveHours: p.resolveHours, businessHoursOnly: p.businessHoursOnly };
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.value.name) return message("请输入策略名称", { type: "warning" });
  const { code } = editingId.value
    ? await updateSlaPolicy(editingId.value, form.value)
    : await createSlaPolicy(form.value);
  if (code === 200) {
    message(editingId.value ? "更新成功" : "创建成功", { type: "success" });
    dialogVisible.value = false;
    loadPolicies();
  }
}

async function handleDelete(p: SlaPolicy) {
  const { code } = await deleteSlaPolicy(p.id);
  if (code === 200) { message("删除成功", { type: "success" }); loadPolicies(); }
}

function formatHours(h: number) {
  if (!h) return "-";
  if (h < 24) return `${h} 小时`;
  const d = Math.floor(h / 24);
  return h % 24 === 0 ? `${d} 天` : `${d} 天 ${h % 24} 小时`;
}

onMounted(loadPolicies);
</script>

<template>
  <div class="p-4">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-lg font-semibold">SLA 策略管理</h2>
      <el-button type="primary" @click="openCreate">新建策略</el-button>
    </div>

    <div class="sla-card" v-loading="loading">
      <div v-for="p in policies" :key="p.id" class="sla-item">
        <div class="sla-item-header">
          <span class="sla-item-name">{{ p.name }}</span>
          <el-tag size="small" :type="p.status === 1 ? 'success' : 'info'">{{ p.status === 1 ? '启用' : '禁用' }}</el-tag>
        </div>
        <div class="sla-item-desc" v-if="p.description">{{ p.description }}</div>
        <div class="sla-item-metrics">
          <div class="sla-metric">
            <span class="sla-metric-label">优先级</span>
            <span class="sla-metric-value">{{ priorityLabels[p.priority] || p.priority }}</span>
          </div>
          <div class="sla-metric">
            <span class="sla-metric-label">响应时间</span>
            <span class="sla-metric-value">{{ formatHours(p.responseHours) }}</span>
          </div>
          <div class="sla-metric">
            <span class="sla-metric-label">解决时间</span>
            <span class="sla-metric-value">{{ formatHours(p.resolveHours) }}</span>
          </div>
          <div class="sla-metric">
            <span class="sla-metric-label">工作时间</span>
            <span class="sla-metric-value">{{ p.businessHoursOnly ? '仅工作日' : '7x24h' }}</span>
          </div>
        </div>
        <div class="sla-item-actions">
          <el-button size="small" text @click="openEdit(p)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(p)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && policies.length === 0" description="暂无 SLA 策略" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑策略' : '新建策略'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="策略名称"><el-input v-model="form.name" placeholder="如 P1 紧急" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" class="w-full">
            <el-option v-for="(label, val) in priorityLabels" :key="val" :label="label" :value="Number(val)" />
          </el-select>
        </el-form-item>
        <el-form-item label="响应时间(h)"><el-input-number v-model="form.responseHours" :min="1" :max="720" class="w-full" /></el-form-item>
        <el-form-item label="解决时间(h)"><el-input-number v-model="form.resolveHours" :min="1" :max="8760" class="w-full" /></el-form-item>
        <el-form-item label="仅工作时间"><el-switch v-model="form.businessHoursOnly" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">{{ editingId ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
.sla-card { display: flex; flex-direction: column; gap: 12px; }
.sla-item { background: var(--el-bg-color); border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 16px; }
.sla-item-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sla-item-name { font-weight: 600; font-size: 15px; }
.sla-item-desc { color: var(--el-text-color-secondary); font-size: 13px; margin-bottom: 12px; }
.sla-item-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin-bottom: 12px; }
.sla-metric { text-align: center; }
.sla-metric-label { display: block; font-size: 12px; color: var(--el-text-color-secondary); margin-bottom: 4px; }
.sla-metric-value { font-weight: 600; font-size: 14px; }
.sla-item-actions { display: flex; justify-content: flex-end; gap: 8px; }

/* SLA page animations */
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

/* Status tags */
:deep(.el-tag--success) {
  animation: pulse 2s ease-in-out infinite;
}
</style>
