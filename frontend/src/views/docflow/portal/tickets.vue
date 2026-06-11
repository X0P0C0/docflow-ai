<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { customerListTickets, getCustomerInfo, clearCustomerSession, type CustomerTicket } from "@/api/customer";

defineOptions({ name: "CustomerTickets" });

const router = useRouter();
const loading = ref(false);
const tickets = ref<CustomerTicket[]>([]);
const customer = getCustomerInfo();

async function loadTickets() {
  loading.value = true;
  try {
    const { code, data } = await customerListTickets();
    if (code === 200) tickets.value = data;
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

function statusColor(status?: number) {
  if (status === 3) return "#10b981";
  if (status === 4) return "#94a3b8";
  if (status === 2) return "#f59e0b";
  return "#3b82f6";
}

function statusLabel(status?: number) {
  const map: Record<number, string> = { 1: "新建", 2: "处理中", 3: "已解决", 4: "已关闭" };
  return map[status || 0] || "未知";
}

function priorityLabel(p?: number) {
  const map: Record<number, string> = { 4: "紧急", 3: "高", 2: "普通", 1: "低" };
  return map[p || 0] || "-";
}

function formatDate(d?: string) {
  return d ? new Date(d).toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" }) : "-";
}

function logout() {
  clearCustomerSession();
  router.push("/portal/login");
}

onMounted(loadTickets);
</script>

<template>
  <div class="portal-page">
    <header class="portal-header">
      <div class="portal-header-left">
        <h1 class="portal-brand">DocFlow AI</h1>
        <span class="portal-divider">|</span>
        <span class="portal-section">我的工单</span>
      </div>
      <div class="portal-header-right">
        <span class="portal-user">{{ customer?.realName || customer?.username || '客户' }}</span>
        <button class="portal-logout" @click="logout">退出</button>
      </div>
    </header>

    <div class="portal-content">
      <div class="portal-toolbar">
        <h2 class="portal-page-title">我的工单 <span class="portal-count">{{ tickets.length }}</span></h2>
        <button class="portal-primary-btn" @click="router.push('/portal/tickets/new')">+ 提交工单</button>
      </div>

      <div v-if="loading" class="portal-loading">加载中...</div>

      <div v-else-if="tickets.length" class="portal-ticket-list">
        <div v-for="t in tickets" :key="t.id" class="portal-ticket-card" @click="router.push(`/portal/tickets/${t.id}`)">
          <div class="portal-ticket-top">
            <span class="portal-ticket-no">{{ t.ticketNo }}</span>
            <span class="portal-ticket-status" :style="{ background: statusColor(t.status) + '18', color: statusColor(t.status) }">
              {{ statusLabel(t.status) }}
            </span>
          </div>
          <h3 class="portal-ticket-title">{{ t.title }}</h3>
          <p class="portal-ticket-preview">{{ t.content?.slice(0, 80) }}{{ (t.content?.length || 0) > 80 ? '...' : '' }}</p>
          <div class="portal-ticket-meta">
            <span>优先级: {{ priorityLabel(t.priority) }}</span>
            <span v-if="t.assigneeName">处理人: {{ t.assigneeName }}</span>
            <span>{{ formatDate(t.createTime) }}</span>
          </div>
        </div>
      </div>

      <div v-else class="portal-empty">
        <p>还没有工单</p>
        <button class="portal-primary-btn" @click="router.push('/portal/tickets/new')">提交第一个工单</button>
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
.portal-brand { font-size: 18px; font-weight: 800; color: #0d253d; margin: 0; }
.portal-divider { color: #e2e8f0; }
.portal-section { font-size: 14px; color: #64748d; font-weight: 500; }
.portal-header-right { display: flex; align-items: center; gap: 12px; }
.portal-user { font-size: 13px; color: #374151; font-weight: 500; }
.portal-logout {
  padding: 6px 14px; border: 1px solid #e2e8f0; border-radius: 6px;
  background: #fff; font-size: 12px; color: #64748d; cursor: pointer;
}
.portal-logout:hover { background: #f8fafc; border-color: #cbd5e1; }

.portal-content { max-width: 800px; margin: 0 auto; padding: 24px 20px; }
.portal-toolbar {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px;
}
.portal-page-title { font-size: 20px; font-weight: 800; color: #0d253d; margin: 0; }
.portal-count {
  font-size: 13px; color: #94a3b8; font-weight: 500; background: #f1f5f9;
  padding: 2px 8px; border-radius: 10px; margin-left: 6px;
}

.portal-primary-btn {
  padding: 10px 20px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 14px; font-weight: 600; cursor: pointer;
  transition: opacity 0.2s;
}
.portal-primary-btn:hover { opacity: 0.9; }

.portal-ticket-list { display: flex; flex-direction: column; gap: 10px; }
.portal-ticket-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 10px;
  padding: 16px 20px; cursor: pointer; transition: all 0.2s;
}
.portal-ticket-card:hover {
  border-color: #667eea; box-shadow: 0 4px 16px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}
.portal-ticket-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.portal-ticket-no { font-family: monospace; font-size: 12px; color: #667eea; font-weight: 600; }
.portal-ticket-status {
  padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600;
}
.portal-ticket-title { font-size: 15px; font-weight: 700; color: #0d253d; margin: 0 0 4px; }
.portal-ticket-preview { font-size: 13px; color: #64748d; margin: 0 0 8px; line-height: 1.5; }
.portal-ticket-meta { display: flex; gap: 16px; font-size: 12px; color: #94a3b8; }

.portal-empty {
  text-align: center; padding: 60px 20px; color: #94a3b8;
}
.portal-empty p { font-size: 15px; margin-bottom: 16px; }
.portal-loading { text-align: center; padding: 40px; color: #94a3b8; font-size: 14px; }

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
