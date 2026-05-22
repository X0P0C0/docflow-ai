<template>
  <div
    class="app-shell"
    :class="{ 'is-sidebar-collapsed': isSidebarCollapsed }"
  >
    <div
      class="sidebar-overlay"
      :class="{ 'is-visible': isSidebarOpen }"
      @click="closeSidebar"
    />

    <AppSidebar
      :workspace-nav="workspaceNav"
      :manage-nav="manageNav"
      :is-open="isSidebarOpen"
      :is-collapsed="isSidebarCollapsed"
      @close="closeSidebar"
    />

    <main class="main-content">
      <AppTopbar
        :is-sidebar-open="isSidebarOpen"
        :is-sidebar-collapsed="isSidebarCollapsed"
        @toggle-sidebar="toggleSidebar"
        @toggle-collapse="toggleCollapse"
      />
      <section v-if="notice" class="state-box state-warning app-shell-notice">
        {{ notice }}
      </section>
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import AppSidebar from "./AppSidebar.vue";
import AppTopbar from "./AppTopbar.vue";
import type { NavItem } from "../../types/dashboard";

defineProps<{
  workspaceNav: NavItem[];
  manageNav: NavItem[];
  notice?: string;
}>();

const isSidebarOpen = ref(false);
const isSidebarCollapsed = ref(false);

function toggleSidebar() {
  isSidebarOpen.value = !isSidebarOpen.value;
}

function closeSidebar() {
  isSidebarOpen.value = false;
}

function toggleCollapse() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value;
}
</script>
