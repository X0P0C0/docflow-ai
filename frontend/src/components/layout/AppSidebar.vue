<template>
  <aside class="sidebar">
    <div class="brand-card">
      <div class="brand-mark">D</div>
      <div>
        <h1>DocFlow AI</h1>
        <p>团队知识库与工单协同平台</p>
      </div>
    </div>

    <section class="nav-section">
      <div class="nav-label">Workspace</div>
      <RouterLink
        v-for="item in visibleWorkspaceNav"
        :key="item.name"
        class="nav-item"
        :class="{ active: isActive(item.to) }"
        :to="item.to"
      >
        <span>{{ item.name }}</span>
        <span class="nav-badge">{{ navBadgeText(item.to, item.badge) }}</span>
      </RouterLink>
    </section>

    <section class="nav-section">
      <div class="nav-label">Manage</div>
      <RouterLink
        v-for="item in visibleManageNav"
        :key="item.name"
        class="nav-item"
        :class="{ active: isActive(item.to) }"
        :to="item.to"
      >
        <span>{{ item.name }}</span>
        <span class="nav-text">{{ navBadgeText(item.to, item.badge) }}</span>
      </RouterLink>
    </section>

    <section class="ai-card">
      <span class="chip" :class="aiAccessChipClass">{{ aiAccessChipText }}</span>
      <h3>{{ aiAccessTitle }}</h3>
      <p>{{ aiAccessDescription }}</p>
      <button
        class="primary-button"
        type="button"
        :disabled="!canOpenAiCenter"
        @click="handleAiAction"
      >
        {{ aiAccessActionText }}
      </button>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { buildAiCenterAccessCopy, buildSidebarBadgeMap } from '../../access-policy'
import { authState } from '../../auth'
import { canAccessAiCenter, canAccessRoute, canManageKnowledgeArticles, canManageSystem } from '../../authz'
import type { NavItem } from '../../types/dashboard'

const route = useRoute()
const router = useRouter()

const props = defineProps<{
  workspaceNav: NavItem[]
  manageNav: NavItem[]
}>()

const visibleWorkspaceNav = computed(() => props.workspaceNav.filter((item) => canAccessRoute(item.to)))
const visibleManageNav = computed(() => props.manageNav.filter((item) => canAccessRoute(item.to)))
const canOpenAiCenter = computed(() => canAccessAiCenter())
const sidebarBadgeMap = computed(() => buildSidebarBadgeMap({
  canManageKnowledge: canManageKnowledgeArticles(),
  canAccessAiCenter: canOpenAiCenter.value,
  canManageSystem: canManageSystem(),
}))
const roleText = computed(() => authState.user?.roles?.join(' / ') || '访客')
const aiAccessCopy = computed(() => buildAiCenterAccessCopy({
  canAccess: canOpenAiCenter.value,
  roleText: roleText.value,
}))
const aiAccessChipClass = computed(() => (canOpenAiCenter.value ? 'chip-blue' : 'chip-default'))
const aiAccessChipText = computed(() => aiAccessCopy.value.chipText)
const aiAccessTitle = computed(() => aiAccessCopy.value.title)
const aiAccessDescription = computed(() => aiAccessCopy.value.description)
const aiAccessActionText = computed(() => aiAccessCopy.value.actionText)

function isActive(target: string) {
  return route.path === target || route.path.startsWith(`${target}/`)
}

function navBadgeText(target: string, fallback: string) {
  return sidebarBadgeMap.value[target as keyof typeof sidebarBadgeMap.value] || fallback
}

function handleAiAction() {
  if (!canOpenAiCenter.value) {
    return
  }
  router.push('/ai-center')
}
</script>
