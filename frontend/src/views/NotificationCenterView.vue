<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />
    <main class="main-content">
      <AppTopbar />
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>{{ notificationCopy.title }}</h3>
            <p>{{ notificationCopy.description }}</p>
          </div>
          <span class="chip chip-orange">{{ notificationCopy.unreadCount }} Unread</span>
        </div>
        <div class="state-box">
          当前通知会根据你的能力边界调整关注重点，处理侧更偏向流转和审核，跟进侧更偏向进展和知识回查。
        </div>
        <div class="mini-list">
          <div v-for="item in notificationCopy.items" :key="item.message + item.time" class="mini-item">
            <span>{{ item.message }}</span><span>{{ item.time }}</span>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { buildNotificationCenterCopy } from '../access-policy'
import { canAccessAiCenter, canManageKnowledgeArticles, canManageSystem } from '../authz'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopbar from '../components/layout/AppTopbar.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'

const notificationCopy = computed(() => buildNotificationCenterCopy({
  canManageKnowledge: canManageKnowledgeArticles(),
  canAccessAiCenter: canAccessAiCenter(),
  canManageSystem: canManageSystem(),
}))
</script>
