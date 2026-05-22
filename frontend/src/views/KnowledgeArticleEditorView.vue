<template>
  <AppShell :workspace-nav="workspaceNav" :manage-nav="manageNav">
    <section class="knowledge-editor-page workspace-page">
      <el-card shadow="never" class="editor-header-card workspace-card">
        <div class="editor-header workspace-header">
          <div class="editor-header__main workspace-header-main">
            <div class="editor-header__title workspace-title">
              <h2>{{ isEditMode ? '继续完善知识文章' : '新建知识文章' }}</h2>
              <p>
                当前优先保持真实保存链路可用；当后端暂时不可用时，仍会自动回退到本地草稿，避免内容丢失。
              </p>
            </div>
            <div class="editor-header__actions workspace-actions">
              <el-button class="ghost-button" :disabled="submitting" @click="saveArticle(0)">保存草稿</el-button>
              <el-button class="primary-button" type="primary" :disabled="submitting" @click="saveArticle(1)">
                {{ submitting ? '保存中...' : '发布文章' }}
              </el-button>
            </div>
          </div>

          <div class="editor-stats workspace-stats">
            <div
              v-for="stat in statCards"
              :key="stat.label"
              class="editor-stat workspace-stat"
            >
              <span class="editor-stat__label workspace-stat-label">{{ stat.label }}</span>
              <strong>{{ stat.value }}</strong>
              <p>{{ stat.description }}</p>
            </div>
          </div>
        </div>
      </el-card>

      <section class="editor-layout">
        <el-card shadow="never" class="editor-main-card workspace-card">
          <div class="editor-main-card__head workspace-section-head workspace-section-head-spaced">
            <div class="workspace-section-title">
              <h3>{{ isEditMode ? '编辑内容' : '填写内容' }}</h3>
              <p>把标题、摘要、正文和分类收进同一套后台表单结构，便于后续继续扩展附件、富文本和协作字段。</p>
            </div>
          </div>

          <el-form class="editor-form workspace-filter-form" label-position="top" @submit.prevent="saveArticle(form.status)">
            <el-form-item label="文章标题" class="editor-form__item">
              <el-input
                v-model.trim="form.title"
                placeholder="例如：支付回调失败排查手册"
              />
            </el-form-item>

            <div class="editor-form__grid">
              <el-form-item label="文章分类" class="editor-form__item">
                <el-select v-model="form.categoryId" placeholder="请选择分类">
                  <el-option :value="1" label="系统使用指南" />
                  <el-option :value="2" label="故障排查" />
                  <el-option :value="3" label="支付与订单" />
                </el-select>
              </el-form-item>

              <el-form-item label="发布状态" class="editor-form__item">
                <el-select v-model="form.status" placeholder="请选择状态">
                  <el-option :value="0" label="草稿" />
                  <el-option :value="1" label="已发布" />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="文章摘要" class="editor-form__item">
              <el-input
                v-model.trim="form.summary"
                type="textarea"
                :rows="4"
                placeholder="用 2 到 3 句话说明这篇文章能解决什么问题"
              />
            </el-form-item>

            <el-form-item label="正文内容" class="editor-form__item">
              <el-input
                v-model.trim="form.content"
                class="editor-textarea"
                type="textarea"
                :rows="14"
                placeholder="建议写清楚问题现象、排查步骤、关键日志和最终结论。"
              />
            </el-form-item>
          </el-form>

          <ErrorTraceNotice v-if="feedbackMessage" :message="feedbackMessage" :trace-id="feedbackTraceId" />

          <div class="editor-progress">
            <div class="editor-progress-bar">
              <span class="editor-progress-fill" :style="{ width: `${completionRate}%` }"></span>
            </div>
            <div class="editor-progress-meta">
              <span>标题、摘要、正文和分类越完整，预览效果越接近真实知识后台。</span>
              <strong>{{ completionRate }}%</strong>
            </div>
          </div>
        </el-card>

        <aside class="editor-side">
          <article class="panel panel-dark">
            <div class="panel-head panel-head-dark">
              <div>
                <h3>实时预览</h3>
                <p>一边写，一边确认信息结构是否足够清晰。</p>
              </div>
            </div>
            <div class="editor-preview">
              <div v-if="sourceTicket" class="editor-source-banner">
                <strong>{{ sourceTicketBannerTitle }}</strong>
                <p>来源工单：{{ sourceTicket.ticketNo || `#${sourceTicket.id}` }} · {{ sourceTicket.title || '待补充标题' }}</p>
              </div>
              <span class="chip chip-blue">{{ categoryLabel }}</span>
              <h3>{{ form.title || '未命名文章' }}</h3>
              <p>{{ form.summary || '摘要会显示在这里，建议尽量写成一眼就能理解价值的一段描述。' }}</p>
              <div class="mini-list dark-list">
                <div class="mini-item"><span>状态</span><span>{{ form.status === 1 ? '已发布' : '草稿' }}</span></div>
                <div class="mini-item"><span>作者</span><span>{{ authorName }}</span></div>
                <div class="mini-item"><span>字数</span><span>{{ contentLength }}</span></div>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <div>
                <h3>来源上下文</h3>
                <p>先把文章的业务来路交代清楚，后面的人才知道它为什么值得复用。</p>
              </div>
            </div>
            <div v-if="sourceTicket" class="mini-list">
              <div class="mini-item"><span>来源工单</span><span>{{ sourceTicket.ticketNo || `#${sourceTicket.id}` }}</span></div>
              <div class="mini-item"><span>工单标题</span><span>{{ sourceTicket.title || '待补充标题' }}</span></div>
              <div class="mini-item"><span>沉淀入口</span><span>{{ route.query.from === 'ticket-close' ? '关单后沉淀' : '处理中沉淀' }}</span></div>
            </div>
            <div v-else class="mini-list">
              <div class="mini-item"><span>当前没有绑定来源工单，更适合编写通用操作指南或平台说明。</span></div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <div>
                <h3>写作建议</h3>
                <p>让内容更像团队资产，而不是个人备忘录。</p>
              </div>
            </div>
            <div class="mini-list">
              <div class="mini-item"><span>先写问题现象，再写排查步骤。</span></div>
              <div class="mini-item"><span>摘要里尽量说明适用场景和收益。</span></div>
              <div class="mini-item"><span>正文中最好保留关键日志和定位结论。</span></div>
            </div>
          </article>

          <article class="panel">
            <div class="panel-head">
              <div>
                <h3>发布检查</h3>
                <p>提交前先看一眼，避免文章能发但不好用。</p>
              </div>
            </div>
            <div class="mini-list">
              <div class="mini-item"><span>标题</span><span>{{ form.title ? '已完成' : '待补充' }}</span></div>
              <div class="mini-item"><span>摘要</span><span>{{ form.summary ? '已完成' : '待补充' }}</span></div>
              <div class="mini-item"><span>正文</span><span>{{ contentLength >= 60 ? '已完成' : '偏短' }}</span></div>
              <div class="mini-item"><span>分类</span><span>{{ categoryLabel }}</span></div>
            </div>
            <div class="publish-checklist">
              <label
                v-for="item in publishChecklist"
                :key="item.label"
                class="publish-check-item"
                :class="{ 'publish-check-item-done': item.done }"
              >
                <span class="publish-check-dot"></span>
                <div>
                  <strong>{{ item.label }}</strong>
                  <p>{{ item.tip }}</p>
                </div>
              </label>
            </div>
            <div class="state-box publish-score-box">
              <span>发布就绪度</span>
              <strong>{{ publishReadiness }}%</strong>
            </div>
          </article>
        </aside>
      </section>
    </section>
  </AppShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/layout/AppShell.vue'
