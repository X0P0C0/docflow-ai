<script setup lang="ts">
import { onMounted, ref, computed } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import dayjs from "dayjs";
import {
  getAiWorkspace,
  getReplyDraft,
  adoptReplyDraft,
  unadoptReplyDraft,
  type AiWorkspace,
  type AiReplySuggestion,
  type AiFollowupItem,
  type AiReplyDraft
} from "@/api/ai";

defineOptions({ name: "DocflowAiCenter" });

const router = useRouter();
const loading = ref(false);
const draftLoading = ref(false);
const workspace = ref<AiWorkspace | null>(null);
const currentDraft = ref<AiReplyDraft | null>(null);
const draftDialogVisible = ref(false);
const errorMessage = ref("");
const debugInfo = ref("");
const currentTime = ref(dayjs().format("HH:mm:ss"));
setInterval(() => { currentTime.value = dayjs().format("HH:mm:ss"); }, 30000);

const stats = computed(() => {
  if (!workspace.value) {
    return [
      { label: "待处理建议", value: "0", icon: "ep:lightbulb", color: "#d97706", bg: "#fffbeb" },
      { label: "已采纳", value: "0", icon: "ep:circle-check", color: "#16a34a", bg: "#f0fdf4" },
      { label: "知识推荐", value: "0", icon: "ep:collection", color: "#9333ea", bg: "#faf5ff" }
    ];
  }
  return [
    { label: "待处理建议", value: String(workspace.value.overview.pendingSuggestions), icon: "ep:lightbulb", color: "#d97706", bg: "#fffbeb" },
    { label: "已采纳", value: String(workspace.value.overview.adoptedSuggestions), icon: "ep:circle-check", color: "#16a34a", bg: "#f0fdf4" },
    { label: "知识推荐", value: String(workspace.value.overview.knowledgeRecommendations), icon: "ep:collection", color: "#9333ea", bg: "#faf5ff" }
  ];
});

function chipType(chipClass: string): "primary" | "success" | "warning" | "info" | "danger" {
  if (chipClass === "danger" || chipClass === "urgent") return "danger";
  if (chipClass === "warning" || chipClass === "medium") return "warning";
  if (chipClass === "success" || chipClass === "adopted") return "success";
  return "info";
}

function confidenceColor(conf: string): string {
  if (!conf) return "#6b7280";
  const pct = parseInt(conf);
  if (pct >= 75) return "#16a34a";
  if (pct >= 50) return "#d97706";
  return "#dc2626";
}

async function loadWorkspace() {
  errorMessage.value = "";
  debugInfo.value = "Loading...";
  loading.value = true;
  try {
    const res = await getAiWorkspace();
    debugInfo.value = "Response: code=" + (res ? res.code : "null") + ", data=" + (res && res.data ? "OK" : "NULL");
    console.log("[AI Center] API response:", res);
    if (res && res.code === 200 && res.data) {
      workspace.value = res.data;
      debugInfo.value = "SUCCESS: workspace loaded";
    } else {
      console.warn("[AI Center] Unexpected response:", res);
      errorMessage.value = "Unexpected response: " + JSON.stringify(res).substring(0, 200);
    }
  } catch (e) {
    console.error("[AI Center] Failed to load workspace:", e);
    errorMessage.value = "Error: " + (e && e.message ? e.message : String(e));
    debugInfo.value = "ERROR: " + (e && e.message ? e.message : String(e));
    message("Failed to load AI workspace", { type: "error" });
  } finally {
    loading.value = false;
  }
}

async function openDraft(ticketId: number) {
  draftDialogVisible.value = true;
  draftLoading.value = true;
  currentDraft.value = null;
  try {
    const { code, data } = await getReplyDraft(ticketId);
    if (code === 200) currentDraft.value = data;
  } catch { message("加载草稿失败", { type: "error" }); }
  finally { draftLoading.value = false; }
}

function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    message("已复制到剪贴板", { type: "success" });
  });
}

async function handleAdopt(ticketId: number) {
  try {
    await adoptReplyDraft(ticketId);
    message("已采纳", { type: "success" });
    loadWorkspace();
  } catch { message("操作失败", { type: "error" }); }
}

async function handleUnadopt(ticketId: number) {
  try {
    await unadoptReplyDraft(ticketId);
    message("已取消采纳", { type: "success" });
    loadWorkspace();
  } catch { message("操作失败", { type: "error" }); }
}

