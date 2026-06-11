<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "@/utils/message";
import { getWebhooks, createWebhook, updateWebhook, deleteWebhook, testWebhook, type WebhookConfig } from "@/api/webhook";

defineOptions({ name: "DocflowWebhooks" });
const loading = ref(false);
const webhooks = ref<WebhookConfig[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = ref({ name: "", url: "", secret: "", events: "TICKET_CREATED,TICKET_RESOLVED" });

const eventOptions = ["TICKET_CREATED", "TICKET_UPDATED", "TICKET_RESOLVED", "TICKET_CLOSED", "COMMENT_ADDED", "SLA_BREACH"];

async function loadWebhooks() {
  loading.value = true;
  try { const { code, data } = await getWebhooks(); if (code === 200) webhooks.value = data; } catch {}
  finally { loading.value = false; }
}

function openCreate() {
  editingId.value = null;
  form.value = { name: "", url: "", secret: "", events: "TICKET_CREATED,TICKET_RESOLVED" };
  dialogVisible.value = true;
}
function openEdit(w: WebhookConfig) {
  editingId.value = w.id;
  form.value = { name: w.name, url: w.url, secret: w.secret || "", events: w.events };
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.value.name.trim() || !form.value.url.trim()) { message("请填写名称和 URL", { type: "warning" }); return; }
  try {
    if (editingId.value) { await updateWebhook(editingId.value, form.value); }
    else { await createWebhook(form.value); }
    dialogVisible.value = false;
    message(editingId.value ? "更新成功" : "创建成功", { type: "success" });
    loadWebhooks();
  } catch (e) { message("操作失败", { type: "error" }); }
}

async function handleDelete(w: WebhookConfig) {
  try { await deleteWebhook(w.id); message("已删除", { type: "success" }); loadWebhooks(); }
  catch { message("删除失败", { type: "error" }); }
}

async function handleTest(w: WebhookConfig) {
  try { await testWebhook(w.id); message("测试发送成功", { type: "success" }); }
  catch { message("测试失败", { type: "error" }); }
}

onMounted(loadWebhooks);
</script>
<template>
  <div class="p-4">
    <div class="mg-top"><h1 class="mg-title">Webhooks</h1><el-button type="primary" @click="openCreate">+ 新建 Webhook</el-button></div>
    <div class="mg-card" v-loading="loading">
      <div v-for="w in webhooks" :key="w.id" class="mg-item">
        <div class="mg-item-header">
          <span class="mg-item-name">{{ w.name }}</span>
          <el-tag size="small" :type="w.enabled ? 'success' : 'info'">{{ w.enabled ? '启用' : '禁用' }}</el-tag>
        </div>
        <p class="mg-item-url">{{ w.url }}</p>
        <div class="mg-item-meta">
          <el-tag type="info" v-for="ev in w.events.split(',')" :key="ev" size="small" effect="plain" class="mr-1">{{ ev }}</el-tag>
        </div>
        <div class="mg-item-actions">
          <el-button size="small" text @click="handleTest(w)">测试</el-button>
          <el-button size="small" text @click="openEdit(w)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(w)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && !webhooks.length" description="暂无 Webhook" />
    </div>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑 Webhook' : '新建 Webhook'" width="540px">
      <div class="mg-form">
        <div class="mg-field"><label>名称 *</label><el-input v-model="form.name" /></div>
        <div class="mg-field"><label>URL *</label><el-input v-model="form.url" placeholder="https://..." /></div>
        <div class="mg-field"><label>Secret</label><el-input v-model="form.secret" placeholder="可选" /></div>
        <div class="mg-field"><label>事件</label><el-input v-model="form.events" placeholder="逗号分隔" /></div>
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
.mg-item-url { font-size: 12px; color: #667eea; font-family: monospace; margin: 0 0 8px; word-break: break-all; }
.mg-item-meta { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.mg-item-actions { display: flex; gap: 4px; }
.mg-form { display: flex; flex-direction: column; gap: 14px; }
.mg-field { display: flex; flex-direction: column; gap: 4px; }
.mg-field label { font-size: 13px; font-weight: 600; color: #374151; }
.dark .mg-title { color: #f1f5f9; }
.dark .mg-card { background: #1e293b; border-color: #334155; }
.dark .mg-item { border-bottom-color: #334155; }
.dark .mg-item-name { color: #f1f5f9; }

/* System page animations */
:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
}

:deep(.el-table) {
  animation: fadeInUp 0.5s ease-out forwards;
  animation-delay: 0.1s;
  opacity: 0;
}

/* Health status indicator */
.status-healthy {
  animation: pulse 2s ease-in-out infinite;
  color: #10b981;
}

/* Feature toggle animation */
:deep(.el-switch) {
  transition: all 0.3s ease;
}

:deep(.el-switch:hover) {
  transform: scale(1.05);
}

</style>