import ErrorTraceNotice from '../components/common/ErrorTraceNotice.vue'
import { authState } from '../auth'
import { canManageKnowledgeArticles } from '../authz'
import { createKnowledgeArticle, fetchKnowledgeArticleDetail, updateKnowledgeArticle } from '../api/knowledge'
import { manageNav, workspaceNav } from '../mock/dashboard'
import { getKnowledgeDraft, removeKnowledgeDraft, saveKnowledgeDraft } from '../mock/knowledgeDrafts'
import { resolveKnowledgeEditorLoadFailure, resolveKnowledgeEditorSaveFailure } from '../utils/knowledgeEditor'
import { consumeKnowledgeDraftSeed } from '../utils/knowledgeFromTicket'
import type { KnowledgeSourceTicketLink } from '../utils/knowledgeSourceTicket'
import { attachArticleSourceTicket, saveArticleSourceTicket } from '../utils/knowledgeSourceTicket'
import { getRuntimeDataSourceMessage, getRuntimeModeHeadline, getRuntimeModeText, isDemoMode } from '../utils/runtimeMode'

const route = useRoute()
const router = useRouter()

const form = reactive({
  id: 0,
  title: '',
  summary: '',
  content: '',
  categoryId: 2,
  status: 0,
})

const feedbackMessage = ref('')
const feedbackTraceId = ref('')
const submitting = ref(false)
const editingLocalDraft = ref(false)
let editorLoadRequestId = 0
let editorSaveRequestId = 0
const isEditMode = computed(() => route.path.includes('/edit'))
const sourceTicket = ref<KnowledgeSourceTicketLink | null>(null)
const _runtimeModeText = computed(() => getRuntimeModeText())
const _runtimeHeadline = computed(() => getRuntimeModeHeadline())
const _runtimeDataSourceMessage = computed(() => getRuntimeDataSourceMessage({
  usedFallbackData: false,
  subject: isEditMode.value ? '知识编辑' : '新建知识文章',
  localOnlyLabel: '本地知识草稿',
}))
const contentLength = computed(() => form.content.replace(/\s/g, '').length)
const sourceTicketBannerTitle = computed(() =>
  route.query.from === 'ticket-close' ? '已根据关单工单生成沉淀草稿' : '这篇文章来自工单沉淀',
)
const completionRate = computed(() => {
  let score = 0
  if (form.title.trim()) score += 25
  if (form.summary.trim()) score += 25
  if (contentLength.value >= 60) score += 35
  if (form.categoryId) score += 15
  return score
})
const publishChecklist = computed(() => [
  {
    label: '问题现象写清楚',
    done: form.title.trim().length >= 8,
    tip: '标题最好能直接点出故障或业务场景，而不是只写系统问题。',
  },
  {
    label: '摘要能独立说明价值',
    done: form.summary.trim().length >= 24,
    tip: '别人不点开正文，也能先知道这篇文章解决什么问题。',
  },
  {
    label: '正文包含处理路径',
    done: contentLength.value >= 120,
    tip: '尽量写出排查步骤、关键日志、结论和预防动作。',
  },
  {
    label: '来源上下文完整',
    done: !sourceTicket.value || !!sourceTicket.value.title,
    tip: '如果来自工单，最好保留工单编号和标题，方便后续回查。',
  },
])
const publishReadiness = computed(() => {
  const doneCount = publishChecklist.value.filter((item) => item.done).length
  return Math.round((doneCount / publishChecklist.value.length) * 100)
})
const authorName = computed(() => authState.user?.nickname || authState.user?.realName || authState.user?.username || '当前用户')
const categoryLabel = computed(() => {
  if (form.categoryId === 1) return '系统使用指南'
  if (form.categoryId === 2) return '故障排查'
  return '支付与订单'
})
const draftLabel = computed(() => (isEditMode.value ? '编辑中' : '创建中'))
const statCards = computed(() => [
  {
    label: '编辑状态',
    value: form.status === 1 ? '已发布' : '草稿中',
    description: draftLabel.value,
  },
  {
    label: '内容字数',
    value: contentLength.value,
    description: '正文当前长度',
  },
  {
    label: '摘要状态',
    value: form.summary ? '已填写' : '待完善',
    description: '影响列表与详情预览',
  },
  {
    label: '完成度',
    value: `${completionRate.value}%`,
    description: '基于标题、摘要、正文和分类估算',
  },
])

