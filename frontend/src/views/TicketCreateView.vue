<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
    <div class="detail-page">
    <header class="detail-topbar">
      <RouterLink class="back-link" to="/tickets">返回工单中心</RouterLink>
      <div class="detail-topbar-actions">
        <el-button plain :disabled="submitting" @click="resetForm">清空表单</el-button>
      </div>
    </header>

    <div class="create-layout">
      <section class="panel create-main">
        <div class="panel-head">
          <div>
            <h3>新建工单</h3>
            <p>从这里正式进入“创建 - 分配 - 处理 - 留痕”的工单主线。</p>
          </div>
          <span class="chip chip-blue">Create Ticket</span>
        </div>

        <div class="state-box create-runtime-banner" :class="{ 'state-warning': isDemoMode() }">
          <strong>{{ runtimeHeadline }} · {{ runtimeModeText }}</strong>
          <p>{{ runtimeDataSourceMessage }}</p>
        </div>

        <form class="ticket-form create-form" @submit.prevent="submitTicket">
          <label class="field">
            <span>工单标题</span>
            <el-input
              v-model="form.title"
              class="create-input"
              maxlength="200"
              placeholder="例如：支付回调接口偶发超时"
            />
          </label>

          <div class="form-grid form-grid-3 create-select-row">
            <label class="field create-select-field">
              <span>工单类型</span>
              <el-select v-model="form.type" class="create-select" placeholder="请选择工单类型">
                <el-option
                  v-for="option in typeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>

            <label class="field create-select-field">
              <span>工单分类</span>
              <el-select v-model="form.categoryId" class="create-select" placeholder="请选择工单分类">
                <el-option
                  v-for="option in categoryOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>

            <label class="field create-select-field">
              <span>优先级</span>
              <el-select v-model="form.priority" class="create-select" placeholder="请选择优先级">
                <el-option
                  v-for="option in priorityOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>
          </div>

          <label class="field">
            <span>问题描述</span>
            <el-input
              v-model="form.content"
              class="create-input create-textarea"
              type="textarea"
              :rows="10"
              placeholder="尽量写清楚现象、影响范围、复现步骤、期望结果和补充线索"
            />
          </label>

          <div class="form-actions">
            <ErrorTraceNotice v-if="submitError" inline :message="submitError" :trace-id="submitErrorTraceId" />
            <el-button class="create-submit-button" type="primary" native-type="submit" :loading="submitting">
              {{ submitting ? '提交中...' : '提交工单' }}
            </el-button>
          </div>
        </form>
      </section>

      <aside class="create-side">
        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>填写建议</h3>
              <p>写得越具体，后续分配、排查和 AI 建议就越有依据。</p>
            </div>
          </div>
          <div class="mini-list">
            <div class="mini-item"><span>1.</span><span>标题尽量突出问题现象，不要只写“有问题”。</span></div>
            <div class="mini-item"><span>2.</span><span>描述里补充时间、影响范围、报错信息和复现步骤。</span></div>
            <div class="mini-item"><span>3.</span><span>优先级要和真实业务影响匹配，别把所有单都提成紧急。</span></div>
          </div>
        </article>

        <article class="panel panel-dark">
          <div class="panel-head panel-head-dark">
            <div>
              <h3>AI 预留位</h3>
              <p>后面这里可以接智能分类、相似工单推荐和知识文章联想。</p>
            </div>
          </div>
          <div class="ai-summary">
            当前先把核心工单链路走通，下一阶段这里就很适合接“提交前风险提示”和“相关文章推荐”。
          </div>
        </article>
      </aside>
    </div>
    </div>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { createTicket } from '../api/ticket'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import AppShell from '../components/layout/AppShell.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'
import { createLocalTicket } from '../mock/ticketWorkspace'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'
import { resolveTicketSubmissionFailure } from '../utils/ticketSubmission'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const submitError = ref('')
const submitErrorTraceId = ref('')
let submitRequestId = 0
const runtimeModeText = computed(() => getRuntimeModeText())
const runtimeHeadline = computed(() => getRuntimeModeHeadline())
const runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: false,
  subject: '新建工单',
  localOnlyLabel: '本地工单草稿',
}))

const typeOptions = [
  { value: 'INCIDENT', label: '故障事件' },
  { value: 'TASK', label: '处理任务' },
  { value: 'QUESTION', label: '问题咨询' },
]

const categoryOptions = [
  { value: 1, label: '系统故障' },
  { value: 2, label: '处理任务' },
  { value: 3, label: '业务咨询' },
]

