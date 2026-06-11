<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "@/utils/message";
import { getTemplates, createTemplate, updateTemplate, deleteTemplate, type TicketTemplate } from "@/api/template";

defineOptions({ name: "DocflowTemplates" });
const loading = ref(false);
const templates = ref<TicketTemplate[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = ref({ name: "", title: "", content: "", type: "INCIDENT", priority: 2 });

async function loadTemplates() {
  loading.value = true;
  try { const { code, data } = await getTemplates(); if (code === 200) templates.value = data; } catch {}
  finally { loading.value = false; }
}

function openCreate() {
  editingId.value = null;
  form.value = { name: "", title: "", content: "", type: "INCIDENT", priority: 2 };
  dialogVisible.value = true;
}
function openEdit(t: TicketTemplate) {
  editingId.value = t.id;
  form.value = { name: t.name, title: t.title, content: t.content, type: t.type || "INCIDENT", priority: t.priority || 2 };
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.value.name.trim() || !form.value.title.trim()) { message("请填写名称和标题", { type: "warning" }); return; }
  try {
    if (editingId.value) { await updateTemplate(editingId.value, form.value); }
    else { await createTemplate(form.value); }
    dialogVisible.value = false;
    message(editingId.value ? "更新成功" : "创建成功", { type: "success" });
    loadTemplates();
  } catch (e) { message("操作失败", { type: "error" }); }
}

async function handleDelete(t: TicketTemplate) {
  try { await deleteTemplate(t.id); message("已删除", { type: "success" }); loadTemplates(); }
  catch { message("删除失败", { type: "error" }); }
}

onMounted(loadTemplates);
</script>
<template>
  <div class="p-4">
    <div class="mg-top"><h1 class="mg-title">工单模板</h1><el-button type="primary" @click="openCreate">+ 新建模板</el-button></div>
    <div class="mg-card" v-loading="loading">
      <div v-for="t in templates" :key="t.id" class="mg-item">
        <div class="mg-item-header">
          <span class="mg-item-name">{{ t.name }}</span>
          <div class="mg-item-actions">
            <el-button size="small" text @click="openEdit(t)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(t)">删除</el-button>
          </div>
        </div>
        <p class="mg-item-title">标题: {{ t.title }}</p>
        <p class="mg-item-preview">{{ t.content?.slice(0, 100) }}{{ (t.content?.length || 0) > 100 ? '...' : '' }}</p>
      </div>
      <el-empty v-if="!loading && !templates.length" description="暂无模板" />
    </div>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新建模板'" width="580px">
      <div class="mg-form">
        <div class="mg-field"><label>模板名称 *</label><el-input v-model="form.name" /></div>
        <div class="mg-field"><label>工单标题 *</label><el-input v-model="form.title" /></div>
        <div class="mg-field"><label>工单内容 *</label><el-input v-model="form.content" type="textarea" :rows="6" /></div>
        <div class="flex gap-3">
          <div class="mg-field flex-1"><label>类型</label>
            <el-select v-model="form.type" class="w-full">
              <el-option label="故障" value="INCIDENT" /><el-option label="任务" value="TASK" /><el-option label="咨询" value="QUESTION" />
            </el-select>
          </div>
          <div class="mg-field flex-1"><label>优先级</label>
            <el-select v-model="form.priority" class="w-full">
              <el-option label="P4 紧急" :value="4" /><el-option label="P3 高" :value="3" /><el-option label="P2 普通" :value="2" /><el-option label="P1 低" :value="1" />
            </el-select>
          </div>
        </div>
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
.mg-item-title { font-size: 13px; color: #64748d; margin: 0 0 2px; }
.mg-item-preview { font-size: 12px; color: #94a3b8; margin: 0; }
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
