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
  transferTicket,
  escalateTicket,
  rateSatisfaction,
  mergeTicket,
  type TicketDetail,
  type TicketAssigneeOption
} from "@/api/tickets";
import {
  getAttachments,
  uploadAttachment,
  deleteAttachment,
  formatFileSize,
  type Attachment
} from "@/api/attachment";
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

// Transfer dialog
const transferDialogVisible = ref(false);
const transferAssignee = ref<number | null>(null);
const transferReason = ref("");

// Escalate dialog
const escalateDialogVisible = ref(false);
const escalationLevel = ref<number>(2);
const escalateReason = ref("");

// Satisfaction dialog
const satisfactionDialogVisible = ref(false);
const satisfactionScore = ref<number>(5);
const satisfactionComment = ref("");

// Merge dialog
const mergeDialogVisible = ref(false);
const mergeTargetId = ref<number | null>(null);
const mergeReason = ref("");

// Attachments
const attachments = ref<Attachment[]>([]);
const uploadingFile = ref(false);

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
  return "info";
}

function priorityTagType(level?: number | null) {
  if (level === 4) return "danger";
  if (level === 3) return "warning";
  return "info";
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

async function handleTransfer() {
  if (!ticket.value || !transferAssignee.value) return;
  actionLoading.value = "transfer";
  try {
    const { code, message: errorMessage, data } = await transferTicket(ticket.value.id, {
      newAssigneeUserId: transferAssignee.value,
      reason: transferReason.value || undefined
    });
    if (code !== 200) throw new Error(errorMessage || "转移失败");
    ticket.value = data;
    transferDialogVisible.value = false;
    transferReason.value = "";
    message("工单已转移", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "转移失败", { type: "error" });
  } finally { actionLoading.value = null; }
}

async function handleEscalate() {
  if (!ticket.value) return;
  actionLoading.value = "escalate";
  try {
    const { code, message: errorMessage, data } = await escalateTicket(ticket.value.id, {
      escalationLevel: escalationLevel.value,
      reason: escalateReason.value || undefined
    });
    if (code !== 200) throw new Error(errorMessage || "升级失败");
    ticket.value = data;
    escalateDialogVisible.value = false;
    escalateReason.value = "";
    message("工单已升级", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "升级失败", { type: "error" });
  } finally { actionLoading.value = null; }
}

async function handleSatisfaction() {
  if (!ticket.value) return;
  actionLoading.value = "satisfaction";
  try {
    const { code, message: errorMessage, data } = await rateSatisfaction(ticket.value.id, {
      score: satisfactionScore.value,
      comment: satisfactionComment.value || undefined
    });
    if (code !== 200) throw new Error(errorMessage || "评价失败");
    ticket.value = data;
    satisfactionDialogVisible.value = false;
    satisfactionComment.value = "";
    message("感谢您的评价", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "评价失败", { type: "error" });
  } finally { actionLoading.value = null; }
}

async function handleMerge() {
  if (!ticket.value || !mergeTargetId.value) return;
  actionLoading.value = "merge";
  try {
    const { code, message: errorMessage, data } = await mergeTicket(ticket.value.id, {
      targetTicketId: mergeTargetId.value,
      reason: mergeReason.value || undefined
    });
    if (code !== 200) throw new Error(errorMessage || "合并失败");
    ticket.value = data;
    mergeDialogVisible.value = false;
    mergeReason.value = "";
    message("工单已合并", { type: "success" });
  } catch (error) {
    message(error instanceof Error ? error.message : "合并失败", { type: "error" });
  } finally { actionLoading.value = null; }
}

function openAssignDialog() {
  selectedAssignee.value = ticket.value?.assigneeUserId ?? null;
  assignDialogVisible.value = true;
}

function openTransferDialog() {
  transferAssignee.value = null;
  transferReason.value = "";
  transferDialogVisible.value = true;
}

function openEscalateDialog() {
  escalationLevel.value = 2;
  escalateReason.value = "";
  escalateDialogVisible.value = true;
}

function openSatisfactionDialog() {
  satisfactionScore.value = 5;
  satisfactionComment.value = "";
  satisfactionDialogVisible.value = true;
}

function openMergeDialog() {
  mergeTargetId.value = null;
  mergeReason.value = "";
  mergeDialogVisible.value = true;
}

async function loadAttachments() {
  if (!ticketId.value) return;
  try {
    const { code, data } = await getAttachments(ticketId.value);
    if (code === 200) attachments.value = data;
  } catch { /* silently fail */ }
}

async function handleFileUpload(event: Event) {
  const input = event.target as HTMLInputElement;
  if (!input.files?.length || !ticket.value) return;
  uploadingFile.value = true;
  try {
    for (const file of input.files) {
      const { code } = await uploadAttachment(ticket.value.id, file);
      if (code !== 200) throw new Error("上传失败");
    }
    message("上传成功", { type: "success" });
    loadAttachments();
  } catch (error) {
    message(error instanceof Error ? error.message : "上传失败", { type: "error" });
  } finally {
    uploadingFile.value = false;
    input.value = "";
  }
}

async function handleDeleteAttachment(a: Attachment) {
  if (!ticket.value) return;
  try {
    await deleteAttachment(ticket.value.id, a.id);
    message("已删除", { type: "success" });
    loadAttachments();
  } catch { message("删除失败", { type: "error" }); }
}

function triggerFileInput() {
  const input = document.getElementById("file-upload") as HTMLInputElement;
  input?.click();
}

const canSatisfaction = computed(() => {
  const s = ticket.value?.status;
  return s === 3 || s === 4;
});

function goBack() { router.back(); }
function goTo(path: string) { router.push(path); }

watch(() => route.params.id, () => loadTicket());
onMounted(() => { loadTicket(); loadAssignees(); loadAttachments(); });
</script>

<template>
  <div v-loading="loading">
    <div class="td-top">
      <el-button text circle @click="goBack">
        <component :is="useRenderIcon('ep:arrow-left', { width: '20px', height: '20px' })" />
      </el-button>
      <span v-if="ticket" class="td-ticket-no">{{ ticket.ticketNo }}</span>
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
                  <el-tag type="info" size="small" :color="confidenceColor(aiDraft.confidence)" effect="dark" round class="ml-2">
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

          <!-- Extra actions -->
          <el-card shadow="never">
            <template #header>
              <span class="font-semibold text-sm flex items-center gap-1.5">
                <component :is="useRenderIcon('ep:operation', { width: '14px', height: '14px' })" />更多操作
              </span>
            </template>
            <div class="flex flex-col gap-2">
              <el-button class="!justify-start !w-full" @click="openTransferDialog">
                <component :is="useRenderIcon('ep:sort', { width: '14px', height: '14px' })" class="mr-2 shrink-0" />转移工单
              </el-button>
              <el-button class="!justify-start !w-full" @click="openEscalateDialog">
                <component :is="useRenderIcon('ep:top', { width: '14px', height: '14px' })" class="mr-2 shrink-0" />升级工单
              </el-button>
              <el-button class="!justify-start !w-full" @click="openMergeDialog">
                <component :is="useRenderIcon('ep:merge', { width: '14px', height: '14px' })" class="mr-2 shrink-0" />合并工单
              </el-button>
              <el-button v-if="canSatisfaction" class="!justify-start !w-full" type="warning" @click="openSatisfactionDialog">
                <component :is="useRenderIcon('ep:star', { width: '14px', height: '14px' })" class="mr-2 shrink-0" />满意度评价
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

    <!-- Transfer dialog -->
    <el-dialog v-model="transferDialogVisible" title="转移工单" width="440px">
      <div class="space-y-3">
        <div>
          <label class="text-sm text-gray-600 mb-1 block">转移给</label>
          <el-select v-model="transferAssignee" placeholder="选择新处理人" class="w-full" filterable>
            <el-option v-for="u in assignees" :key="u.id" :label="`${u.displayName} (@${u.username})`" :value="u.id" />
          </el-select>
        </div>
        <div>
          <label class="text-sm text-gray-600 mb-1 block">转移原因</label>
          <el-input v-model="transferReason" type="textarea" :rows="2" placeholder="可选" />
        </div>
      </div>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!transferAssignee" :loading="actionLoading === 'transfer'" @click="handleTransfer">确认转移</el-button>
      </template>
    </el-dialog>

    <!-- Escalate dialog -->
    <el-dialog v-model="escalateDialogVisible" title="升级工单" width="440px">
      <div class="space-y-3">
        <div>
          <label class="text-sm text-gray-600 mb-1 block">升级级别</label>
          <el-select v-model="escalationLevel" class="w-full">
            <el-option :label="'组长升级'" :value="1" />
            <el-option :label="'经理升级'" :value="2" />
            <el-option :label="'总监升级'" :value="3" />
          </el-select>
        </div>
        <div>
          <label class="text-sm text-gray-600 mb-1 block">升级原因</label>
          <el-input v-model="escalateReason" type="textarea" :rows="2" placeholder="请说明升级原因" />
        </div>
      </div>
      <template #footer>
        <el-button @click="escalateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading === 'escalate'" @click="handleEscalate">确认升级</el-button>
      </template>
    </el-dialog>

    <!-- Satisfaction dialog -->
    <el-dialog v-model="satisfactionDialogVisible" title="满意度评价" width="440px">
      <div class="space-y-4">
        <div class="text-center">
          <el-rate v-model="satisfactionScore" :texts="['很不满', '不满意', '一般', '满意', '很满意']" show-text size="large" />
        </div>
        <div>
          <label class="text-sm text-gray-600 mb-1 block">评价内容</label>
          <el-input v-model="satisfactionComment" type="textarea" :rows="3" placeholder="可选，请分享您的感受" />
        </div>
      </div>
      <template #footer>
        <el-button @click="satisfactionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading === 'satisfaction'" @click="handleSatisfaction">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- Merge dialog -->
    <el-dialog v-model="mergeDialogVisible" title="合并工单" width="440px">
      <div class="space-y-3">
        <div>
          <label class="text-sm text-gray-600 mb-1 block">目标工单 ID</label>
          <el-input-number v-model="mergeTargetId" :min="1" placeholder="输入目标工单 ID" class="w-full" />
        </div>
        <div>
          <label class="text-sm text-gray-600 mb-1 block">合并原因</label>
          <el-input v-model="mergeReason" type="textarea" :rows="2" placeholder="可选" />
        </div>
        <p class="text-xs text-gray-400">当前工单将被合并到目标工单，并自动关闭。</p>
      </div>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!mergeTargetId" :loading="actionLoading === 'merge'" @click="handleMerge">确认合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
/* === Ticket Detail: Stripe Design Language === */
.td-top { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.td-ticket-no { font-family: monospace; font-size: 13px; color: #64748d; font-weight: 500; }

/* Stripe card overrides */
:deep(.el-card) { border: 1px solid #e8ecf1; border-radius: 10px; background: #fff; transition: box-shadow 0.2s; }
:deep(.el-card__header) { padding: 14px 20px; border-bottom: 1px solid #f1f5f9; }
:deep(.el-card__body) { padding: 16px 20px; }
:deep(.el-card:hover) { box-shadow: 0 2px 12px rgba(0,0,0,0.04); }

/* Timeline */
.timeline-card :deep(.el-card__header) { padding: 12px 20px; }
.timeline-card :deep(.el-card__body) { padding: 16px 20px; }
:deep(.el-card:hover) { box-shadow: 0 2px 12px rgba(0,0,0,0.04); }
.tl-timeline { position: relative; padding-left: 24px; }
.tl-timeline::before {
  content: ''; position: absolute; left: 7px; top: 4px; bottom: 4px;
  width: 2px; background: #e8ecf1; border-radius: 1px;
}
.dark .tl-timeline::before { background: #334155; }
.tl-timeline-item { position: relative; padding-bottom: 20px; }
.tl-timeline-item:last-child { padding-bottom: 0; }
.tl-timeline-dot {
  position: absolute; left: -20px; top: 4px;
  width: 10px; height: 10px; border-radius: 50%;
  border: 2px solid #fff; z-index: 1;
}
.dark .tl-timeline-dot { border-color: #1e293b; }
.tl-dot-create { background: #10b981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2); }
.tl-dot-status { background: #3b82f6; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2); }
.tl-dot-assign { background: #f59e0b; box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.2); }
.tl-dot-comment { background: #8b5cf6; box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.2); }
.tl-dot-default { background: #94a3b8; box-shadow: 0 0 0 3px rgba(148, 163, 184, 0.2); }
.tl-timeline-title { font-size: 14px; font-weight: 700; color: #0d253d; }
.dark .tl-timeline-title { color: #f1f5f9; }
.tl-timeline-desc { font-size: 12px; color: #64748d; margin-top: 2px; line-height: 1.5; }
.tl-timeline-meta { font-size: 11px; color: #94a3b8; margin-top: 4px; display: flex; align-items: center; gap: 4px; }
.tl-timeline-operator { font-weight: 500; }
.tl-timeline-sep { color: #cbd5e1; }

.ai-draft-card :deep(.el-card__header) { padding: 12px 20px; }
.ai-draft-card :deep(.el-card__body) { padding: 16px 20px; }

/* Action card */
.td-action-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 12px;
  padding: 16px; margin-bottom: 12px;
}
.td-action-header {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 700; color: #64748d;
  margin-bottom: 14px; padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
  text-transform: uppercase; letter-spacing: 0.5px;
}
.td-action-list { display: flex; flex-direction: column; gap: 8px; }

.td-action-btn-item {
  display: flex; align-items: center; gap: 10px;
  width: 100%; padding: 10px 14px; border: none; border-radius: 10px;
  font-size: 13px; font-weight: 600; cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative; overflow: hidden;
}
.td-action-btn-item:hover { transform: translateY(-1px); }
.td-action-btn-item:active { transform: translateY(0); }
.td-action-btn-item:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.td-action-btn-icon {
  width: 28px; height: 28px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.td-action-btn-label { flex: 1; text-align: left; }
.td-action-spinner {
  width: 14px; height: 14px; border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff; border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Primary action (success/green) */
.td-action-primary {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}
.td-action-primary:hover {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
  background: linear-gradient(135deg, #059669, #047857);
}
.td-action-primary .td-action-btn-icon { background: rgba(255,255,255,0.2); }

/* Success action */
.td-action-success {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}
.td-action-success:hover {
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4);
  background: linear-gradient(135deg, #059669, #047857);
}
.td-action-success .td-action-btn-icon { background: rgba(255,255,255,0.2); }

/* Warning action */
.td-action-warning {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #fff; box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}
.td-action-warning:hover {
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.4);
  background: linear-gradient(135deg, #d97706, #b45309);
}
.td-action-warning .td-action-btn-icon { background: rgba(255,255,255,0.2); }

/* Info action */
.td-action-info {
  background: #f8fafc; color: #475569;
  border: 1px solid #e2e8f0;
}
.td-action-info:hover {
  background: #f1f5f9; border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.td-action-info .td-action-btn-icon { background: #e2e8f0; color: #64748d; }

/* Danger action */
.td-action-danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff; box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}
.td-action-danger:hover {
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.4);
  background: linear-gradient(135deg, #dc2626, #b91c1c);
}
.td-action-danger .td-action-btn-icon { background: rgba(255,255,255,0.2); }

/* Info card */
.td-info-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 12px;
  padding: 16px; margin-bottom: 12px;
}
.td-info-header {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 700; color: #64748d;
  margin-bottom: 14px; padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
  text-transform: uppercase; letter-spacing: 0.5px;
}
.td-info-grid { display: flex; flex-direction: column; gap: 10px; }
.td-info-row { display: flex; justify-content: space-between; align-items: center; }
.td-info-label { font-size: 13px; color: #94a3b8; }
.td-info-value { font-size: 13px; font-weight: 600; color: #0d253d; }
.td-info-mono { font-family: 'SF Mono', monospace; font-size: 12px; color: #64748d; }

/* Dark mode */
.dark .td-action-card { background: #1e293b; border-color: #334155; }
.dark .td-action-header { color: #94a3b8; border-bottom-color: #334155; }
.dark .td-action-info { background: #334155; border-color: #475569; color: #e2e8f0; }
.dark .td-action-info:hover { background: #475569; }
.dark .td-action-info .td-action-btn-icon { background: #475569; color: #94a3b8; }
.dark .td-info-card { background: #1e293b; border-color: #334155; }
.dark .td-info-header { color: #94a3b8; border-bottom-color: #334155; }
.dark .td-info-value { color: #f1f5f9; }
.dark .td-info-mono { color: #94a3b8; }

/* Old action button override */
.td-action-btn { border-radius: 8px; font-weight: 600; transition: all 0.2s; }
.td-action-btn:hover { transform: translateY(-1px); box-shadow: 0 2px 8px rgba(0,0,0,0.1); }

/* Enhanced card headers */
:deep(.el-card__header) {
  font-size: 14px; font-weight: 700; color: #0d253d;
}

/* Timeline line enhancement */
.tl-timeline::before {
  background: linear-gradient(to bottom, #e8ecf1, #c7d2fe, #e8ecf1) !important;
}
.dark .tl-timeline::before {
  background: linear-gradient(to bottom, #334155, #4338ca, #334155) !important;
}
:deep(.el-card:hover) { box-shadow: 0 2px 12px rgba(0,0,0,0.04); }

/* Dark mode */
.dark :deep(.el-card) { background: #1e293b; border-color: #334155; }
.dark :deep(.el-card__header) { border-bottom-color: #334155; }
.dark .tl-timeline-title { color: #f1f5f9; }

/* Ticket detail animations */
.td-top {
  animation: slideInDown 0.3s ease-out forwards;
}

:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
  opacity: 0;
}

:deep(.el-card:nth-child(1)) { animation-delay: 0.1s; }
:deep(.el-card:nth-child(2)) { animation-delay: 0.2s; }
:deep(.el-card:nth-child(3)) { animation-delay: 0.3s; }
:deep(.el-card:nth-child(4)) { animation-delay: 0.4s; }

/* Timeline animation */
.timeline-item {
  animation: fadeInLeft 0.4s ease-out forwards;
  opacity: 0;
}

.timeline-item:nth-child(1) { animation-delay: 0.1s; }
.timeline-item:nth-child(2) { animation-delay: 0.2s; }
.timeline-item:nth-child(3) { animation-delay: 0.3s; }

/* Comment animation */
.comment-item {
  animation: fadeInRight 0.4s ease-out forwards;
  opacity: 0;
}

/* Status badge pulse for 处理中 */
.status-processing {
  animation: pulse 2s ease-in-out infinite;
}

/* Action button hover */
:deep(.el-button) {
  transition: all 0.2s ease;
}

:deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

/* AI draft card special animation */
.ai-draft-card {
  animation: scaleIn 0.5s ease-out forwards;
  border: 1px solid #a78bfa;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}

.dark .ai-draft-card {
  background: linear-gradient(135deg, #1e1b4b 0%, #312e81 100%);
  border-color: #7c3aed;
}
</style>