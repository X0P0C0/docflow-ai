<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />
    <main class="main-content">
      <AppTopbar />

      <section class="panel knowledge-hero profile-hero">
        <div class="profile-hero-copy">
          <span class="hero-tag">Profile</span>
          <h2>把当前账号、协作身份和能力边界放进同一张工作名片里。</h2>
          <p>
            这一页优先承接真实登录用户信息，帮助我们快速确认当前是谁、能做什么，
            以及接下来最适合从哪个业务入口继续推进工单与知识沉淀主线。
          </p>

          <div class="profile-identity">
            <div class="profile-avatar">{{ avatarText }}</div>
            <div>
              <strong>{{ displayName }}</strong>
              <p>{{ usernameLine }}</p>
            </div>
          </div>

          <div class="profile-role-chips">
            <span v-for="role in roleList" :key="role" class="chip chip-blue">{{ role }}</span>
            <span class="chip" :class="modeChipClass">{{ runtimeModeText }}</span>
          </div>
        </div>

        <div class="hero-stats">
          <div class="stat-row">
            <div>
              <p>账号身份</p>
              <strong>{{ primaryRoleText }}</strong>
            </div>
            <span class="chip chip-blue">Role</span>
          </div>
          <div class="stat-row">
            <div>
              <p>权限点</p>
              <strong>{{ permissionCount }}</strong>
            </div>
            <span class="chip chip-default">Scope</span>
          </div>
          <div class="stat-row">
            <div>
              <p>能力集</p>
              <strong>{{ capabilityCount }}</strong>
            </div>
            <span class="chip chip-green">Capability</span>
          </div>
          <div class="stat-row">
            <div>
              <p>当前工作焦点</p>
              <strong>{{ workspaceFocus }}</strong>
            </div>
            <span class="chip chip-orange">Focus</span>
          </div>
        </div>
      </section>

      <section class="profile-grid">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>账号概览</h3>
              <p>直接对照当前登录用户的真实资料，后续继续接头像、偏好设置和团队信息时也有落点。</p>
            </div>
          </div>

          <div class="mini-list">
            <div class="mini-item"><span>显示名称</span><strong>{{ displayName }}</strong></div>
            <div class="mini-item"><span>用户名</span><strong>{{ currentUser?.username || '-' }}</strong></div>
            <div class="mini-item"><span>真实姓名</span><strong>{{ currentUser?.realName || '未设置' }}</strong></div>
            <div class="mini-item"><span>昵称</span><strong>{{ currentUser?.nickname || '未设置' }}</strong></div>
            <div class="mini-item"><span>邮箱</span><strong>{{ currentUser?.email || '未设置' }}</strong></div>
            <div class="mini-item"><span>手机号</span><strong>{{ currentUser?.phone || '未设置' }}</strong></div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h3>下一步入口</h3>
              <p>把当前账号此刻最合理的几个去处摆在一起，减少来回翻菜单的成本。</p>
            </div>
          </div>

          <div class="quick-action-grid">
            <RouterLink
              v-for="action in quickActions"
              :key="action.to"
              class="quick-action-card"
              :class="{ disabled: !action.enabled }"
              :to="action.enabled ? action.to : '/profile'"
            >
              <div class="quick-action-head">
                <strong>{{ action.label }}</strong>
                <span :class="['chip', action.enabled ? action.tone : 'chip-default']">
                  {{ action.enabled ? 'Available' : 'Locked' }}
                </span>
              </div>
              <p>{{ action.description }}</p>
            </RouterLink>
          </div>
        </section>
      </section>

      <section class="panel profile-capability-panel">
        <div class="panel-head">
          <div>
            <h3>当前账号能力边界</h3>
            <p>把登录身份、可见范围和可操作动作直接讲清楚，后面继续补 RBAC 和用户模块时会顺很多。</p>
          </div>
        </div>

        <div class="mini-list">
          <div class="mini-item"><span>1.</span><span>{{ roleSummary }}</span></div>
          <div class="mini-item"><span>2.</span><span>{{ ticketScopeSummary }}</span></div>
          <div class="mini-item"><span>3.</span><span>{{ knowledgeScopeSummary }}</span></div>
        </div>

        <div class="capability-grid">
          <div v-for="capability in capabilityCards" :key="capability.code" class="capability-card">
            <div class="capability-head">
              <strong>{{ capability.label }}</strong>
              <span :class="['chip', capability.enabled ? 'chip-green' : 'chip-default']">
                {{ capability.enabled ? 'Enabled' : 'Hidden' }}
              </span>
            </div>
            <p>{{ capability.description }}</p>
            <code>{{ capability.code }}</code>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { buildProfileCapabilitySummaries, CAPABILITY_PRESENTATION } from '../access-policy'
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopbar from '../components/layout/AppTopbar.vue'
import { authState } from '../auth'
import { ROLE_CODES } from '../auth-constants'
import {
  canAccessAiCenter,
  canAccessCapability,
  canManageKnowledgeArticles,
  canManageSystem,
  canOperateTickets,
  canViewAllTickets,
} from '../authz'
import { manageNav, workspaceNav } from '../mock/dashboard'
import { isDemoMode } from '../utils/runtimeMode'

