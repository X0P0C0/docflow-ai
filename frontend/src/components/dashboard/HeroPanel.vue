<template>
  <section class="hero-panel">
    <div class="hero-copy">
      <span class="hero-tag">Linear x Notion x AI Workspace</span>
      <h2>{{ heroHeadline }}</h2>
      <p>{{ heroDescription }}</p>
      <div class="hero-inline-note">
        <span class="chip" :class="modeChipClass">{{ runtimeMode }}</span>
        <span>当前账号：{{ currentUserName }}</span>
      </div>
      <section class="dashboard-mode-panel" :class="modePanelClass">
        <div class="dashboard-mode-copy">
          <strong>{{ modeHeadline }}</strong>
          <p>{{ modeDescription }}</p>
        </div>
        <span class="dashboard-mode-badge">{{ runtimeMode }}</span>
      </section>
      <div class="hero-actions">
        <button class="primary-button" type="button" @click="navigateTo(primaryAction.to)">{{ primaryAction.label }}</button>
        <button class="ghost-button" type="button" @click="navigateTo(secondaryAction.to)">{{ secondaryAction.label }}</button>
      </div>
      <p class="hero-capability-note">{{ capabilityNote }}</p>
    </div>

    <div class="hero-stats">
      <div class="stat-row">
        <div>
          <span class="muted">待处理工单</span>
          <strong>{{ pendingCount }}</strong>
        </div>
        <span class="chip chip-orange">当前需要优先跟进</span>
      </div>
      <div class="stat-row">
        <div>
          <span class="muted">知识覆盖率</span>
          <strong>{{ knowledgeCoverage }}%</strong>
        </div>
        <span class="chip chip-green">已接入当前工作台</span>
      </div>
      <div class="stat-row">
        <div>
          <span class="muted">本地可继续记录</span>
          <strong>{{ localCount }}</strong>
        </div>
        <span class="chip chip-blue">断开后端也能继续演示</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authState } from '../../auth'

const route = useRoute()
const router = useRouter()

const props = defineProps<{
  pendingCount: number
  knowledgeCoverage: number
  localCount: number
  runtimeMode: string
  heroHeadline: string
  heroDescription: string
  capabilityNote: string
  primaryAction: {
    label: string
    to: string
  }
  secondaryAction: {
    label: string
    to: string
  }
}>()

const currentUserName = computed(() => authState.user?.nickname || authState.user?.realName || authState.user?.username || '当前用户')
const isDemoRuntime = computed(() => props.runtimeMode === '演示模式')
const modeChipClass = computed(() => (isDemoRuntime.value ? 'chip-orange' : 'chip-green'))
const modePanelClass = computed(() => (isDemoRuntime.value ? 'dashboard-mode-panel-demo' : 'dashboard-mode-panel-live'))
const modeHeadline = computed(() => (isDemoRuntime.value ? '首页当前展示的是演示模式' : '首页当前展示的是正常模式'))
const modeDescription = computed(() => (
  isDemoRuntime.value
    ? '当前页面中的工单、知识和流转反馈会优先使用本地演示数据，适合预览页面和讲解流程。'
    : '当前页面的数据优先来自后端接口，后续新增、编辑和状态变更会按真实业务链路处理。'
))

function navigateTo(path: string) {
  if (route.path === path) {
    return
  }
  router.push(path)
}
</script>