function resetEditorState() {
  editorSaveRequestId += 1
  editingLocalDraft.value = false
  submitting.value = false
  form.id = 0
  form.title = ''
  form.summary = ''
  form.content = ''
  form.categoryId = 2
  form.status = 0
  sourceTicket.value = null
  feedbackMessage.value = ''
  feedbackTraceId.value = ''
}

async function loadInitialArticle() {
  const requestId = ++editorLoadRequestId
  resetEditorState()
  if (!canManageKnowledgeArticles()) {
    feedbackMessage.value = '当前账号只有阅读权限，不能新建或编辑知识文章。'
    feedbackTraceId.value = ''
    return
  }

  const id = Number(route.params.id)
  if (!id) {
    if (isEditMode.value) {
      feedbackMessage.value = '文章 ID 不合法，无法进入编辑模式。'
      feedbackTraceId.value = ''
      return
    }
    applyTicketSeed()
    return
  }

  const localDraft = getKnowledgeDraft(id)
  if (localDraft) {
    if (requestId !== editorLoadRequestId) {
      return
    }
    if (route.query.from === 'ticket' || route.query.from === 'ticket-close') {
      consumeKnowledgeDraftSeed()
    }
    editingLocalDraft.value = true
    syncForm(localDraft)
    sourceTicket.value = localDraft.sourceTicket || null
    return
  }

  try {
    const detail = await fetchKnowledgeArticleDetail(id)
    if (requestId !== editorLoadRequestId) {
      return
    }
    const article = attachArticleSourceTicket(detail)
    if (route.query.from === 'ticket' || route.query.from === 'ticket-close') {
      consumeKnowledgeDraftSeed()
    }
    editingLocalDraft.value = false
    syncForm(article)
    sourceTicket.value = article.sourceTicket || null
  } catch (error) {
    if (requestId !== editorLoadRequestId) {
      return
    }
    console.error(error)
    const result = resolveKnowledgeEditorLoadFailure(error)
    if (result.mode === 'fallback-local') {
      applyTicketSeed()
      if (sourceTicket.value) {
        return
      }
    }
    feedbackMessage.value = result.message
    feedbackTraceId.value = result.traceId
  }
}

