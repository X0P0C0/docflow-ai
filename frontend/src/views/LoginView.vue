<template>
  <div class="login-page">
    <section class="login-hero">
      <span class="hero-tag">DocFlow AI Access</span>
      <h1>登录后继续处理知识库与工单协同工作。</h1>
      <p>
        这个入口同时支持真实后端登录和后端不可用时的演示体验。现在页面会明确提示当前运行模式，避免演示时把本地数据和真实数据混在一起看。
      </p>
      <section class="login-runtime-panel" :class="runtimePanelClass">
        <div class="login-runtime-copy">
          <strong>{{ runtimeHeadline }}</strong>
          <p>{{ runtimeDescription }}</p>
        </div>
        <span class="login-runtime-badge">{{ runtimeModeText }}</span>
      </section>
      <div class="login-demo-card">
        <strong>演示账号</strong>
        <div class="login-demo-grid">
          <button type="button" class="login-demo-account" :disabled="submitting" @click="fillDemoAccount('admin', 'password')">
            <span>管理员</span>
            <p>admin / password</p>
          </button>
          <button type="button" class="login-demo-account" :disabled="submitting" @click="fillDemoAccount('support01', 'password')">
            <span>技术支持</span>
            <p>support01 / password</p>
          </button>
          <button type="button" class="login-demo-account" :disabled="submitting" @click="fillDemoAccount('user01', 'password')">
            <span>普通用户</span>
            <p>user01 / password</p>
          </button>
        </div>
        <small>如果本地后端临时不可用，以上账号会自动进入演示模式，方便直接预览页面。</small>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-panel-head">
        <span class="chip chip-blue">Sign In</span>
        <h2>欢迎回来</h2>
        <p>输入已有测试账号，继续进入 DocFlow AI 工作台。</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <label class="login-field">
          <span>用户名</span>
          <input v-model.trim="form.username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </label>

        <label class="login-field">
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </label>

        <ErrorTraceNotice v-if="errorMessage" :message="errorMessage" :trace-id="errorTraceId" />
        <div class="state-box">
          当前登录页支持两种路径：后端可用时走真实登录，后端不可用时自动切到演示模式。
        </div>

        <button class="primary-button login-submit" type="submit" :disabled="submitting">
          {{ submitting ? '登录中...' : '进入工作台' }}
        </button>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDemoSession, login } from '../api/auth'
import { getApiErrorMessage, getApiErrorTraceId, isNetworkFallbackCandidate } from '../api/http'
import { saveSession } from '../auth'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import { getLoginAuthNotice } from '../utils/loginAuthNotice'
import { getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

const route = useRoute()
const router = useRouter()

const form = reactive({
  username: 'admin',
  password: 'password',
})

const submitting = ref(false)
const errorMessage = ref('')
const errorTraceId = ref('')
const showingRouteAuthNotice = ref(false)
const runtimeModeText = computed(() => getRuntimeModeText())
const runtimePanelClass = computed(() => (isDemoMode() ? 'login-runtime-panel-demo' : 'login-runtime-panel-live'))
const runtimeHeadline = computed(() => (isDemoMode() ? '当前默认会进入演示模式' : '当前默认会进入正常模式'))
const runtimeDescription = computed(() => (
  isDemoMode()
    ? '说明当前还没有连到真实后端，登录成功后会先使用本地演示数据来预览页面和流程。'
    : '说明当前已经拿到了真实登录态，后续进入工作台会优先读取并写入后端数据。'
))

function resolveRedirectTarget() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/dashboard'
}

function syncRouteAuthReason() {
  const authNotice = getLoginAuthNotice(route.query.reason)
  if (authNotice) {
    errorMessage.value = authNotice
    errorTraceId.value = ''
    showingRouteAuthNotice.value = true
    return
  }
  if (showingRouteAuthNotice.value) {
    errorMessage.value = ''
    errorTraceId.value = ''
    showingRouteAuthNotice.value = false
  }
}

async function handleSubmit() {
  if (submitting.value) {
    return
  }
  if (!form.username || !form.password) {
    errorMessage.value = '请输入用户名和密码。'
    errorTraceId.value = ''
    return
  }

  submitting.value = true
  errorMessage.value = ''
  errorTraceId.value = ''
  showingRouteAuthNotice.value = false

  try {
    const result = await login(form)
    saveSession(result)

    await router.replace(resolveRedirectTarget())
  } catch (error) {
    const demoSession = createDemoSession(form.username, form.password)
    if (demoSession && isNetworkFallbackCandidate(error)) {
      saveSession(demoSession)
      await router.replace(resolveRedirectTarget())
      return
    }

    errorMessage.value = getApiErrorMessage(error, '登录失败，请稍后重试。')
    errorTraceId.value = getApiErrorTraceId(error)
  } finally {
    submitting.value = false
  }
}

function fillDemoAccount(username: string, password: string) {
  if (submitting.value) {
    return
  }
  form.username = username
  form.password = password
  errorMessage.value = ''
  errorTraceId.value = ''
  showingRouteAuthNotice.value = false
}

watch(
  () => route.query.reason,
  () => {
    syncRouteAuthReason()
  },
  { immediate: true },
)
</script>

<style scoped>
.login-runtime-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.login-runtime-copy strong,
.login-runtime-copy p {
  display: block;
  margin: 0;
}

.login-runtime-copy p {
  margin-top: 0.45rem;
}

.login-runtime-badge {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  padding: 0.65rem 0.9rem;
  border-radius: 999px;
  font-weight: 700;
}

.login-runtime-panel-demo {
  background: rgba(251, 146, 60, 0.14);
}

.login-runtime-panel-demo .login-runtime-badge {
  background: rgba(255, 255, 255, 0.16);
  color: #fed7aa;
}

.login-runtime-panel-live {
  background: rgba(34, 197, 94, 0.12);
}

.login-runtime-panel-live .login-runtime-badge {
  background: rgba(255, 255, 255, 0.16);
  color: #bbf7d0;
}

.login-demo-account {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 0.85rem;
  text-align: left;
  color: inherit;
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease, border-color 180ms ease;
}

.login-demo-account:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.22);
}

.login-demo-account span,
.login-demo-account p {
  display: block;
  margin: 0;
}

.login-demo-account p {
  margin-top: 0.35rem;
}

@media (max-width: 680px) {
  .login-runtime-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .login-runtime-badge {
    min-width: 0;
  }
}
</style>
