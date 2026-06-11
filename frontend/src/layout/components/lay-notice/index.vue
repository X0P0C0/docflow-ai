<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import dayjs from "dayjs";
import { getTickets, type TicketListItem } from "@/api/tickets";
import { getTicketStatusLabel } from "@/constants/tickets";
import { useWebSocket, type WsNotification } from "@/utils/websocket";
import BellIcon from "~icons/lucide/bell";

const router = useRouter();
const dropdownRef = ref();
const pendingTickets = ref<TicketListItem[]>([]);
const loading = ref(false);

// WebSocket integration
const { notifications: wsNotifications, connected, connect, disconnect } = useWebSocket();

const badgeCount = computed(() => {
  const ticketCount = pendingTickets.value.length;
  const wsCount = wsNotifications.value.length;
  return Math.min(ticketCount + wsCount, 99);
});

const wsUnreadCount = computed(() => wsNotifications.value.length);

function statusTagType(status?: number | null): "success" | "warning" | "info" {
  if (status === 3 || status === 4) return "success";
  if (status === 2) return "warning";
  return "info";
}

function formatRelative(value?: string | null) {
  if (!value) return "";
  const d = dayjs(value);
  const now = dayjs();
  const diff = now.diff(d, "minute");
  if (diff < 1) return "刚刚";
  if (diff < 60) return `${diff} 分钟前`;
  const hourDiff = now.diff(d, "hour");
  if (hourDiff < 24) return `${hourDiff} 小时前`;
  return d.format("MM-DD");
}

function wsTimeAgo(ts: number) {
  return formatRelative(dayjs(ts).toISOString());
}

async function loadPending() {
  loading.value = true;
  try {
    const { code, data } = await getTickets({ status: 1, size: 10 });
    if (code === 200) {
      pendingTickets.value = data.records.filter(t => t.status === 1 || t.status === 2);
    }
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

function goTicket(id: number) {
  router.push(`/tickets/detail/${id}`);
  dropdownRef.value?.handleClose?.();
}

function goAll() {
  router.push("/tickets/list");
  dropdownRef.value?.handleClose?.();
}

function goNotifications() {
  router.push("/notifications");
  dropdownRef.value?.handleClose?.();
}

onMounted(() => {
  loadPending();
  connect();
});

onUnmounted(() => {
  disconnect();
});
</script>

<template>
  <el-dropdown ref="dropdownRef" trigger="click" placement="bottom-end">
    <span class="dropdown-badge navbar-bg-hover select-none mr-1.75">
      <el-badge :value="badgeCount" :hidden="badgeCount === 0" :max="99">
        <span class="header-notice-icon">
          <IconifyIconOffline :icon="BellIcon" />
        </span>
      </el-badge>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <div style="width: 360px; max-height: 460px;">
          <!-- Header -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <div class="flex items-center gap-2">
              <span class="font-semibold text-sm">通知中心</span>
              <span v-if="connected" class="w-2 h-2 rounded-full bg-green-400" title="实时连接中" />
              <span v-else class="w-2 h-2 rounded-full bg-gray-300" title="未连接" />
            </div>
            <span v-if="badgeCount" class="text-xs text-gray-400">{{ badgeCount }} 条</span>
          </div>

          <!-- WebSocket Real-time Notifications -->
          <div v-if="wsNotifications.length" class="border-b border-gray-100">
            <div class="px-4 py-2 text-xs text-gray-400 font-medium">实时通知</div>
            <el-scrollbar max-height="120px">
              <div v-for="(notif, idx) in wsNotifications.slice(0, 5)" :key="idx"
                class="flex items-start gap-3 px-4 py-2 cursor-pointer hover:bg-blue-50 transition-colors">
                <div class="w-2 h-2 rounded-full bg-blue-500 shrink-0 mt-1.5" />
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-gray-800 line-clamp-1">{{ notif.title }}</p>
                  <p class="text-xs text-gray-500 line-clamp-1 mt-0.5">{{ notif.content }}</p>
                  <span class="text-xs text-gray-300 mt-0.5">{{ wsTimeAgo(notif.timestamp) }}</span>
                </div>
              </div>
            </el-scrollbar>
          </div>

          <!-- Pending Tickets -->
          <div>
            <div class="px-4 py-2 text-xs text-gray-400 font-medium">待处理工单</div>
            <el-scrollbar max-height="200px">
              <div v-if="pendingTickets.length" class="py-1">
                <div v-for="ticket in pendingTickets" :key="ticket.id"
                  class="flex items-start gap-3 px-4 py-2.5 cursor-pointer hover:bg-gray-50 transition-colors"
                  @click="goTicket(ticket.id)">
                  <div class="shrink-0 mt-0.5">
                    <div class="w-2 h-2 rounded-full"
                      :class="ticket.status === 2 ? 'bg-amber-400' : 'bg-blue-400'" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-sm text-gray-800 line-clamp-1">{{ ticket.title }}</p>
                    <div class="flex items-center gap-2 mt-1">
                      <el-tag :type="statusTagType(ticket.status)" size="small" effect="plain">
                        {{ getTicketStatusLabel(ticket.status) }}
                      </el-tag>
                      <span v-if="ticket.assigneeName" class="text-xs text-gray-400">{{ ticket.assigneeName }}</span>
                      <span v-else class="text-xs text-orange-400">待分配</span>
                      <span class="text-xs text-gray-300">{{ formatRelative(ticket.updateTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="flex items-center justify-center h-24 text-sm text-gray-400">
                暂无待处理工单
              </div>
            </el-scrollbar>
          </div>

          <!-- Footer -->
          <div class="border-t border-gray-100 flex">
            <el-button text size="small" class="!flex-1" @click="goAll">全部工单</el-button>
            <el-button text size="small" class="!flex-1" @click="goNotifications">通知列表</el-button>
          </div>
        </div>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style lang="scss" scoped>
@keyframes pure-bell-ring {
  0%, 100% { transform-origin: top; }
  15% { transform: rotateZ(10deg); }
  30% { transform: rotateZ(-10deg); }
  45% { transform: rotateZ(5deg); }
  60% { transform: rotateZ(-5deg); }
  75% { transform: rotateZ(2deg); }
}
.line-clamp-1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
</style>
