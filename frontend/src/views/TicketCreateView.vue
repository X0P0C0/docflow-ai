<template>
  <div class="detail-page">
    <header class="detail-topbar">
      <RouterLink class="back-link" to="/tickets">返回工单中心</RouterLink>
      <div class="detail-topbar-actions">
        <button class="ghost-button" type="button" :disabled="submitting" @click="resetForm">清空表单</button>
      </div>
    </header>

    <div class="create-layout">
      <section class="panel create-main">
        <div class="panel-head">
          <div>
            <h3>新建工单</h3>
            <p>从这里正式进入“创建 -> 分配 -> 处理 -> 留痕”的工单主线。</p>
          </div>
          <span class="chip chip-blue">Create Ticket</span>
        </div>

        <form class="ticket-form create-form" @submit.prevent="submitTicket">
          <label class="field">
            <span>工单标题</span>
            <input
              v-model="form.title"
              class="field-control"
              type="text"
              maxlength="200"
              placeholder="例如：支付回调接口偶发超时"
            />
          </label>

          <div class="form-grid form-grid-3">
            <label class="field">
              <span>工单类型</span>
              <select v-model="form.type" class="field-control">
                <option v-for="option in typeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>工单分类</span>
              <select v-model="form.categoryId" class="field-control">
                <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>

            <label class="field">
              <span>优先级</span>
              <select v-model="form.priority" class="field-control">
                <option v-for="option in priorityOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
          </div>

          <label class="field">
            <span>问题描述</span>
            <textarea
              v-model="form.content"
              class="field-control field-textarea create-textarea"
              rows="10"
              placeholder="请尽量写清楚现象、影响范围、复现步骤、期望结果和补充线索"
            />
          </label>

          <div class="form-actions">
            <ErrorTraceNotice v-if="submitError" inline :message="submitError" :trace-id="submitErrorTraceId" />
            <button class="primary-button" type="submit" :disabled="submitting">
              {{ submitting ? '提交中...' : '提交工单' }}
            </button>
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
            <div class="mini-item"><span>1.</span><span>标题尽量突出问题现象，不要只写“有问题”</span></div>
            <div class="mini-item"><span>2.</span><span>描述里补充时间、影响范围、报错信息和复现步骤</span></div>
            <div class="mini-item"><span>3.</span><span>优先级要和真实业务影响匹配，别把所有单都提成紧急</span></div>
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
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { createTicket } from '../api/ticket'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import { createLocalTicket } from '../mock/ticketWorkspace'
import { resolveTicketSubmissionFailure } from '../utils/ticketSubmission'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const submitError = ref('')
const submitErrorTraceId = ref('')
let submitRequestId = 0

const typeOptions = [
  { value: 'INCIDENT', label: '故障事件' },
  { value: 'TASK', label: '处理任务' },
  { value: 'QUESTION', label: '问题咨询' },
]

const categoryOptions = [
  { value: 1, label: '系统故障' },
  { value: 2, label: '账号权限' },
  { value: 3, label: '业务咨询' },
]

const priorityOptions = [
  { value: 1, label: 'P4 · 低优先级' },
  { value: 2, label: 'P3 · 普通' },
  { value: 3, label: 'P2 · 高优先级' },
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
  padding: 1.2rem;
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
  color: #475569;
  font-size: 0.92rem;
}

.field:first-child {
  margin-top: 0;
}

.field-control {
  width: 100%;
  padding: 0.8rem 0.95rem;
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  font: inherit;
}

.field-textarea {
  resize: vertical;
  min-height: 108px;
}

.create-textarea {
  min-height: 220px;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
}

.form-error {
  margin: 0;
  color: #b42318;
  font-size: 0.9rem;
}

@media (max-width: 1100px) {
  .create-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .form-grid,
  .form-grid-3 {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
