<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import dayjs from "dayjs";
import { useRoute, useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import {
  addTicketComment,
  getTicketDetail,
  getTicketAssignees,
  updateTicketStatus,
  assignTicket,
  createTicketKnowledgeDraft,
  type TicketDetail,
  type TicketAssigneeOption
} from "@/api/tickets";
import {
  getReplyDraft,
  adoptReplyDraft,
  unadoptReplyDraft,
  type AiReplyDraft
} from "@/api/ai";
import { getTicketPriorityLabel, getTicketStatusLabel } from "@/constants/tickets";

defineOptions({ name: "DocflowTicketDetail" });

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const submittingComment = ref(false);
const commentText = ref("");
const ticket = ref<TicketDetail | null>(null);
const assignees = ref<TicketAssigneeOption[]>([]);
const aiDraft = ref<AiReplyDraft | null>(null);
const aiDraftLoading = ref(false);
const aiDraftExpanded = ref(false);
const assignDialogVisible = ref(false);
const selectedAssignee = ref<number | null>(null);
const actionLoading = ref<string | null>(null);
const draftLoading = ref(false);
const ticketId = computed(() => Number(route.params.id));

interface StatusAction { label: string; status: number; icon: string; type: "primary" | "success" | "warning" | "danger" | "info"; desc: string }

const availableActions = computed<StatusAction[]>(() => {
  const s = ticket.value?.status;
  if (!s) return [];
  const map: Record<number, StatusAction[]> = {
    1: [
      { label: "开始处理", status: 2, icon: "ep:video-play", type: "primary", desc: "标记为处理中" },
      { label: "关闭工单", status: 4, icon: "ep:switch", type: "info", desc: "直接关闭" }
    ],
    2: [
      { label: "标记解决", status: 3, icon: "ep:circle-check", type: "success", desc: "问题已解决" },
      { label: "关闭工单", status: 4, icon: "ep:switch", type: "info", desc: "关闭工单" }
    ],
    3: [
      { label: "确认关闭", status: 4, icon: "ep:switch", type: "info", desc: "确认关闭" },
      { label: "重新打开", status: 1, icon: "ep:refresh-left", type: "warning", desc: "问题未解决" }
    ],
    4: [
      { label: "重新打开", status: 1, icon: "ep:refresh-left", type: "warning", desc: "重新激活" }
    ]
  };
  return map[s] ?? [];
});

function statusTagType(status?: number | null) {
  if (status === 3) return "success";
  if (status === 4) return "info";
  if (status === 2) return "warning";
  return "";
}

function priorityTagType(level?: number | null) {
  if (level === 4) return "danger";
  if (level === 3) return "warning";
  return "";
}

function confidenceColor(conf?: string): string {
  if (!conf) return "#6b7280";
  const pct = parseInt(conf);
  if (pct >= 75) return "#16a34a";
  if (pct >= 50) return "#d97706";
  return "#dc2626";
}

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function formatRelative(value?: string | null) {
  if (!value) return "";
  const d = dayjs(value);
  const now = dayjs();
  const diff = now.diff(d, "minute");
  if (diff < 1) return "刚刚";
  if (diff < 60) return `${diff} 分钟前`;
  const h = now.diff(d, "hour");
  if (h < 24) return `${h} 小时前`;
  return d.format("MM-DD HH:mm");
}

async function loadTicket() {
  if (!Number.isFinite(ticketId.value)) return;
  loading.value = true;
  try {
    const { code, message: errorMessage, data } = await getTicketDetail(ticketId.value);
    if (code !== 200) throw new Error(errorMessage || "加载失败");
    ticket.value = data;
    // Try loading AI draft
    loadAiDraft();
  } catch (error) {
    ticket.value = null;
    message(error instanceof Error ? error.message : "加载失败", { type: "error" });
  } finally { loading.value = false; }
}

async function loadAssignees() {
  try {
    const { code, data } = await getTicketAssignees();
    if (code === 200) assignees.value = data;
  } catch { /* silently fail */ }
}

async function loadAiDraft() {
  if (!ticketId.value) return;
  aiDraftLoading.value = true;
  try {
    const { code, data } = await getReplyDraft(ticketId.value);
    if (code === 200 && data) {
      aiDraft.value = data;
      aiDraftExpanded.value = !data.adopted;
    }
  } catch { aiDraft.value = null; }
  finally { aiDraftLoading.value = false; }
}

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    message("已复制到剪贴板", { type: "success" });
  });
}

