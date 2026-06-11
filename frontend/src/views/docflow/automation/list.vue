<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "@/utils/message";
import { getAutomationRules, createAutomationRule, updateAutomationRule, deleteAutomationRule, toggleAutomationRule, type AutomationRule } from "@/api/automation";

defineOptions({ name: "DocflowAutomation" });
const loading = ref(false);
const rules = ref<AutomationRule[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = ref({ name: "", description: "", triggerType: "TICKET_CREATED", conditions: "{}", actions: "{}", priority: 0 });

const triggerTypes = [
  { label: "工单创建", value: "TICKET_CREATED" },
  { label: "状态变更", value: "STATUS_CHANGED" },
  { label: "优先级变更", value: "PRIORITY_CHANGED" },
  { label: "SLA危险", value: "SLA_BREACH" }
];

async function loadRules() {
  loading.value = true;
  try { const { code, data } = await getAutomationRules(); if (code === 200) rules.value = data; } catch {}
  finally { loading.value = false; }
}

function openCreate() {
  editingId.value = null;
  form.value = { name: "", description: "", triggerType: "TICKET_CREATED", conditions: "{}", actions: "{}", priority: 0 };
  dialogVisible.value = true;
}
function openEdit(r: AutomationRule) {
  editingId.value = r.id;
  form.value = { name: r.name, description: r.description || "", triggerType: r.triggerType, conditions: r.conditions, actions: r.actions, priority: r.priority };
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.value.name.trim()) { message("请填写名称", { type: "warning" }); return; }
  try {
    if (editingId.value) { await updateAutomationRule(editingId.value, form.value); }
    else { await createAutomationRule(form.value); }
    dialogVisible.value = false;
    message(editingId.value ? "更新成功" : "创建成功", { type: "success" });
    loadRules();
  } catch (e) { message("操作失败", { type: "error" }); }
}

async function handleDelete(r: AutomationRule) {
  try { await deleteAutomationRule(r.id); message("已删除", { type: "success" }); loadRules(); }
  catch { message("删除失败", { type: "error" }); }
}

async function handleToggle(r: AutomationRule) {
  try { await toggleAutomationRule(r.id, r.status === 1 ? 0 : 1); loadRules(); }
  catch { message("操作失败", { type: "error" }); }
}

onMounted(loadRules);
</script>
<template>
  <div class="p-4">
    <div class="mg-top"><h1 class="mg-title">自动化规则</h1><el-button type="primary" @click="openCreate">+ 新建规则</el-button></div>
    <div class="mg-card" v-loading="loading">
      <div v-for="r in rules" :key="r.id" class="mg-item">
        <div class="mg-item-header">
          <span class="mg-item-name">{{ r.name }}</span>
          <div class="flex items-center gap-2">
            <el-tag size="small" :type="r.status === 1 ? 'success' : 'info'">{{ r.status === 1 ? '启用' : '禁用' }}</el-tag>
            <el-switch :model-value="r.status === 1" @change="handleToggle(r)" size="small" />
          </div>
        </div>
        <p v-if="r.description" class="mg-item-desc">{{ r.description }}</p>
        <div class="mg-item-meta">
          <el-tag type="info" size="small" effect="plain">{{ triggerTypes.find(t => t.value === r.triggerType)?.label || r.triggerType }}</el-tag>
          <span class="text-xs text-gray-400">优先级: {{ r.priority }}</span>
        </div>
        <div class="mg-item-actions">
          <el-button size="small" text @click="openEdit(r)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(r)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && !rules.length" description="暂无规则" />
    </div>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑规则' : '新建规则'" width="540px">
      <div class="mg-form">
        <div class="mg-field"><label>名称 *</label><el-input v-model="form.name" /></div>
        <div class="mg-field"><label>描述</label><el-input v-model="form.description" type="textarea" :rows="2" /></div>
        <div class="mg-field"><label>触发条件</label>
          <el-select v-model="form.triggerType" class="w-full"><el-option v-for="t in triggerTypes" :key="t.value" :label="t.label" :value="t.value" /></el-select>
        </div>
        <div class="mg-field"><label>条件 (JSON)</label><el-input v-model="form.conditions" type="textarea" :rows="2" /></div>
        <div class="mg-field"><label>动作 (JSON)</label><el-input v-model="form.actions" type="textarea" :rows="2" /></div>
        <div class="mg-field"><label>优先级</label><el-input-number v-model="form.priority" :min="0" :max="100" /></div>
      </div>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">{{ editingId ? '保存' : '创建' }}</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
@import "@/styles/animations.css";
.mg-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.mg-title { font-size: 22px; font-weight: 800; color: #0d253d; margin: 0; }
.mg-card { background: #fff; border: 1px solid #e8ecf1; border-radius: 10px; overflow: hidden; }
.mg-item { padding: 16px 20px; border-bottom: 1px solid #f1f5f9; }
.mg-item:last-child { border-bottom: none; }
.mg-item-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.mg-item-name { font-size: 15px; font-weight: 700; color: #0d253d; }
.mg-item-desc { font-size: 13px; color: #64748d; margin: 0 0 8px; }
.mg-item-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.mg-item-actions { display: flex; gap: 4px; }
.mg-form { display: flex; flex-direction: column; gap: 14px; }
.mg-field { display: flex; flex-direction: column; gap: 4px; }
.mg-field label { font-size: 13px; font-weight: 600; color: #374151; }
.dark .mg-title { color: #f1f5f9; }
.dark .mg-card { background: #1e293b; border-color: #334155; }
.dark .mg-item { border-bottom-color: #334155; }
.dark .mg-item-name { color: #f1f5f9; }

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
