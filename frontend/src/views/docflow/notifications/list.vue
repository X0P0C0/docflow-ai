<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "@/utils/message";
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, type NotificationItem } from "@/api/notification";

defineOptions({ name: "DocflowNotifications" });

const loading = ref(false);
const notifications = ref<NotificationItem[]>([]);
const unreadCount = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

async function loadNotifications() {
  loading.value = true;
  try {
    const { code, data } = await getNotifications({ page: currentPage.value, size: pageSize.value });
    if (code === 200) {
      notifications.value = Array.isArray(data) ? data : (data?.records || []);
      total.value = data.total;
    }
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

async function loadUnread() {
  try {
    const { code, data } = await getUnreadCount();
    if (code === 200) unreadCount.value = data.unreadCount;
  } catch { /* silently fail */ }
}

async function handleMarkRead(n: NotificationItem) {
  try {
    await markAsRead(n.id);
    n.read = true;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
  } catch { /* silently fail */ }
}

async function handleMarkAllRead() {
  try {
    await markAllAsRead();
    notifications.value.forEach(n => n.read = true);
    unreadCount.value = 0;
    message("已全部标记为已读", { type: "success" });
  } catch { message("操作失败", { type: "error" }); }
}

function notifIcon(type?: string) {
  const map: Record<string, string> = {
    TICKET: "ep:tickets",
    SYSTEM: "ep:bell",
    AI: "ep:magic-stick",
    SLA: "ep:alarm-clock"
  };
  return map[type || ""] || "ep:notification";
}

function formatTime(d?: string) {
  if (!d) return "";
  const diff = Date.now() - new Date(d).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "刚刚";
  if (mins < 60) return `${mins}分钟前`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}小时前`;
  return new Date(d).toLocaleDateString("zh-CN");
}

onMounted(() => { loadNotifications(); loadUnread(); });
</script>

<template>
  <div class="p-4">
    <div class="nl-top">
      <div class="nl-top-left">
        <h1 class="nl-title">通知中心</h1>
        <span v-if="unreadCount" class="nl-badge">{{ unreadCount }}</span>
      </div>
      <el-button v-if="unreadCount" @click="handleMarkAllRead">全部已读</el-button>
    </div>

    <div class="nl-card" v-loading="loading">
      <div v-for="n in notifications" :key="n.id" class="nl-item" :class="{ unread: !n.read }">
        <div class="nl-item-icon">
          <el-icon :size="18"><component :is="notifIcon(n.type)" /></el-icon>
        </div>
        <div class="nl-item-body">
          <p class="nl-item-title">{{ n.title }}</p>
          <p class="nl-item-content">{{ n.content }}</p>
          <span class="nl-item-time">{{ formatTime(n.createTime) }}</span>
        </div>
        <el-button v-if="!n.read" size="small" text @click="handleMarkRead(n)">标记已读</el-button>
      </div>

      <el-empty v-if="!loading && !notifications.length" description="暂无通知" />

      <div v-if="total > pageSize" class="nl-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="loadNotifications"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
.nl-top {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px;
}
.nl-top-left { display: flex; align-items: center; gap: 8px; }
.nl-title { font-size: 22px; font-weight: 800; color: #0d253d; margin: 0; }
.nl-badge {
  background: #ef4444; color: #fff; font-size: 12px; font-weight: 700;
  padding: 2px 8px; border-radius: 10px; min-width: 20px; text-align: center;
}

.nl-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 10px; overflow: hidden;
}
.nl-item {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 14px 18px; border-bottom: 1px solid #f1f5f9;
  transition: background 0.15s;
}
.nl-item:last-child { border-bottom: none; }
.nl-item:hover { background: #f8fafc; }
.nl-item.unread { background: #f0f5ff; }
.nl-item.unread:hover { background: #e8efff; }

.nl-item-icon {
  width: 36px; height: 36px; border-radius: 8px; background: #f1f5f9;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  color: #64748d;
}
.nl-item.unread .nl-item-icon { background: #667eea18; color: #667eea; }

.nl-item-body { flex: 1; min-width: 0; }
.nl-item-title { font-size: 14px; font-weight: 600; color: #0d253d; margin: 0 0 2px; }
.nl-item.unread .nl-item-title { color: #1e293b; }
.nl-item-content { font-size: 13px; color: #64748d; margin: 0 0 4px; line-height: 1.4; }
.nl-item-time { font-size: 11px; color: #94a3b8; }

.nl-pagination { display: flex; justify-content: center; padding: 12px; border-top: 1px solid #f1f5f9; }

.dark .nl-title { color: #f1f5f9; }
.dark .nl-card { background: #1e293b; border-color: #334155; }
.dark .nl-item { border-bottom-color: #334155; }
.dark .nl-item:hover { background: rgba(255,255,255,0.02); }
.dark .nl-item.unread { background: rgba(102,126,234,0.08); }
.dark .nl-item-title { color: #f1f5f9; }
.dark .nl-item-icon { background: #334155; }
.dark .nl-item.unread .nl-item-icon { background: #667eea28; }

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