function goTicket(id: number) { router.push(`/tickets/${id}`); }
function goArticle(id: number) { router.push(`/knowledge/articles/${id}`); }

onMounted(() => loadWorkspace());
</script>

<template>
  <div class="docflow-ai-center" v-loading="loading">
    <!-- Header -->
    <div class="mb-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold text-gray-900 dark:text-white">AI 协作工作区</h2>
          <p class="mt-1 text-sm text-gray-500">
            <template v-if="workspace?.heuristicBased">基于规则的 AI 分析（启发式模式）</template>
            <template v-else>AI 智能分析与建议</template>
            <span class="mx-2 text-gray-300">|</span>
            <span class="text-gray-400">更新于 {{ currentTime }}</span>
          </p>
        </div>
        <el-button size="small" text @click="loadWorkspace" :loading="loading">
          <component :is="useRenderIcon('ep:refresh', { width: '16px', height: '16px' })" class="mr-1" />
          刷新
        </el-button>
      </div>
    </div>

    <!-- DEBUG PANEL -->
    <div v-if="debugInfo" style="background:#fef2f2;border:1px solid #fca5a5;padding:8px 16px;margin-bottom:16px;border-radius:6px;font-family:monospace;font-size:13px;">
      <strong>Debug:</strong> {{ debugInfo }}
    </div>

    <!-- Stats row -->
    <el-row :gutter="16" class="mb-6">
      <el-col v-for="(item, i) in stats" :key="i" :xs="12" :sm="8" class="mb-4">
        <el-card shadow="never" class="stat-card">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center rounded-lg size-10 shrink-0" :style="{ backgroundColor: item.bg, color: item.color }">
              <component :is="useRenderIcon(item.icon, { width: '20px', height: '20px' })" />
            </div>
            <div class="min-w-0">
              <p class="text-sm text-gray-500 truncate">{{ item.label }}</p>
              <p class="text-2xl font-bold tracking-tight" :style="{ color: item.color }">{{ item.value }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Two-column layout -->
    <el-row :gutter="16">
      <el-col :md="16" :xs="24">
        <!-- Primary Suggestion -->
        <el-card v-if="workspace?.primarySuggestion" shadow="never" class="mb-4 primary-card">
          <template #header>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <component :is="useRenderIcon('ep:star-filled', { width: '16px', height: '16px' })" class="text-yellow-500" />
                <span class="font-semibold">首要建议</span>
              </div>
              <el-tag size="small" :color="confidenceColor(workspace.primarySuggestion.confidence)" effect="dark" round>
                置信度 {{ workspace.primarySuggestion.confidence }}
              </el-tag>
            </div>
          </template>

          <div class="mb-3 flex items-center gap-2 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 p-2 -mx-2 rounded transition-colors" @click="goTicket(workspace.primarySuggestion.ticketId)">
            <span class="font-mono text-xs text-gray-400 bg-gray-100 dark:bg-gray-800 px-2 py-0.5 rounded">
              {{ workspace.primarySuggestion.ticketNo }}
            </span>
            <span class="font-medium text-sm">{{ workspace.primarySuggestion.title }}</span>
            <component :is="useRenderIcon('ep:arrow-right', { width: '14px', height: '14px' })" class="ml-auto text-gray-400" />
          </div>

          <div class="text-sm text-gray-600 dark:text-gray-300 mb-3 p-4 bg-blue-50/60 dark:bg-blue-900/10 rounded-lg leading-relaxed border-l-2 border-blue-300 dark:border-blue-700">
            {{ workspace.primarySuggestion.summary }}
          </div>

          <div v-if="workspace.primarySuggestion.checklist?.length" class="mb-4">
            <p class="text-xs font-medium text-gray-500 mb-2 flex items-center gap-1">
              <component :is="useRenderIcon('ep:list', { width: '14px', height: '14px' })" />
              检查清单
            </p>
            <div class="space-y-1">
              <div v-for="(item, idx) in workspace.primarySuggestion.checklist" :key="idx"
                class="flex items-start gap-2 text-sm text-gray-600 dark:text-gray-300 pl-2 py-1">
                <component :is="useRenderIcon('ep:select', { width: '14px', height: '14px' })" class="mt-0.5 text-blue-500 shrink-0" />
                <span>{{ item }}</span>
              </div>
            </div>
          </div>

          <div class="flex gap-2 pt-2 border-t border-gray-100 dark:border-gray-700">
            <el-button
              v-if="!workspace.primarySuggestion.adopted"
              size="small" type="primary"
              @click="handleAdopt(workspace.primarySuggestion.ticketId)"
            >
              <component :is="useRenderIcon('ep:check', { width: '14px', height: '14px' })" class="mr-1" />采纳
            </el-button>
            <el-button v-else size="small" type="warning" @click="handleUnadopt(workspace.primarySuggestion.ticketId)">
              <component :is="useRenderIcon('ep:close', { width: '14px', height: '14px' })" class="mr-1" />取消采纳
            </el-button>
            <el-button size="small" @click="openDraft(workspace.primarySuggestion.ticketId)">
              <component :is="useRenderIcon('ep:document-copy', { width: '14px', height: '14px' })" class="mr-1" />查看草稿
            </el-button>
            <el-button size="small" @click="goTicket(workspace.primarySuggestion.ticketId)">
              <component :is="useRenderIcon('ep:link', { width: '14px', height: '14px' })" class="mr-1" />工单
            </el-button>
          </div>
        </el-card>

        <!-- Feed -->
        <el-card v-if="workspace?.feed?.length" shadow="never" class="mb-4 feed-card">
          <template #header>
            <div class="flex items-center gap-2">
              <component :is="useRenderIcon('ep:data-line', { width: '16px', height: '16px' })" class="text-gray-500" />
              <span class="font-semibold">动态</span>
            </div>
          </template>
          <div v-for="(item, idx) in workspace.feed" :key="idx"
            class="flex justify-between items-center py-2.5 px-2 rounded transition-colors hover:bg-gray-50 dark:hover:bg-gray-800"
            :class="{ 'border-t border-gray-100 dark:border-gray-700': idx > 0 }">
            <span class="text-sm text-gray-700 dark:text-gray-200">{{ item.title }}</span>
            <span class="text-sm font-semibold text-gray-900 dark:text-white">{{ item.value }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- Right column -->
      <el-col :md="8" :xs="24">
        <el-card v-if="workspace?.recommendations?.length" shadow="never" class="mb-4 rec-card">
          <template #header>
            <div class="flex items-center gap-2">
              <component :is="useRenderIcon('ep:collection', { width: '16px', height: '16px' })" class="text-purple-500" />
              <span class="font-semibold">知识推荐</span>
            </div>
          </template>
          <div v-for="(rec, idx) in workspace.recommendations" :key="rec.articleId"
            class="py-3 px-2 rounded transition-colors cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800"
            :class="{ 'border-t border-gray-100 dark:border-gray-700': idx > 0 }"
            @click="goArticle(rec.articleId)">
            <div class="flex items-start gap-2">
              <component :is="useRenderIcon('ep:document', { width: '14px', height: '14px' })" class="mt-0.5 text-gray-400 shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ rec.title }}</p>
                <p class="text-xs text-gray-400 mt-0.5">{{ rec.reason }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2 mt-2 ml-6">
              <div class="flex-1 h-1.5 bg-gray-100 dark:bg-gray-700 rounded-full overflow-hidden">
                <div class="h-full bg-purple-500 rounded-full transition-all" :style="{ width: rec.matchRate || '0%' }" />
              </div>
              <span class="text-xs font-medium text-purple-600 shrink-0">{{ rec.matchRate }}</span>
            </div>
          </div>
        </el-card>

        <el-card v-if="workspace?.followups?.length" shadow="never" class="followup-card">
          <template #header>
            <div class="flex items-center gap-2">
              <component :is="useRenderIcon('ep:clock', { width: '16px', height: '16px' })" class="text-orange-500" />
              <span class="font-semibold">跟进事项</span>
            </div>
          </template>
          <div v-for="(item, idx) in workspace.followups" :key="idx"
            class="flex items-start gap-2 py-2.5 px-2 rounded transition-colors cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800"
            :class="{ 'border-t border-gray-100 dark:border-gray-700': idx > 0 }"
            @click="goTicket(item.ticketId)">
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-800 dark:text-gray-200">{{ item.title }}</p>
              <p class="text-xs text-gray-400 mt-0.5">{{ item.desc }}</p>
            </div>
            <el-tag size="small" :type="chipType(item.chipClass)" round>{{ item.chip }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Empty -->
    <el-empty v-if="!loading && !workspace" description="AI 工作区暂无数据" :image-size="120">
      <div v-if="errorMessage" style="color: #dc2626; font-size: 12px; margin-bottom: 8px; max-width: 400px; word-break: break-all;">{{ errorMessage }}</div>
      <el-button type="primary" @click="loadWorkspace">重新加载</el-button>
    </el-empty>

    <!-- Reply Draft Dialog -->
    <el-dialog v-model="draftDialogVisible" title="AI 回复草稿" width="640px" class="draft-dialog">
      <div v-loading="draftLoading" v-if="currentDraft" class="space-y-4">
        <!-- Meta -->
        <div class="flex items-center gap-3 text-sm">
          <span class="font-mono text-gray-400">{{ currentDraft.ticketNo }}</span>
          <span class="font-medium">{{ currentDraft.ticketTitle }}</span>
          <el-tag size="small" :color="confidenceColor(currentDraft.confidence)" effect="dark" round>
            {{ currentDraft.confidence }}
          </el-tag>
        </div>

        <!-- Scene + Diagnosis -->
        <div class="grid grid-cols-1 gap-3">
          <div v-if="currentDraft.scene" class="p-3 bg-amber-50 dark:bg-amber-900/10 rounded-lg">
            <p class="text-xs font-medium text-amber-700 dark:text-amber-400 mb-1">场景分析</p>
            <p class="text-sm text-gray-600 dark:text-gray-300">{{ currentDraft.scene }}</p>
          </div>
          <div v-if="currentDraft.diagnosis" class="p-3 bg-blue-50 dark:bg-blue-900/10 rounded-lg">
            <p class="text-xs font-medium text-blue-700 dark:text-blue-400 mb-1">诊断</p>
            <p class="text-sm text-gray-600 dark:text-gray-300">{{ currentDraft.diagnosis }}</p>
          </div>
          <div v-if="currentDraft.nextStep" class="p-3 bg-green-50 dark:bg-green-900/10 rounded-lg">
            <p class="text-xs font-medium text-green-700 dark:text-green-400 mb-1">建议下一步</p>
            <p class="text-sm text-gray-600 dark:text-gray-300">{{ currentDraft.nextStep }}</p>
          </div>
        </div>

        <!-- Customer Reply -->
        <div v-if="currentDraft.customerReply" class="p-4 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
          <div class="flex items-center justify-between mb-2">
            <p class="text-xs font-medium text-gray-500">回复模板</p>
            <el-button size="small" text type="primary" @click="copyText(currentDraft.customerReply)">
              <component :is="useRenderIcon('ep:copy-document', { width: '14px', height: '14px' })" class="mr-1" />
              复制
            </el-button>
          </div>
          <p class="text-sm leading-relaxed text-gray-800 dark:text-gray-200 whitespace-pre-wrap">{{ currentDraft.customerReply }}</p>
        </div>

        <!-- Operator Notes -->
        <div v-if="currentDraft.operatorNotes?.length" class="p-3 bg-purple-50 dark:bg-purple-900/10 rounded-lg">
          <p class="text-xs font-medium text-purple-700 dark:text-purple-400 mb-2">操作提示</p>
          <ul class="space-y-1">
            <li v-for="(note, idx) in currentDraft.operatorNotes" :key="idx"
              class="text-sm text-gray-600 dark:text-gray-300 flex items-start gap-2">
              <span class="text-purple-500 mt-0.5">•</span>
              <span>{{ note }}</span>
            </li>
          </ul>
        </div>
      </div>
      <div v-else-if="!draftLoading" class="text-center py-8 text-gray-400">暂无草稿数据</div>
    </el-dialog>
  </div>
</template>

<style scoped>
.docflow-ai-center .stat-card :deep(.el-card__body) { padding: 20px; }
.docflow-ai-center .primary-card :deep(.el-card__body) { padding: 20px 24px; }
.docflow-ai-center .feed-card :deep(.el-card__body) { padding: 12px 24px 16px; }
.docflow-ai-center .rec-card :deep(.el-card__body) { padding: 12px 20px 16px; }
.docflow-ai-center .followup-card :deep(.el-card__body) { padding: 12px 20px 16px; }
.stat-card { transition: transform 0.15s ease, box-shadow 0.15s ease; }
.stat-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.draft-dialog :deep(.el-dialog__body) { padding-top: 8px; }
</style>