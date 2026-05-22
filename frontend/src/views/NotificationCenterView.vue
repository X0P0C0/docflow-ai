<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
    <section class="workspace-page">
      <section class="panel workspace-card">
        <div class="workspace-header">
          <div class="workspace-header-main">
            <div class="workspace-title">
              <span class="page-eyebrow">Notifications</span>
              <h2>{{ notificationCopy.title }}</h2>
              <p>{{ notificationCopy.description }}</p>
            </div>
            <div class="workspace-actions">
              <RouterLink class="ghost-button" to="/tickets">查看工单</RouterLink>
              <RouterLink class="primary-button" :to="knowledgeAction.to">{{ knowledgeAction.label }}</RouterLink>
            </div>
          </div>

          <div class="workspace-stats">
            <article v-for="stat in overviewStats" :key="stat.label" class="workspace-stat">
              <span class="workspace-stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.description }}</p>
            </article>
          </div>

          <div class="state-box notification-runtime-banner" :class="{ 'state-warning': isDemoMode() }">
            <strong>{{ runtimeHeadline }} · {{ runtimeModeText }}</strong>
            <p>{{ runtimeDataSourceMessage }}</p>
          </div>
        </div>
      </section>

      <section class="notification-grid">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>待跟进提醒</h3>
              <p>把此刻最值得先看的协作信号聚到一列，避免在多个业务页之间来回切换。</p>
            </div>
            <span class="chip chip-orange">{{ notificationCopy.unreadCount }} Unread</span>
          </div>

          <div class="workspace-highlight-card notification-focus-card">
            <strong>{{ focusMessage.title }}</strong>
            <p>{{ focusMessage.description }}</p>
          </div>

          <div class="mini-list">
            <div v-for="item in notificationCopy.items" :key="item.message + item.time" class="mini-item notification-item">
              <div class="workspace-link-main">
                <strong>{{ item.message }}</strong>
                <p>{{ resolveToneHint(item.message) }}</p>
              </div>
              <span>{{ item.time }}</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>下一步处理入口</h3>
              <p>通知页不只展示消息，也直接给出此刻最合理的继续动作。</p>
            </div>
          </div>

          <div class="quick-link-grid">
            <RouterLink v-for="action in nextActions" :key="action.to" class="workspace-link-card quick-link-card" :to="action.to">
              <div class="workspace-link-main">
                <strong>{{ action.label }}</strong>
                <p>{{ action.description }}</p>
              </div>
              <div class="workspace-badge-row">
                <span class="chip" :class="action.chipClass">{{ action.badge }}</span>
              </div>
            </RouterLink>
          </div>
        </section>
      </section>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { buildNotificationCenterCopy } from '../access-policy'
import { canAccessAiCenter, canManageKnowledgeArticles, canManageSystem } from '../authz'
import AppShell from '../components/layout/AppShell.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

const canManageKnowledge = computed(() => canManageKnowledgeArticles())
const canUseAiCenter = computed(() => canAccessAiCenter())
const canUseSystemManage = computed(() => canManageSystem())

const notificationCopy = computed(() => buildNotificationCenterCopy({
  canManageKnowledge: canManageKnowledge.value,
  canAccessAiCenter: canUseAiCenter.value,
  canManageSystem: canUseSystemManage.value,
}))

const runtimeModeText = computed(() => getRuntimeModeText())
const runtimeHeadline = computed(() => getRuntimeModeHeadline())
const runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: false,
  subject: '通知中心',
}))

const overviewStats = computed(() => [
  {
    label: '未读事项',
    value: String(notificationCopy.value.unreadCount),
    description: '等待确认、跟进或回看的通知总数',
  },
  {
    label: '知识相关',
    value: canManageKnowledge.value ? '可审核' : '可回查',
    description: canManageKnowledge.value ? '优先处理待沉淀与待审核内容' : '优先回看知识补充与经验更新',
  },
  {
    label: 'AI 协作',
    value: canUseAiCenter.value ? '已开放' : '未开放',
    description: canUseAiCenter.value ? '可继续查看推荐、摘要与回复建议' : '当前仍以工单与知识主线跟进为主',
  },
  {
    label: '治理信号',
    value: canUseSystemManage.value ? '可见' : '只读',
    description: canUseSystemManage.value ? '系统变更通知会进入当前收件流' : '系统治理变更仅保留概览提示',
  },
])

const focusMessage = computed(() => ({
  title: canUseSystemManage.value
    ? '当前通知会同时覆盖协作流与系统治理流。'
    : '当前通知会优先服务你的业务协作与知识跟进。',
  description: canManageKnowledge.value
    ? '建议先处理需要确认、审核或沉淀的事项，再回看一般性进展更新。'
    : '建议优先回看与你相关的工单进展，再补看知识与协作侧的更新提醒。',
}))

const knowledgeAction = computed(() => (
  canManageKnowledge.value
    ? { label: '进入知识沉淀', to: '/knowledge/articles/create' }
    : { label: '查看知识回查', to: '/knowledge/articles' }
))

const nextActions = computed(() => [
  {
    label: canManageKnowledge.value ? '处理待沉淀内容' : '回看知识更新',
    description: canManageKnowledge.value
      ? '继续从已解决工单沉淀草稿、补充经验并准备发布。'
      : '快速回查新补充的排查经验与相关知识文章更新。',
    to: canManageKnowledge.value ? '/knowledge/articles/create' : '/knowledge/articles',
    badge: canManageKnowledge.value ? 'Knowledge' : 'Browse',
    chipClass: 'chip-green',
  },
  {
    label: '继续工单跟进',
    description: '把通知里的进展变化落回工单处理、评论补充和状态确认。',
    to: '/tickets',
    badge: 'Ticket',
    chipClass: 'chip-blue',
  },
  {
    label: canUseAiCenter.value ? '查看 AI 协作建议' : '查看个人能力边界',
    description: canUseAiCenter.value
      ? '继续处理 AI 推荐、摘要与回复建议命中结果。'
      : '当前账号未开放 AI 协作，可先回到个人中心确认可用能力。',
    to: canUseAiCenter.value ? '/ai-center' : '/profile',
    badge: canUseAiCenter.value ? 'AI' : 'Profile',
    chipClass: canUseAiCenter.value ? 'chip-orange' : 'chip-default',
  },
])

function resolveToneHint(message: string) {
  if (message.includes('AI')) {
    return '更适合先回看推荐命中结果，再决定是否继续沉淀。'
  }
  if (message.includes('知识')) {
    return '更适合先确认内容是否需要补充、审核或转成正式经验。'
  }
  if (message.includes('角色') || message.includes('权限') || message.includes('系统')) {
    return '属于治理类变更，建议和当前能力边界一起交叉确认。'
  }
  return '属于业务协作类提醒，建议优先回到工单现场继续跟进。'
}
</script>

<style scoped>
.page-eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.notification-runtime-banner {
  display: grid;
  gap: var(--space-2);
}

.notification-runtime-banner strong,
.notification-runtime-banner p,
.notification-item p,
.notification-focus-card p {
  margin: 0;
}

.notification-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: var(--space-4);
}

.notification-item {
  align-items: flex-start;
}

.quick-link-grid {
  display: grid;
  gap: var(--space-3);
}

.quick-link-card {
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: var(--bg-panel-solid);
}

@media (max-width: 1280px) {
  .notification-grid {
    grid-template-columns: 1fr;
  }
}
</style>