const priorityOptions = [
  { value: 1, label: 'P4 · 低优先级' },
  { value: 2, label: 'P3 · 普通' },
  { value: 3, label: 'P2 · 较高' },
  { value: 4, label: 'P1 · 紧急' },
]

const form = reactive({
  title: '',
  type: 'TASK',
  categoryId: 3,
  priority: 2,
  content: '',
})

function resetForm() {
  if (submitting.value) {
    return
  }
  form.title = ''
  form.type = 'TASK'
  form.categoryId = 3
  form.priority = 2
  form.content = ''
  submitError.value = ''
  submitErrorTraceId.value = ''
}

async function submitTicket() {
  if (submitting.value) {
    return
  }
  if (!form.title.trim()) {
    submitError.value = '请先填写工单标题'
    submitErrorTraceId.value = ''
    return
  }
  if (!form.content.trim()) {
    submitError.value = '请先填写问题描述'
    submitErrorTraceId.value = ''
    return
  }

  const requestId = ++submitRequestId
  submitting.value = true
  submitError.value = ''
  submitErrorTraceId.value = ''

  try {
    const data = await createTicket({
      title: form.title.trim(),
      type: form.type,
      categoryId: form.categoryId,
      priority: form.priority,
      content: form.content.trim(),
    })
    if (requestId !== submitRequestId || route.path !== '/tickets/create') {
      return
    }
    router.replace(`/tickets/${data.id}?created=1`)
  } catch (error) {
    const result = resolveTicketSubmissionFailure(error)
    if (requestId !== submitRequestId || route.path !== '/tickets/create') {
      return
    }
    if (result.mode === 'show-error') {
      submitError.value = result.message
      submitErrorTraceId.value = result.traceId
      return
    }

    const localTicket = createLocalTicket({
      title: form.title.trim(),
      type: form.type,
      categoryId: form.categoryId,
      priority: form.priority,
      content: form.content.trim(),
    })
    router.replace(`/tickets/${localTicket.id}?localCreated=1`)
  } finally {
    if (requestId === submitRequestId && route.path === '/tickets/create') {
      submitting.value = false
    }
  }
}
</script>

<style scoped>
.create-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(300px, 0.85fr);
  gap: 1.5rem;
}

.create-side {
  display: grid;
  gap: 1.25rem;
  align-self: start;
}

.ticket-form {
  margin-top: 1.25rem;
  padding: 1rem;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.78);
}

.create-form {
  padding: 1rem 1.1rem 1.1rem;
}

.create-select-row {
  align-items: end;
  gap: var(--space-4);
  margin-top: 0.9rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.78);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.875rem;
}

.form-grid-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-top: 0.85rem;
  color: var(--gray-600);
  font-size: 0.92rem;
}

.field:first-child {
  margin-top: 0;
}

.field span {
  font-size: 0.86rem;
  font-weight: 600;
  color: var(--gray-700);
}

.create-select-field {
  margin-top: 0;
}

.create-input,
.create-select {
  width: 100%;
}

.create-input :deep(.el-input__wrapper) {
  min-height: 2.75rem;
  padding-inline: 0.85rem;
  border-radius: var(--radius-sm);
  background: var(--bg-panel-solid)fff;
  box-shadow: none;
}

.create-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.create-input :deep(.el-input__inner),
.create-input :deep(.el-textarea__inner) {
  font-size: 0.92rem;
  color: var(--text-primary);
}

.create-textarea :deep(.el-textarea__inner) {
  min-height: 240px;
  padding: 0.9rem 1rem;
  border-radius: 12px;
  background: var(--bg-panel-solid)fff;
  box-shadow: none;
  line-height: 1.7;
}

.create-select :deep(.el-select__wrapper) {
  min-height: 2.55rem;
  padding-inline: 0.8rem 2.2rem;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: var(--bg-panel-solid)fff;
  box-shadow: none;
}

.create-select :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(37, 99, 235, 0.35);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.create-select :deep(.el-select__placeholder),
.create-select :deep(.el-select__selected-item) {
  color: var(--text-primary);
  font-size: 0.9rem;
  line-height: 1.3;
}

.create-select :deep(.el-select__caret) {
  color: var(--text-secondary);
  font-size: 14px;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: 1rem;
}

.create-submit-button {
  min-width: 124px;
}

@media (max-width: 1100px) {
  .create-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .form-grid,
  .form-grid-3 {
    grid-template-columns: 1fr;
  }

  .create-select-row {
    padding: 0;
    border: 0;
    background: transparent;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .create-submit-button {
    width: 100%;
  }
}
</style>
