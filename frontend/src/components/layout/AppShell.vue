<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />

    <main class="main-content">
      <AppTopbar />
      <section v-if="notice" class="state-box state-warning app-shell-notice">
        {{ notice }}
      </section>
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import AppTopbar from './AppTopbar.vue'
import type { NavItem } from '../../types/dashboard'

defineProps<{
  workspaceNav: NavItem[]
  manageNav: NavItem[]
  notice?: string
}>()
</script>
