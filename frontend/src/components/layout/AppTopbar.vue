<template>
  <div class="topbar-stack">
    <header class="topbar">
      <button class="search-box search-button" type="button" @click="navigateTo('/knowledge/articles')">
        搜索知识文章、工单、标签或成员...
      </button>
      <div class="topbar-actions">
        <span class="chip" :class="modeChipClass">{{ modeText }}</span>
        <span class="chip chip-default">{{ roleText }}</span>
        <button class="ghost-button" type="button" @click="navigateTo('/tickets/create')">新建工单</button>
        <button
          v-if="knowledgeAction.visible"
          class="primary-button"
          type="button"
          @click="navigateTo('/knowledge/articles/create')"
        >
          {{ knowledgeAction.label }}
        </button>
        <button class="ghost-button" type="button" @click="navigateTo('/profile')">
          <span class="topbar-profile-label">个人中心</span>
          <span class="avatar avatar-inline">{{ avatarText }}</span>
        </button>
        <button class="ghost-button" type="button" @click="handleLogout">退出</button>
      </div>
    </header>

    <section class="runtime-banner" :class="bannerClass">
      <div class="runtime-banner-copy">
        <strong>{{ modeHeadline }}</strong>
        <p>{{ modeDescription }}</p>
      </div>
      <span class="runtime-banner-badge">{{ modeText }}</span>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { buildTopbarKnowledgeActionCopy } from '../../access-policy'
import { authState, clearSession } from '../../auth'
import { canManageKnowledgeArticles } from '../../authz'
import { isDemoMode } from '../../utils/runtimeMode'

const route = useRoute()
const router = useRouter()

const avatarText = computed(() => {
  const name = authState.user?.nickname || authState.user?.realName || authState.user?.username || 'D'
  return name.slice(0, 1).toUpperCase()
})

const demoMode = computed(() => isDemoMode())
const canPublishKnowledge = computed(() => canManageKnowledgeArticles())
const knowledgeAction = computed(() => buildTopbarKnowledgeActionCopy({
  canManageKnowledge: canPublishKnowledge.value,
}))
const modeText = computed(() => (demoMode.value ? '演示模式' : '正常模式'))
const modeChipClass = computed(() => (demoMode.value ? 'chip-orange' : 'chip-green'))
const roleText = computed(() => authState.user?.roles?.join(' / ') || '访客')
const bannerClass = computed(() => (demoMode.value ? 'runtime-banner-demo' : 'runtime-banner-live'))
const modeHeadline = computed(() => (demoMode.value ? '当前为演示模式' : '当前为正常模式'))
const modeDescription = computed(() => (
  demoMode.value
    ? '后端当前未参与本次操作，页面中的数据和交互会以本地演示数据为准。'
    : '当前页面已经连接后端接口，新增、编辑和状态流转会优先写入真实数据。'
))

function handleLogout() {
  clearSession()
  router.replace('/login')
}

function navigateTo(path: string) {
  if (route.path === path) {
    return
  }
  router.push(path)
}
</script>

<style scoped>
.topbar-profile-label {
  display: inline-flex;
  align-items: center;
}

.avatar-inline {
  width: 2rem;
  height: 2rem;
  margin-left: 0.5rem;
  border-radius: 0.9rem;
  font-size: 0.85rem;
}
</style>
