<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import dayjs from "dayjs";
import { getTickets, type TicketListItem } from "@/api/tickets";
import { getTicketStatusLabel } from "@/constants/tickets";

import BellIcon from "~icons/lucide/bell";

const router = useRouter();
const dropdownRef = ref();
const pendingTickets = ref<TicketListItem[]>([]);
const loading = ref(false);

const badgeCount = computed(() => pendingTickets.value.length);

function statusTagType(status?: number | null) {
  if (status === 3 || status === 4) return "success";
  if (status === 2) return "warning";
  if (status === 1) return "info";
  return "";
}

function formatRelative(value?: string | null) {
  if (!value) return "";
  const d = dayjs(value);
  const now = dayjs();
  const diff = now.diff(d, "hour");
  if (diff < 1) return "刚刚";
  if (diff < 24) return `${diff} 小时前`;
  return d.format("MM-DD");
}

async function loadPending() {
  loading.value = true;
  try {
    const { code, data } = await getTickets();
    if (code === 200) {
      pendingTickets.value = data.records.filter(t => t.status === 1 || t.status === 2);
    }
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

function goTicket(id: number) {
  router.push(`/tickets/${id}`);
  dropdownRef.value?.handleClose();
}

function goAll() {
  router.push("/tickets/list");
  dropdownRef.value?.handleClose();
}

onMounted(() => loadPending());
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
        <div style="width: 340px; max-height: 400px;">
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-gray-700">
            <span class="font-semibold text-sm">待处理工单</span>
            <span v-if="badgeCount" class="text-xs text-gray-400">{{ badgeCount }} 张</span>
          </div>
          <el-scrollbar max-height="320px">
            <div v-if="pendingTickets.length" class="py-1">
              <div
                v-for="ticket in pendingTickets"
                :key="ticket.id"
                class="flex items-start gap-3 px-4 py-2.5 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                @click="goTicket(ticket.id)"
              >
                <div class="shrink-0 mt-0.5">
                  <div class="w-2 h-2 rounded-full"
                    :class="ticket.status === 2 ? 'bg-amber-400' : 'bg-blue-400'" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm text-gray-800 dark:text-gray-200 line-clamp-1">{{ ticket.title }}</p>
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
            <div v-else class="flex items-center justify-center h-32 text-sm text-gray-400">
              暂无待处理工单
            </div>
          </el-scrollbar>
          <div class="border-t border-gray-100 dark:border-gray-700 px-4 py-2.5">
            <el-button text size="small" class="!w-full" @click="goAll">查看全部工单</el-button>
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

.dropdown-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 48px;
  cursor: pointer;

  .header-notice-icon { font-size: 16px; }

  &:hover {
    .header-notice-icon svg { animation: pure-bell-ring 1s both; }
  }
}
</style>