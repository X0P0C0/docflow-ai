<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, shallowRef, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import dayjs from "dayjs";
import "@wangeditor/editor/dist/css/style.css";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import {
  getKnowledgeArticle,
  createKnowledgeArticle,
  updateKnowledgeArticle
} from "@/api/knowledge";

defineOptions({ name: "DocflowKnowledgeEditor" });

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const lastSavedAt = ref<string | null>(null);
const autoSaveStatus = ref<"" | "saving" | "saved">("");

const articleId = computed(() => {
  const id = Number(route.params.id);
  return Number.isFinite(id) ? id : null;
});
const isEdit = computed(() => articleId.value !== null);

const form = ref({
  title: "",
  summary: "",
  content: "",
  status: 0,
  ticketNo: null as string | null
});

const statusOptions = [
  { label: "草稿", value: 0 },
  { label: "发布", value: 1 }
];

// Auto-save key
const draftKey = computed(() => `docflow_draft_${articleId.value ?? "new"}`);

// wangEditor
const editorRef = shallowRef();
const editorContent = ref("");
const editorReady = ref(false);

const toolbarConfig = {
  excludeKeys: ["fullScreen", "group-video"]
};
const editorConfig = {
  placeholder: "开始编写知识库文章...",
  autoFocus: false
};

function handleCreated(editor: any) {
  editorRef.value = editor;
  editorReady.value = true;
}

// --- Auto-save ---
let autoSaveTimer: ReturnType<typeof setInterval> | null = null;
let lastAutoSavedContent = "";

function saveDraft() {
  if (!editorReady.value) return;
  const content = editorContent.value;
  if (!content || content === "<p><br></p>") return;
  if (content === lastAutoSavedContent) return;

  const draft = {
    title: form.value.title,
    summary: form.value.summary,
    content: content,
    status: form.value.status,
    savedAt: dayjs().format("HH:mm:ss")
  };
  try {
    localStorage.setItem(draftKey.value, JSON.stringify(draft));
    lastAutoSavedContent = content;
    autoSaveStatus.value = "saved";
    setTimeout(() => { autoSaveStatus.value = ""; }, 2000);
  } catch { /* storage full */ }
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(draftKey.value);
    if (!raw) return false;
    const draft = JSON.parse(raw);
    form.value.title = draft.title || form.value.title;
    form.value.summary = draft.summary || form.value.summary;
    form.value.status = draft.status ?? form.value.status;
    editorContent.value = draft.content || "";
    message(`已恢复草稿（${draft.savedAt}）`, { type: "info" });
    return true;
  } catch { return false; }
}

function clearDraft() {
  localStorage.removeItem(draftKey.value);
  lastAutoSavedContent = "";
}

function startAutoSave() {
  autoSaveTimer = setInterval(saveDraft, 15000);
}

function stopAutoSave() {
  if (autoSaveTimer) { clearInterval(autoSaveTimer); autoSaveTimer = null; }
}

// --- Load article ---
async function loadArticle() {
  if (!articleId.value) return;
  loading.value = true;
  try {
    const { code, message: errorMessage, data } = await getKnowledgeArticle(articleId.value);
    if (code !== 200) throw new Error(errorMessage || "加载失败");
    form.value = {
      title: data.title,
      summary: data.summary || "",
      content: data.content,
      status: data.status ?? 0,
      ticketNo: data.ticketNo ?? null
    };
    // Check for newer local draft
    const raw = localStorage.getItem(draftKey.value);
    if (raw) {
      try {
        const draft = JSON.parse(raw);
        if (draft.content && draft.content !== data.content) {
          message("检测到本地草稿，已自动恢复", { type: "info" });
          editorContent.value = draft.content;
          if (draft.title) form.value.title = draft.title;
          if (draft.summary) form.value.summary = draft.summary;
          return;
        }
      } catch { /* ignore */ }
    }
    editorContent.value = data.content || "";
  } catch (error) {
    message(error instanceof Error ? error.message : "加载失败", { type: "error" });
  } finally { loading.value = false; }
}

async function submit() {
  if (!form.value.title.trim()) {
    message("请输入文章标题", { type: "warning" });
    return;
  }
  if (!editorContent.value || editorContent.value === "<p><br></p>") {
    message("请输入文章内容", { type: "warning" });
    return;
  }
  saving.value = true;
  try {
    const request = {
      title: form.value.title.trim(),
      summary: form.value.summary.trim() || undefined,
      content: editorContent.value,
      status: form.value.status
    };

    const { code, message: errorMessage, data } = isEdit.value
      ? await updateKnowledgeArticle(articleId.value!, request)
      : await createKnowledgeArticle(request);

    if (code !== 200) throw new Error(errorMessage || "保存失败");
    clearDraft();
    message(isEdit.value ? "更新成功" : "创建成功", { type: "success" });
    router.push(`/knowledge/articles/${data.id}`);
  } catch (error) {
    message(error instanceof Error ? error.message : "保存失败", { type: "error" });
  } finally { saving.value = false; }
}

function goBack() {
  saveDraft();
  router.push("/knowledge/articles");
}

// Watch content for auto-save indicator
watch(editorContent, () => {
  if (editorReady.value) {
    autoSaveStatus.value = "saving";
    // Debounce the status back to empty
    clearTimeout(autoSaveTimer as any);
    setTimeout(() => { if (autoSaveStatus.value === "saving") autoSaveStatus.value = ""; }, 1500);
  }
});

