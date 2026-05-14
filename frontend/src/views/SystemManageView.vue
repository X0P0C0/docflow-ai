<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />
    <main class="main-content">
      <AppTopbar />
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>{{ systemCopy.title }}</h3>
            <p>{{ systemCopy.description }}</p>
          </div>
          <span class="chip chip-blue">{{ systemCopy.chipText }}</span>
        </div>
        <div class="ticket-overview-grid">
          <article v-for="card in systemCopy.cards" :key="card.label" class="ticket-overview-card">
            <span class="muted">{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <span>{{ card.description }}</span>
          </article>
        </div>
        <div class="state-box system-manage-hint">
          {{ systemCopy.hint }}
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { buildSystemManageCopy } from '../access-policy'
import { canManageSystem } from '../authz'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopbar from '../components/layout/AppTopbar.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'

const systemCopy = computed(() => buildSystemManageCopy({
  canManageSystem: canManageSystem(),
}))
</script>
