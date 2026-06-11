<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { customerSubmitTicket } from "@/api/customer";

defineOptions({ name: "CustomerNewTicket" });

const router = useRouter();
const loading = ref(false);
const form = ref({
  title: "",
  content: "",
  type: "INCIDENT",
  priority: 2
});

const typeOptions = [
  { label: "故障", value: "INCIDENT" },
  { label: "任务", value: "TASK" },
  { label: "咨询", value: "QUESTION" }
];

const priorityOptions = [
  { label: "P4 - 紧急", value: 4 },
  { label: "P3 - 高", value: 3 },
  { label: "P2 - 普通", value: 2 },
  { label: "P1 - 低", value: 1 }
];

async function handleSubmit() {
  if (!form.value.title.trim()) { message("请填写标题", { type: "warning" }); return; }
  if (!form.value.content.trim()) { message("请填写问题描述", { type: "warning" }); return; }
  loading.value = true;
  try {
    const { code, data, message: msg } = await customerSubmitTicket(form.value);
    if (code !== 200) throw new Error(msg || "提交失败");
    message("工单提交成功", { type: "success" });
    router.push(`/portal/tickets/${data.id}`);
  } catch (error) {
    message(error instanceof Error ? error.message : "提交失败", { type: "error" });
  } finally { loading.value = false; }
}
</script>

<template>
  <div class="portal-page">
    <header class="portal-header">
      <div class="portal-header-left">
        <button class="portal-back" @click="router.push('/portal/tickets')">\u2190</button>
        <h1 class="portal-brand">DocFlow AI</h1>
        <span class="portal-divider">|</span>
        <span class="portal-section">提交工单</span>
      </div>
    </header>

    <div class="portal-content">
      <div class="portal-form-card">
        <h2 class="portal-form-title">提交新工单</h2>
        <p class="portal-form-desc">请描述您遇到的问题，我们会尽快处理。</p>

        <div class="portal-field">
          <label>标题 *</label>
          <input v-model="form.title" placeholder="简要描述您的问题" />
        </div>

        <div class="portal-field">
          <label>问题描述 *</label>
          <textarea v-model="form.content" rows="6" placeholder="请详细描述问题，包括发生时间、影响范围等" />
        </div>

        <div class="portal-field-row">
          <div class="portal-field flex-1">
            <label>类型</label>
            <select v-model="form.type">
              <option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </div>
          <div class="portal-field flex-1">
            <label>优先级</label>
            <select v-model="form.priority">
              <option v-for="o in priorityOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
            </select>
          </div>
        </div>

        <button class="portal-primary-btn" :disabled="loading" @click="handleSubmit">
          {{ loading ? '提交中...' : '提交工单' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
.portal-page { min-height: 100vh; background: #f8fafc; }
.portal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 28px; background: #fff; border-bottom: 1px solid #e8ecf1;
  position: sticky; top: 0; z-index: 10;
}
.portal-header-left { display: flex; align-items: center; gap: 10px; }
.portal-back {
  width: 32px; height: 32px; border: 1px solid #e2e8f0; border-radius: 6px;
  background: #fff; font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.portal-brand { font-size: 18px; font-weight: 800; color: #0d253d; margin: 0; }
.portal-divider { color: #e2e8f0; }
.portal-section { font-size: 14px; color: #64748d; font-weight: 500; }

.portal-content { max-width: 640px; margin: 0 auto; padding: 24px 20px; }
.portal-form-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 12px;
  padding: 28px 24px;
}
.portal-form-title { font-size: 20px; font-weight: 800; color: #0d253d; margin: 0 0 4px; }
.portal-form-desc { font-size: 13px; color: #94a3b8; margin: 0 0 24px; }

.portal-field { margin-bottom: 18px; }
.portal-field label { display: block; font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 6px; }
.portal-field input, .portal-field textarea, .portal-field select {
  width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 8px;
  font-size: 14px; outline: none; transition: border-color 0.2s;
  box-sizing: border-box; font-family: inherit;
}
.portal-field input:focus, .portal-field textarea:focus, .portal-field select:focus { border-color: #667eea; }
.portal-field textarea { resize: vertical; }
.portal-field-row { display: flex; gap: 12px; }
.flex-1 { flex: 1; }

.portal-primary-btn {
  width: 100%; padding: 12px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 15px; font-weight: 700; cursor: pointer;
  transition: opacity 0.2s; margin-top: 8px;
}
.portal-primary-btn:hover { opacity: 0.9; }
.portal-primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* Portal animations */
:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
}

:deep(.el-table) {
  animation: fadeInUp 0.5s ease-out forwards;
  animation-delay: 0.1s;
  opacity: 0;
}

.ticket-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.ticket-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}

/* Status indicator animation */
.status-dot {
  animation: pulse 2s ease-in-out infinite;
}

</style>
