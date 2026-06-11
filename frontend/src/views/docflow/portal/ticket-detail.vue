<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "@/utils/message";
import { customerGetTicket, customerRateSatisfaction, type CustomerTicket } from "@/api/customer";

defineOptions({ name: "CustomerTicketDetail" });

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const ticket = ref<CustomerTicket | null>(null);
const ticketId = computed(() => Number(route.params.id));

// Satisfaction
const showSatisfaction = ref(false);
const satisfactionScore = ref(5);
const satisfactionComment = ref("");
const submittingRating = ref(false);

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

function formatDateTime(d?: string) {
  return d ? new Date(d).toLocaleString("zh-CN") : "-";
}

async function loadTicket() {
  loading.value = true;
  try {
    const { code, data } = await customerGetTicket(ticketId.value);
    if (code === 200) ticket.value = data;
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

async function submitRating() {
  submittingRating.value = true;
  try {
    const { code, data, message: msg } = await customerRateSatisfaction(ticketId.value, {
      score: satisfactionScore.value,
      comment: satisfactionComment.value || undefined
    });
    if (code !== 200) throw new Error(msg || "评价失败");
    ticket.value = data;
    showSatisfaction.value = false;
    message("感谢您的评价！", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "评价失败", { type: "error" });
  } finally { submittingRating.value = false; }
}

onMounted(loadTicket);
</script>

<template>
  <div class="portal-page">
    <header class="portal-header">
      <div class="portal-header-left">
        <button class="portal-back" @click="router.push('/portal/tickets')">\u2190</button>
        <h1 class="portal-brand">DocFlow AI</h1>
        <span class="portal-divider">|</span>
        <span class="portal-section">工单详情</span>
      </div>
    </header>

    <div class="portal-content" v-loading="loading">
      <template v-if="ticket">
        <div class="pd-header">
          <div class="pd-header-top">
            <span class="pd-ticket-no">{{ ticket.ticketNo }}</span>
            <span class="pd-status" :style="{ background: statusColor(ticket.status) + '18', color: statusColor(ticket.status) }">
              {{ statusLabel(ticket.status) }}
            </span>
          </div>
          <h1 class="pd-title">{{ ticket.title }}</h1>
          <div class="pd-meta">
            <span>优先级: {{ priorityLabel(ticket.priority) }}</span>
            <span v-if="ticket.assigneeName">处理人: {{ ticket.assigneeName }}</span>
            <span>提交于 {{ formatDateTime(ticket.createTime) }}</span>
          </div>
        </div>

        <div class="pd-card">
          <h3 class="pd-card-title">问题描述</h3>
          <p class="pd-content">{{ ticket.content }}</p>
        </div>

        <!-- Timeline -->
        <div v-if="ticket.timeline?.length" class="pd-card">
          <h3 class="pd-card-title">处理进度</h3>
          <div class="pd-timeline">
            <div v-for="(item, idx) in ticket.timeline" :key="idx" class="pd-tl-item">
              <div class="pd-tl-dot" />
              <div class="pd-tl-body">
                <p class="pd-tl-action">{{ item.actionType }}</p>
                <p v-if="item.remark" class="pd-tl-remark">{{ item.remark }}</p>
                <p class="pd-tl-meta">{{ item.operatorName }} \u00b7 {{ item.time }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Satisfaction -->
        <div v-if="ticket.satisfactionScore" class="pd-card pd-rating-card">
          <h3 class="pd-card-title">您的评价</h3>
          <div class="pd-stars">
            <span v-for="i in 5" :key="i" :class="{ filled: i <= (ticket.satisfactionScore || 0) }">\u2605</span>
          </div>
        </div>

        <div v-else-if="ticket.status === 3 || ticket.status === 4" class="pd-card pd-rate-prompt">
          <h3 class="pd-card-title">请对本次服务评价</h3>
          <div v-if="!showSatisfaction">
            <button class="portal-primary-btn" @click="showSatisfaction = true">去评价</button>
          </div>
          <div v-else class="pd-rate-form">
            <div class="pd-stars-input">
              <span v-for="i in 5" :key="i" :class="{ filled: i <= satisfactionScore }" @click="satisfactionScore = i">\u2605</span>
            </div>
            <textarea v-model="satisfactionComment" rows="3" placeholder="可选，请分享您的感受" class="pd-rate-textarea" />
            <button class="portal-primary-btn" :disabled="submittingRating" @click="submitRating">
              {{ submittingRating ? '提交中...' : '提交评价' }}
            </button>
          </div>
        </div>
      </template>

      <div v-else-if="!loading" class="portal-empty">工单不存在</div>
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

.portal-content { max-width: 700px; margin: 0 auto; padding: 24px 20px; }
.portal-empty { text-align: center; padding: 60px; color: #94a3b8; }

.pd-header {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 12px;
  padding: 20px 24px; margin-bottom: 14px;
}
.pd-header-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.pd-ticket-no { font-family: monospace; font-size: 12px; color: #667eea; font-weight: 600; }
.pd-status { padding: 4px 12px; border-radius: 14px; font-size: 12px; font-weight: 700; }
.pd-title { font-size: 18px; font-weight: 800; color: #0d253d; margin: 0 0 8px; }
.pd-meta { display: flex; gap: 16px; font-size: 12px; color: #94a3b8; flex-wrap: wrap; }

.pd-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 12px;
  padding: 18px 24px; margin-bottom: 14px;
}
.pd-card-title { font-size: 14px; font-weight: 700; color: #0d253d; margin: 0 0 12px; }
.pd-content { font-size: 14px; color: #374151; line-height: 1.7; white-space: pre-wrap; margin: 0; }

.pd-timeline { padding-left: 20px; border-left: 2px solid #e8ecf1; }
.pd-tl-item { position: relative; padding-bottom: 16px; }
.pd-tl-item:last-child { padding-bottom: 0; }
.pd-tl-dot {
  position: absolute; left: -25px; top: 4px;
  width: 10px; height: 10px; border-radius: 50%;
  background: #667eea; border: 2px solid #fff;
}
.pd-tl-action { font-size: 13px; font-weight: 600; color: #0d253d; margin: 0; }
.pd-tl-remark { font-size: 12px; color: #64748d; margin: 2px 0 0; }
.pd-tl-meta { font-size: 11px; color: #94a3b8; margin: 4px 0 0; }

.pd-rating-card .pd-stars,
.pd-rate-form .pd-stars-input { display: flex; gap: 4px; }
.pd-rating-card .pd-stars span,
.pd-rate-form .pd-stars-input span {
  font-size: 24px; color: #e2e8f0; cursor: pointer; transition: color 0.15s;
}
.pd-rating-card .pd-stars span.filled,
.pd-rate-form .pd-stars-input span.filled { color: #f59e0b; }

.pd-rate-textarea {
  width: 100%; padding: 10px; border: 1px solid #e2e8f0; border-radius: 8px;
  font-size: 13px; outline: none; margin: 12px 0; font-family: inherit;
  box-sizing: border-box; resize: vertical;
}
.pd-rate-textarea:focus { border-color: #667eea; }

.portal-primary-btn {
  padding: 10px 20px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 14px; font-weight: 600; cursor: pointer;
  transition: opacity 0.2s;
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
