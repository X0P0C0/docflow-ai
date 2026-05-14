<template>
  <article class="panel">
    <div class="panel-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <RouterLink to="/tickets">{{ actionText }}</RouterLink>
    </div>
    <ErrorTraceNotice
      v-if="errorMessage"
      :message="`${errorMessage}，当前先展示本地工作台数据。`"
      :trace-id="errorTraceId"
    />
    <div class="ticket-list">
      <RouterLink v-for="ticket in items" :key="ticket.id" class="ticket-item ticket-link" :to="`/tickets/${ticket.id}`">
        <div>
          <strong>{{ ticket.title }}</strong>
          <p>{{ ticket.meta }}</p>
        </div>
        <div class="ticket-tags">
          <span class="chip" :class="ticket.priorityClass">{{ ticket.priority }}</span>
          <span class="chip" :class="statusChipClass(ticket.status)">{{ ticket.status }}</span>
        </div>
      </RouterLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import ErrorTraceNotice from '../common/ErrorTraceNotice.vue'
import type { TicketItem } from '../../types/dashboard'

defineProps<{
  items: TicketItem[]
  errorMessage: string
  errorTraceId: string
  title: string
  description: string
  actionText: string
}>()

function statusChipClass(status: string) {
  if (status === '已解决') {
    return 'chip-green'
  }
  if (status === '已关闭') {
    return 'chip-blue'
  }
  return 'chip-orange'
}
</script>