onMounted(() => {
  if (isEdit.value) {
    loadArticle();
  } else {
    loadDraft();
  }
  startAutoSave();
});

onBeforeUnmount(() => {
  stopAutoSave();
  saveDraft();
  const editor = editorRef.value;
  if (editor) editor.destroy();
});
</script>

<template>
  <div class="docflow-knowledge-editor" v-loading="loading">
    <!-- Header -->
    <div class="mb-5 flex items-center justify-between">
      <div class="flex items-center gap-3">
        <el-button text circle @click="goBack">
          <component :is="useRenderIcon('ep:arrow-left', { width: '20px', height: '20px' })" />
        </el-button>
        <div>
          <h2 class="text-xl font-bold text-gray-900 dark:text-white">
            {{ isEdit ? '编辑文章' : '新建文章' }}
          </h2>
          <p class="text-xs text-gray-500 mt-0.5">
            {{ isEdit ? '编辑已有知识库文章' : '撰写新的知识库文章' }}
            <template v-if="autoSaveStatus">
              <span class="mx-1 text-gray-300">|</span>
              <span v-if="autoSaveStatus === 'saving'" class="text-amber-500">正在保存...</span>
              <span v-else-if="autoSaveStatus === 'saved'" class="text-green-500">草稿已保存</span>
            </template>
          </p>
        </div>
      </div>
      <div class="flex gap-2">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit" :disabled="!form.title.trim()">
          <component :is="useRenderIcon('ep:check', { width: '16px', height: '16px' })" class="mr-1" />
          {{ isEdit ? '保存修改' : '发布文章' }}
        </el-button>
      </div>
    </div>

    <!-- Source ticket -->
    <div v-if="isEdit && form.ticketNo" class="mb-4 flex items-center gap-4 px-4 py-2.5 bg-blue-50 dark:bg-blue-900/20 rounded-lg text-sm">
      <span class="text-gray-500">来源工单：</span>
      <router-link :to="`/tickets/${form.ticketNo}`" class="text-blue-600 font-medium hover:underline flex items-center gap-1">
        <component :is="useRenderIcon('ep:link', { width: '14px', height: '14px' })" />
        #{{ form.ticketNo }}
      </router-link>
    </div>

    <!-- Metadata -->
    <el-card shadow="never" class="mb-4 metadata-card">
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">
            文章标题 <span class="text-red-500">*</span>
          </label>
          <el-input v-model="form.title" placeholder="输入文章标题..." maxlength="200" show-word-limit size="large" class="title-input" />
        </div>
        <div>
          <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">发布状态</label>
          <el-radio-group v-model="form.status" size="large" class="mt-1">
            <el-radio-button v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div class="mt-4">
        <label class="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-1.5">摘要</label>
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="简要描述文章内容，将展示在列表页..." maxlength="500" show-word-limit />
      </div>
    </el-card>

    <!-- Editor -->
    <el-card shadow="never" class="editor-card" body-class="!p-0">
      <div class="wangeditor-wrapper">
        <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" class="toolbar-border" />
        <Editor v-model="editorContent" :defaultConfig="editorConfig" mode="default" class="editor-body" @onCreated="handleCreated" />
      </div>
    </el-card>

    <!-- Bottom -->
    <div class="mt-5 flex items-center justify-between">
      <span class="text-xs text-gray-400">
        每 15 秒自动保存草稿 · 支持富文本编辑
        <span v-if="autoSaveStatus === 'saved'" class="ml-2 text-green-500">✓ 已保存</span>
      </span>
      <div class="flex gap-2">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit" :disabled="!form.title.trim()">
          <component :is="useRenderIcon('ep:check', { width: '16px', height: '16px' })" class="mr-1" />
          {{ isEdit ? '保存修改' : '发布文章' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.docflow-knowledge-editor .metadata-card :deep(.el-card__body) { padding: 20px 24px; }
.docflow-knowledge-editor .editor-card :deep(.el-card__body) { padding: 0; }

.title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-bottom: 2px solid #e5e7eb;
  border-radius: 0;
  padding: 0 0 4px 0;
}
.title-input :deep(.el-input__wrapper:hover) { border-bottom-color: #3b82f6; }
.title-input :deep(.el-input__wrapper.is-focus) { border-bottom-color: #3b82f6; box-shadow: none !important; }
.title-input :deep(.el-input__inner) { font-size: 18px; font-weight: 600; }
.title-input :deep(.el-input__inner::placeholder) { color: #9ca3af; font-weight: 400; font-size: 16px; }

.dark .title-input :deep(.el-input__wrapper) { border-bottom-color: #374151; }
.dark .title-input :deep(.el-input__wrapper:hover),
.dark .title-input :deep(.el-input__wrapper.is-focus) { border-bottom-color: #60a5fa; }
.dark .title-input :deep(.el-input__inner) { color: #f3f4f6; }

.wangeditor-wrapper { border: 0; }
.toolbar-border { border-bottom: 1px solid #e5e7eb !important; }
.dark .toolbar-border { border-bottom-color: #374151 !important; }
.editor-body { height: 480px; overflow-y: hidden; }
</style>