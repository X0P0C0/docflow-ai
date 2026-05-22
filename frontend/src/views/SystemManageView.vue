<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
    <section class="workspace-page">
      <section class="panel workspace-card">
        <div class="workspace-header">
          <div class="workspace-header-main">
            <div class="workspace-title">
              <span class="page-eyebrow">Settings</span>
              <h2>{{ systemCopy.title }}</h2>
              <p>{{ systemCopy.description }}</p>
            </div>
            <div class="workspace-actions">
              <RouterLink class="ghost-button" to="/profile">查看当前账号</RouterLink>
              <RouterLink class="primary-button" :to="primaryAction.to">{{ primaryAction.label }}</RouterLink>
            </div>
          </div>

          <div class="workspace-stats">
            <article v-for="stat in overviewStats" :key="stat.label" class="workspace-stat">
              <span class="workspace-stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.description }}</p>
            </article>
          </div>

        </div>
      </section>

      <section class="system-grid">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>治理范围概览</h3>
              <p>把当前版本已经接上的治理范围先讲清楚，避免系统页只剩下“只读展示”的感觉。</p>
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

        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>下一步治理动作</h3>
              <p>系统页的价值不只在概览，还要把后续应该往哪里扩清楚。</p>
            </div>
          </div>

          <div class="mini-list">
            <div v-for="item in governanceActions" :key="item.title" class="mini-item system-action-item">
              <div class="workspace-link-main">
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </div>
              <span class="chip" :class="item.chipClass">{{ item.badge }}</span>
            </div>
          </div>
        </section>
      </section>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { buildSystemManageCopy } from '../access-policy'
import { canManageSystem } from '../authz'
import AppShell from '../components/layout/AppShell.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

const canUseSystemManage = computed(() => canManageSystem())

const systemCopy = computed(() => buildSystemManageCopy({
  canManageSystem: canUseSystemManage.value,
}))

const _runtimeModeText = computed(() => getRuntimeModeText())
const _runtimeHeadline = computed(() => getRuntimeModeHeadline())
const _runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: false,
  subject: '系统管理',
}))

const overviewStats = computed(() => [
  {
    label: '当前治理模式',
    value: canUseSystemManage.value ? '可管理' : '只读',
    description: canUseSystemManage.value ? '可以继续承接角色、模板与参数治理入口' : '当前账号仅适合确认系统结构与治理范围',
  },
  {
    label: '角色边界',
    value: systemCopy.value.cards[0]?.value ?? '-',
    description: '当前版本已经形成基础角色与能力范围骨架',
  },
  {
    label: '通知模板',
    value: systemCopy.value.cards.find((item) => item.label.includes('通知'))?.value ?? '-',
    description: '可继续承接站内信、协作提醒与治理信号模板',
  },
  {
    label: '当前重点',
    value: canUseSystemManage.value ? '治理扩展' : '结构认知',
    description: canUseSystemManage.value ? '适合继续往角色、分类与模板治理收口' : '适合先确认哪些入口仍需要管理员账号参与',
  },
])

const primaryAction = computed(() => (
  canUseSystemManage.value
    ? { label: '回看通知信号', to: '/notifications' }
    : { label: '回到工作台', to: '/dashboard' }
))

const governanceActions = computed(() => (
  canUseSystemManage.value
    ? [
        {
          title: '继续补角色与能力治理',
          description: '把页面入口、能力点与角色边界继续往同一套治理口径收紧。',
          badge: 'RBAC',
          chipClass: 'chip-blue',
        },
        {
          title: '扩通知模板与系统提醒',
          description: '把工单、知识和治理事件的提醒模板补成一套可维护结构。',
          badge: 'Template',
          chipClass: 'chip-green',
        },
        {
          title: '梳理系统参数与分类入口',
          description: '后续可以继续把分类、参数与辅助配置挂进系统治理主线。',
          badge: 'Config',
          chipClass: 'chip-orange',
        },
      ]
    : [
        {
          title: '确认当前账号的治理边界',
          description: '先从个人中心和通知页回看当前账号能看到什么、不能改什么。',
          badge: 'Scope',
          chipClass: 'chip-blue',
        },
        {
          title: '切回业务主线继续推进',
          description: '如果暂时没有系统治理能力，优先回到工单与知识沉淀主线。',
          badge: 'Flow',
          chipClass: 'chip-green',
        },
        {
          title: '需要治理改动时切换账号',
          description: '涉及角色、模板或参数修改时，再切换到具备系统管理能力的账号。',
          badge: 'Admin',
          chipClass: 'chip-orange',
        },
      ]
))
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



.system-action-item p {
  margin: 0;
}

.system-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
  gap: var(--space-4);
}

.system-manage-hint {
  margin-top: var(--space-4);
  margin-bottom: 0;
}

.system-action-item {
  align-items: flex-start;
}

@media (max-width: 1280px) {
  .system-grid {
    grid-template-columns: 1fr;
  }
}
</style>