const currentUser = computed(() => authState.user)
const roleList = computed(() => currentUser.value?.roles?.length ? currentUser.value.roles : [ROLE_CODES.USER])
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.realName || currentUser.value?.username || '当前用户')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const usernameLine = computed(() => {
  const username = currentUser.value?.username || 'guest'
  const email = currentUser.value?.email
  return email ? `@${username} · ${email}` : `@${username}`
})
const primaryRoleText = computed(() => roleList.value[0] || ROLE_CODES.USER)
const permissionCount = computed(() => currentUser.value?.permissions?.length || 0)
const capabilityCount = computed(() => currentUser.value?.capabilities?.length || 0)
const runtimeModeText = computed(() => isDemoMode() ? '演示模式' : '正常模式')
const modeChipClass = computed(() => isDemoMode() ? 'chip-orange' : 'chip-green')
const workspaceFocus = computed(() => {
  if (canManageSystem()) {
    return '系统治理'
  }
  if (canManageKnowledgeArticles()) {
    return '知识沉淀'
  }
  if (canOperateTickets()) {
    return '工单协作'
  }
  return '问题跟进'
})

const summaries = computed(() => buildProfileCapabilitySummaries({
  canOperateTickets: canOperateTickets(),
  canViewAllTickets: canViewAllTickets(),
  canManageKnowledge: canManageKnowledgeArticles(),
}))
const roleSummary = computed(() => summaries.value.roleSummary)
const ticketScopeSummary = computed(() => summaries.value.ticketScopeSummary)
const knowledgeScopeSummary = computed(() => summaries.value.knowledgeScopeSummary)
const capabilityCards = computed(() => CAPABILITY_PRESENTATION.map((capability) => ({
  ...capability,
  enabled: canAccessCapability(capability.code),
})))

const quickActions = computed(() => [
  {
    label: canViewAllTickets() ? '进入工单中心' : '查看我的工单',
    description: canViewAllTickets()
      ? '继续处理工作台范围内的待办、指派与状态流转。'
      : '优先回看与你相关的工单进展、评论和处理结果。',
    to: '/tickets',
    enabled: true,
    tone: 'chip-blue',
  },
  {
    label: canManageKnowledgeArticles() ? '新建知识文章' : '浏览知识库',
    description: canManageKnowledgeArticles()
      ? '从已解决工单继续沉淀草稿、编辑内容并完成发布。'
      : '回查已有知识沉淀结果，复用处理经验和排查线索。',
    to: canManageKnowledgeArticles() ? '/knowledge/articles/create' : '/knowledge/articles',
    enabled: true,
    tone: 'chip-green',
  },
  {
    label: 'AI 工作台',
    description: canAccessAiCenter()
      ? '查看摘要、推荐和回复建议等 AI 协作能力。'
      : '当前账号暂未开放 AI 工作台入口，后续可按角色继续扩展。',
    to: '/ai-center',
    enabled: canAccessAiCenter(),
    tone: 'chip-orange',
  },
  {
    label: '系统管理',
    description: canManageSystem()
      ? '继续查看角色、分类、通知模板和系统治理入口。'
      : '当前账号以协作和业务处理为主，系统治理入口保持隐藏。',
    to: '/settings',
    enabled: canManageSystem(),
    tone: 'chip-default',
  },
])
</script>

<style scoped>
.profile-hero {
  align-items: stretch;
}

.profile-hero-copy {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.profile-identity {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.profile-identity p {
  margin: 0.25rem 0 0;
  color: #64748b;
}

.profile-avatar {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 1rem;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #0f172a, #2563eb);
  color: #f8fafc;
  font-size: 1.4rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.profile-role-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

.quick-action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.quick-action-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 1rem;
  padding: 1rem;
  background: rgba(248, 250, 252, 0.88);
  color: inherit;
  text-decoration: none;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.quick-action-card:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.quick-action-card.disabled {
  opacity: 0.8;
}

.quick-action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.quick-action-card p {
  margin: 0.75rem 0 0;
  color: #475569;
}

.profile-capability-panel {
  margin-top: 1.5rem;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-top: 1.5rem;
}

.capability-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 1rem;
  padding: 1rem;
  background: rgba(248, 250, 252, 0.88);
}

.capability-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.capability-card p {
  margin: 0.75rem 0;
}

.capability-card code {
  font-size: 0.85rem;
  color: #475569;
}

@media (max-width: 960px) {
  .profile-grid,
  .quick-action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