function applyTicketSeed() {
  const seed = consumeKnowledgeDraftSeed()
  if (!seed) {
    return
  }

  editingLocalDraft.value = false
  form.id = 0
  form.title = seed.title
  form.summary = seed.summary
  form.content = seed.content
  form.categoryId = seed.categoryId
  form.status = 0
  sourceTicket.value = {
    id: seed.sourceTicketId,
    ticketNo: seed.sourceTicketNo,
    title: seed.sourceTicketTitle,
  }
  feedbackMessage.value = seed.origin === 'ticket-close'
    ? `工单 ${seed.sourceTicketNo || `#${seed.sourceTicketId}`} 已关闭，已经帮你带出沉淀草稿${seed.closeRemark ? '和关闭备注' : ''}。`
    : `已从工单 ${seed.sourceTicketNo || `#${seed.sourceTicketId}`} 生成知识文章草稿，请补充结论后再发布。`
  feedbackTraceId.value = ''
}

function syncForm(article: {
  id: number
  title: string
  summary: string
  content: string
  categoryId: number | null
  status: number
}) {
  form.id = article.id
  form.title = article.title
  form.summary = article.summary
  form.content = article.content
  form.categoryId = article.categoryId || 2
  form.status = article.status
}

function buildNormalizedArticleDraft(status: number) {
  return {
    title: form.title.trim(),
    summary: form.summary.trim(),
    content: form.content.trim(),
    categoryId: form.categoryId,
    sourceTicketId: sourceTicket.value?.id ?? null,
    status,
  }
}