function useAiReply() {
  if (aiDraft.value?.customerReply) {
    commentText.value = aiDraft.value.customerReply;
    message("已填入 AI 回复模板", { type: "success" });
  }
}

async function handleAiAdopt() {
  if (!ticketId.value) return;
  try {
    await adoptReplyDraft(ticketId.value);
    if (aiDraft.value) aiDraft.value.adopted = true;
    message("已采纳 AI 建议", { type: "success" });
  } catch { message("操作失败", { type: "error" }); }
}

async function handleAiUnadopt() {
  if (!ticketId.value) return;
  try {
    await unadoptReplyDraft(ticketId.value);
    if (aiDraft.value) aiDraft.value.adopted = false;
    message("已取消采纳", { type: "success" });
  } catch { message("操作失败", { type: "error" }); }
}

async function submitComment() {
  if (!ticket.value || !commentText.value.trim()) return;
  submittingComment.value = true;
  try {
    const { code, message: errorMessage, data } = await addTicketComment(ticket.value.id, {
      content: commentText.value.trim(), commentType: 1, internal: false
    });
    if (code !== 200) throw new Error(errorMessage || "提交失败");
    ticket.value = data;
    commentText.value = "";
    message("评论已发送", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "提交失败", { type: "error" });
  } finally { submittingComment.value = false; }
}