async function saveArticle(status: number) {
  if (submitting.value) {
    return
  }
  if (!canManageKnowledgeArticles()) {
    feedbackMessage.value = '当前账号只有阅读权限，不能保存知识文章。'
    feedbackTraceId.value = ''
    return
  }

  if (!form.title || !form.content) {
    feedbackMessage.value = '标题和正文至少要先写一点内容。'
    feedbackTraceId.value = ''
    return
  }

  submitting.value = true
  const requestId = ++editorSaveRequestId
  feedbackMessage.value = ''
  feedbackTraceId.value = ''

  try {
    const payload = buildNormalizedArticleDraft(status)
    if (!isDemoMode()) {
      const previousId = form.id

      const article = !editingLocalDraft.value && form.id
        ? await updateKnowledgeArticle(form.id, payload)
        : await createKnowledgeArticle(payload)
      if (requestId !== editorSaveRequestId) {
        return
      }

      if (editingLocalDraft.value && previousId) {
        removeKnowledgeDraft(previousId)
        saveArticleSourceTicket(previousId, null)
      }
      editingLocalDraft.value = false
      form.id = article.id
      saveArticleSourceTicket(article.id, sourceTicket.value)
      feedbackMessage.value = status === 1 ? '文章已发布到真实知识库。' : '草稿已保存到真实知识库。'
      feedbackTraceId.value = ''
      router.push(`/knowledge/articles/${article.id}`)
      return
    }
  } catch (error) {
    console.error(error)
    const result = resolveKnowledgeEditorSaveFailure(error, status)
    if (requestId !== editorSaveRequestId) {
      return
    }
    if (result.mode === 'show-error') {
      feedbackMessage.value = result.message
      feedbackTraceId.value = result.traceId
      return
    }
    feedbackMessage.value = result.message
    feedbackTraceId.value = ''
  } finally {
    if (requestId === editorSaveRequestId) {
      submitting.value = false
    }
  }

  if (requestId !== editorSaveRequestId) {
    return
  }
  const publishTime = status === 1 ? new Date().toISOString().slice(0, 19) : null
  const localPayload = buildNormalizedArticleDraft(status)
  const draft = saveKnowledgeDraft({
    id: form.id,
    title: localPayload.title,
    summary: localPayload.summary,
    content: localPayload.content,
    categoryId: localPayload.categoryId,
    authorUserId: authState.user?.id || 1,
    status,
    publishTime,
    sourceTicket: sourceTicket.value || undefined,
  })

  feedbackMessage.value = status === 1 ? '后端不可用，已回退为本地发布预览。' : '后端不可用，草稿已保存到本地。'
  feedbackTraceId.value = ''
  router.push(`/knowledge/articles/${draft.id}`)
}

onMounted(() => {
  loadInitialArticle()
})

watch(
  () => [route.path, route.params.id, JSON.stringify(route.query), route.fullPath],
  () => {
    loadInitialArticle()
  },
)
</script>

<style scoped>
.editor-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.95fr);
  gap: var(--space-4);
  align-items: start;
}




.editor-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.editor-form__item {
  margin-bottom: 0;
}

.editor-side {
  display: grid;
  gap: var(--space-4);
}

.editor-progress {
  display: grid;
  gap: var(--space-3);
  margin-top: 1rem;
}

.editor-progress-bar {
  height: 10px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.editor-progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #0ea5e9);
  transition: width 180ms ease;
}

.editor-progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  color: var(--text-secondary);
}

.editor-source-banner {
  margin-bottom: 0.9rem;
  padding: 0.85rem 0.95rem;
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.12);
}

.editor-source-banner strong,
.editor-source-banner p {
  margin: 0;
}

.editor-source-banner p {
  margin-top: 0.35rem;
  color: rgba(255, 255, 255, 0.88);
}

.publish-checklist {
  display: grid;
  gap: var(--space-3);
  margin-top: 1rem;
}

.publish-check-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: var(--space-3);
  align-items: start;
  padding: 0.85rem 0.95rem;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.82);
}

.publish-check-item strong,
.publish-check-item p {
  margin: 0;
}

.publish-check-item p {
  margin-top: 0.25rem;
  color: var(--text-secondary);
}

.publish-check-item-done {
  border-color: rgba(34, 197, 94, 0.2);
  background: rgba(240, 253, 244, 0.86);
}

.publish-check-dot {
  width: 12px;
  height: 12px;
  margin-top: 0.25rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.5);
}

.publish-check-item-done .publish-check-dot {
  background: #22c55e;
}

.publish-score-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 1rem;
}

@media (max-width: 1280px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .editor-form__grid,
  .editor-progress-meta {
    grid-template-columns: 1fr;
    display: grid;
  }

  .editor-progress-meta {
    gap: var(--space-2);
  }
}
</style>