async function handleStatusChange(action: StatusAction) {
  if (!ticket.value) return;
  actionLoading.value = action.label;
  try {
    const { code, message: errorMessage, data } = await updateTicketStatus(ticket.value.id, {
      status: action.status, remark: action.desc
    });
    if (code !== 200) throw new Error(errorMessage || "操作失败");
    ticket.value = data;
    message(`工单已${action.label}`, { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "操作失败", { type: "error" });
  } finally { actionLoading.value = null; }
}

async function handleAssign() {
  if (!ticket.value || !selectedAssignee.value) return;
  actionLoading.value = "assign";
  try {
    const { code, message: errorMessage, data } = await assignTicket(ticket.value.id, {
      assigneeUserId: selectedAssignee.value
    });
    if (code !== 200) throw new Error(errorMessage || "指派失败");
    ticket.value = data;
    assignDialogVisible.value = false;
    message("指派成功", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "指派失败", { type: "error" });
  } finally { actionLoading.value = null; }
}

async function handleCreateDraft() {
  if (!ticket.value) return;
  draftLoading.value = true;
  try {
    const { code, message: errorMessage, data } = await createTicketKnowledgeDraft(ticket.value.id);
    if (code !== 200) throw new Error(errorMessage || "创建失败");
    message("知识草稿已创建", { type: "success" });
    router.push(`/knowledge/articles/${data.id}/edit`);
  } catch (error) {
    message(error instanceof Error ? error.message : "创建失败", { type: "error" });
  } finally { draftLoading.value = false; }
}

function openAssignDialog() {
  selectedAssignee.value = ticket.value?.assigneeUserId ?? null;
  assignDialogVisible.value = true;
}

function goBack() { router.back(); }
function goTo(path: string) { router.push(path); }

watch(() => route.params.id, () => loadTicket());
onMounted(() => { loadTicket(); loadAssignees(); });
</script>

<template>
  <div v-loading="loading">
    <div class="mb-4 flex items-center gap-3">
      <el-button text circle @click="goBack">
        <component :is="useRenderIcon('ep:arrow-left', { width: '20px', height: '20px' })" />
      </el-button>
      <span v-if="ticket" class="font-mono text-sm text-gray-400">{{ ticket.ticketNo }}</span>
    </div>

    <template v-if="ticket">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <!-- Main content -->
        <div class="lg:col-span-2 space-y-4">
          <el-card shadow="never">
            <h1 class="text-lg font-bold leading-snug">{{ ticket.title }}</h1>
            <div class="flex flex-wrap items-center gap-2 mt-3">
              <el-tag :type="statusTagType(ticket.status)" effect="dark" size="small">
                {{ getTicketStatusLabel(ticket.status) }}
              </el-tag>
              <el-tag :type="priorityTagType(ticket.priority)" effect="light" size="small">
                {{ getTicketPriorityLabel(ticket.priority) }}
              </el-tag>
              <span class="text-xs text-gray-400">
                {{ ticket.submitterName || "未知" }} 提交于 {{ formatDateTime(ticket.createTime) }}
              </span>
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:document', { width: '14px', height: '14px' })" />问题描述
              </span>
            </template>
            <div class="text-sm leading-relaxed whitespace-pre-wrap text-gray-700 dark:text-gray-300">
              {{ ticket.content }}
            </div>
          </el-card>

          <!-- AI Reply Draft (inline) -->
          <el-card v-if="aiDraft && aiDraftExpanded" shadow="never" class="ai-draft-card">
            <template #header>
              <div class="flex items-center justify-between">
                <span class="font-semibold text-sm flex items-center gap-1.5">
                  <component :is="useRenderIcon('ep:magic-stick', { width: '14px', height: '14px' })" class="text-purple-500" />
                  AI 建议回复
                  <el-tag size="small" :color="confidenceColor(aiDraft.confidence)" effect="dark" round class="ml-2">
                    {{ aiDraft.confidence }}
                  </el-tag>
                </span>
                <div class="flex items-center gap-1">
                  <el-button size="small" text @click="useAiReply">
                    <component :is="useRenderIcon('ep:bottom-left', { width: '14px', height: '14px' })" class="mr-1" />
                    填入回复框
                  </el-button>
                  <el-button size="small" text @click="copyText(aiDraft.customerReply)">
                    <component :is="useRenderIcon('ep:copy-document', { width: '14px', height: '14px' })" />
                  </el-button>
                  <el-button size="small" text @click="aiDraftExpanded = false">
                    <component :is="useRenderIcon('ep:close', { width: '14px', height: '14px' })" />
                  </el-button>
                </div>
              </div>
            </template>
            <div v-if="aiDraft.diagnosis" class="mb-3 p-3 bg-blue-50 dark:bg-blue-900/10 rounded-lg text-sm">
              <span class="font-medium text-blue-700 dark:text-blue-400">诊断：</span>
              {{ aiDraft.diagnosis }}
            </div>
            <div v-if="aiDraft.nextStep" class="mb-3 p-3 bg-green-50 dark:bg-green-900/10 rounded-lg text-sm">
              <span class="font-medium text-green-700 dark:text-green-400">建议：</span>
              {{ aiDraft.nextStep }}
            </div>
            <div v-if="aiDraft.customerReply" class="p-3 bg-gray-50 dark:bg-gray-800 rounded-lg text-sm leading-relaxed whitespace-pre-wrap text-gray-800 dark:text-gray-200">
              {{ aiDraft.customerReply }}
            </div>
            <div v-if="aiDraft.operatorNotes?.length" class="mt-3 p-3 bg-purple-50 dark:bg-purple-900/10 rounded-lg">
              <p class="text-xs font-medium text-purple-700 dark:text-purple-400 mb-1">操作提示</p>
              <ul class="space-y-0.5">
                <li v-for="(note, idx) in aiDraft.operatorNotes" :key="idx" class="text-xs text-gray-600 dark:text-gray-300">• {{ note }}</li>
              </ul>
            </div>
            <div class="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700 flex gap-2">
              <el-button v-if="!aiDraft.adopted" size="small" type="primary" @click="handleAiAdopt">
                <component :is="useRenderIcon('ep:check', { width: '14px', height: '14px' })" class="mr-1" />采纳
              </el-button>
              <el-button v-else size="small" type="warning" @click="handleAiUnadopt">取消采纳</el-button>
            </div>
          </el-card>
          <div v-else-if="aiDraft && !aiDraftExpanded" class="flex items-center gap-2 px-3 py-2 bg-purple-50 dark:bg-purple-900/10 rounded-lg cursor-pointer" @click="aiDraftExpanded = true">
            <component :is="useRenderIcon('ep:magic-stick', { width: '14px', height: '14px' })" class="text-purple-500" />
            <span class="text-sm text-purple-700 dark:text-purple-300">AI 已生成回复建议</span>
            <component :is="useRenderIcon('ep:arrow-down', { width: '14px', height: '14px' })" class="ml-auto text-purple-400" />
          </div>

          <el-card v-if="ticket.timeline?.length" shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:clock', { width: '14px', height: '14px' })" />处理记录
              </span>
            </template>
            <div class="relative pl-5 border-l-2 border-gray-100 dark:border-gray-700 space-y-4">
              <div v-for="item in ticket.timeline" :key="item.id" class="relative">
                <div class="absolute -left-[25px] top-1 w-2.5 h-2.5 rounded-full bg-blue-400 border-2 border-white dark:border-gray-800" />
                <div>
                  <p class="text-sm font-medium">{{ item.title }}</p>
                  <p v-if="item.desc" class="text-xs text-gray-500 mt-0.5">{{ item.desc }}</p>
                  <p class="text-xs text-gray-400 mt-0.5">
                    <span>{{ item.operatorName }}</span><span class="mx-1">·</span><span>{{ formatRelative(item.createTime) }}</span>
                  </p>
                </div>
              </div>
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:chat-line-round', { width: '14px', height: '14px' })" />
                评论 <span class="text-gray-400 font-normal">({{ ticket.comments?.length || 0 }})</span>
              </span>
            </template>
            <div v-if="ticket.comments?.length" class="space-y-3 mb-4">
              <div v-for="c in ticket.comments" :key="c.id" class="p-3 rounded-lg bg-gray-50 dark:bg-gray-800/50">
                <div class="flex items-center gap-2 mb-1">
                  <span class="text-sm font-medium">{{ c.authorName }}</span>
                  <span class="text-xs text-gray-400">{{ formatRelative(c.createTime) }}</span>
                  <el-tag v-if="c.commentTypeLabel" size="small" effect="plain" type="info" class="ml-auto">{{ c.commentTypeLabel }}</el-tag>
                </div>
                <p class="text-sm text-gray-600 dark:text-gray-300 whitespace-pre-wrap">{{ c.content }}</p>
              </div>
            </div>
            <div class="flex gap-2">
              <el-input v-model="commentText" type="textarea" :rows="3" placeholder="输入评论，Ctrl+Enter 发送..." class="flex-1" @keyup.enter.ctrl="submitComment" />
              <el-button type="primary" :loading="submittingComment" :disabled="!commentText.trim()" @click="submitComment">
                <component :is="useRenderIcon('ep:promotion', { width: '14px', height: '14px' })" class="mr-1" />发送
              </el-button>
            </div>
          </el-card>
        </div>

        <!-- Sidebar -->
        <div class="space-y-4">
          <el-card v-if="availableActions.length" shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:set-up', { width: '14px', height: '14px' })" />工单操作
              </span>
            </template>
            <div class="flex flex-col gap-2">
              <el-button v-for="action in availableActions" :key="action.status" :type="action.type"
                :loading="actionLoading === action.label" class="!justify-start !w-full"
                @click="handleStatusChange(action)">
                <component :is="useRenderIcon(action.icon, { width: '14px', height: '14px' })" class="mr-2 shrink-0" />
                <span>{{ action.label }}</span>
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:user', { width: '14px', height: '14px' })" />处理人
              </span>
            </template>
            <div class="flex items-center justify-between">
              <span class="text-sm font-medium truncate mr-2">{{ ticket.assigneeName || "待分配" }}</span>
              <el-button size="small" text type="primary" class="shrink-0" @click="openAssignDialog" :loading="actionLoading === 'assign'">
                <component :is="useRenderIcon('ep:edit', { width: '12px', height: '12px' })" class="mr-1" />
                {{ ticket.assigneeUserId ? '更换' : '指派' }}
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:collection', { width: '14px', height: '14px' })" />知识沉淀
              </span>
            </template>
            <p class="text-xs text-gray-500 mb-3 leading-relaxed">将工单处理经验沉淀为知识库文章。</p>
            <el-button type="primary" plain class="!w-full" :loading="draftLoading" @click="handleCreateDraft">
              <component :is="useRenderIcon('ep:document-add', { width: '14px', height: '14px' })" class="mr-1" />
              沉淀为知识草稿
            </el-button>
          </el-card>

          <el-card shadow="never">
            <template #header><span class="font-semibold text-sm">基本信息</span></template>
            <div class="space-y-2.5 text-sm">
              <div class="flex justify-between"><span class="text-gray-400">提交人</span><span class="text-gray-700 dark:text-gray-300">{{ ticket.submitterName || "-" }}</span></div>
              <div class="flex justify-between"><span class="text-gray-400">类型</span><span>{{ ticket.type || "-" }}</span></div>
              <div class="flex justify-between"><span class="text-gray-400">创建时间</span><span class="text-xs">{{ formatDateTime(ticket.createTime) }}</span></div>
              <div class="flex justify-between"><span class="text-gray-400">更新时间</span><span class="text-xs">{{ formatDateTime(ticket.updateTime) }}</span></div>
            </div>
          </el-card>

          <el-card v-if="ticket.sourceKnowledgeArticles?.length" shadow="never">
            <template #header><span class="font-semibold text-sm">已沉淀知识</span></template>
            <div class="space-y-2">
              <div v-for="a in ticket.sourceKnowledgeArticles" :key="a.id"
                class="p-2 -mx-2 rounded cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                @click="goTo(`/knowledge/articles/${a.id}`)">
                <p class="text-sm font-medium line-clamp-1">{{ a.title }}</p>
                <div class="flex items-center gap-2 mt-1">
                  <el-tag size="small" effect="plain" :type="a.status === 1 ? 'success' : 'info'">{{ a.statusLabel }}</el-tag>
                  <span class="text-xs text-gray-400">{{ formatDateTime(a.updateTime) }}</span>
                </div>
              </div>
            </div>
          </el-card>

          <el-card v-if="ticket.relatedArticles?.length" shadow="never">
            <template #header><span class="font-semibold text-sm">推荐知识</span></template>
            <div class="space-y-3">
              <div v-for="a in ticket.relatedArticles" :key="a.id"
                class="p-2 -mx-2 rounded cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                @click="goTo(`/knowledge/articles/${a.id}`)">
                <p class="text-sm font-medium">{{ a.title }}</p>
                <p class="text-xs text-gray-400 mt-1">{{ a.reason }}</p>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="工单不存在或无权访问" />

    <el-dialog v-model="assignDialogVisible" title="指派处理人" width="400px">
      <el-select v-model="selectedAssignee" placeholder="选择处理人" class="w-full" filterable>
        <el-option v-for="u in assignees" :key="u.id" :label="`${u.displayName} (@${u.username})`" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAssignee" :loading="actionLoading === 'assign'" @click="handleAssign">确认指派</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ai-draft-card :deep(.el-card__header) { padding: 12px 20px; }
.ai-draft-card :deep(.el-card__body) { padding: 16px 20px; }
</style>