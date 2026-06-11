<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { getKnowledgeArticle, deleteKnowledgeArticle, type KnowledgeArticle } from "@/api/knowledge";
import dayjs from "dayjs";

defineOptions({ name: "DocflowKnowledgeDetail" });

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const article = ref<KnowledgeArticle | null>(null);
const articleId = computed(() => Number(route.params.id));

const statusLabel = (s?: number | null) => {
  if (s === 0) return "草稿";
  if (s === 1) return "已发布";
  if (s === 2) return "已归档";
  return "未知";
};

const statusTagType = (s?: number | null) => {
  if (s === 1) return "success";
  if (s === 2) return "info";
  return "warning";
};

function formatDateTime(value?: string | null) {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

async function loadArticle() {
  if (!Number.isFinite(articleId.value)) return;
  loading.value = true;
  try {
    const { code, message: errorMessage, data } = await getKnowledgeArticle(articleId.value);
    if (code !== 200) throw new Error(errorMessage || "加载失败");
    article.value = data;
  } catch (error) {
    message(error instanceof Error ? error.message : "加载失败", { type: "error" });
  } finally {
    loading.value = false;
  }
}

async function handleDelete() {
  if (!article.value) return;
  try {
    await deleteKnowledgeArticle(article.value.id);
    message("已删除", { type: "success" });
    router.push("/knowledge/articles");
  } catch {
    message("删除失败", { type: "error" });
  }
}

function goBack() { router.push("/knowledge/articles"); }
function goEdit() { router.push(`/knowledge/articles/${articleId.value}/edit`); }

onMounted(() => loadArticle());
</script>

<template>
  <div class="max-w-4xl" v-loading="loading">
    <div class="kd-top">
      <div class="flex items-center gap-2">
        <el-button text circle @click="goBack">
          <component :is="useRenderIcon('ep:arrow-left', { width: '20px', height: '20px' })" />
        </el-button>
        <span class="text-sm text-gray-400">知识库</span>
      </div>
      <div v-if="article" class="flex items-center gap-2">
        <el-button @click="goEdit">
          <component :is="useRenderIcon('ep:edit', { width: '14px', height: '14px' })" class="mr-1" />
          编辑
        </el-button>
        <el-popconfirm title="确定删除这篇文章？" @confirm="handleDelete">
          <template #reference>
            <el-button type="danger" text>
              <component :is="useRenderIcon('ep:delete', { width: '16px', height: '16px' })" />
            </el-button>
          </template>
        </el-popconfirm>
      </div>
    </div>

    <template v-if="article">
      <!-- Header -->
      <div class="kd-card kd-header-card">
        <div class="flex items-center gap-2 mb-3">
          <el-tag :type="statusTagType(article.status)" size="small" effect="light">
            {{ statusLabel(article.status) }}
          </el-tag>
          <span v-if="article.sourceTicket" class="text-xs text-gray-400">
            来源工单：
            <router-link :to="`/tickets/${article.sourceTicket.id}`" class="kd-source-link">
              {{ article.sourceTicket.ticketNo }} {{ article.sourceTicket.title }}
            </router-link>
          </span>
        </div>
        <h1 class="text-xl font-bold leading-snug">{{ article.title }}</h1>
        <div class="flex items-center gap-3 mt-3 text-xs text-gray-400">
          <span>{{ article.viewCount || 0 }} 次浏览</span>
          <span>·</span>
          <span>更新于 {{ formatDateTime(article.updateTime) }}</span>
          <span>·</span>
          <span>创建于 {{ formatDateTime(article.createTime) }}</span>
        </div>
      </div>

      <!-- Summary -->
      <div v-if="article.summary" class="kd-card">
        <div class="text-sm text-gray-500 leading-relaxed">{{ article.summary }}</div>
      </div>

      <!-- Content -->
      <div class="kd-card kd-content-card">
        <div class="knowledge-content" v-html="article.content"></div>
      </div>

      <!-- Versions -->
      <div v-if="article.versions && article.versions.length > 1" class="kd-card">
        <div class="kd-card-header">
          <component :is="useRenderIcon('ep:clock', { width: '14px', height: '14px' })" />
          <span>版本历史</span>
        </div>
        <div class="divide-y divide-gray-100 dark:divide-gray-700">
          <div v-for="v in article.versions" :key="v.id"
            class="flex items-center justify-between py-2.5 text-sm">
            <div class="flex items-center gap-3">
              <span class="font-mono text-xs text-gray-400">v{{ v.versionNo }}</span>
              <span v-if="v.remark" class="text-gray-500">{{ v.remark }}</span>
            </div>
            <span class="text-xs text-gray-400">{{ formatDateTime(v.createTime) }}</span>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="文章不存在或无权访问" />
  </div>
</template>

<style scoped>
/* === Knowledge Detail: Stripe Design Language === */
.kd-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.kd-card { background: #fff; border: 1px solid #e8ecf1; border-radius: 8px; padding: 20px 24px; margin-bottom: 12px; }
.kd-header-card { border-top: 3px solid #533afd; }
.kd-content-card { padding: 24px; }
.kd-source-link { color: #533afd; font-weight: 600; text-decoration: none; }
.kd-card-header { display: flex; align-items: center; gap: 8px; font-size: 14px; font-weight: 600; color: #0d253d; margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #f1f5f9; }
.kd-source-link:hover { text-decoration: underline; }

.dark .kd-card { background: #1e293b; border-color: #334155; }
.dark .kd-header-card { border-top-color: #818cf8; }
.dark .kd-source-link { color: #818cf8; }
.dark .kd-card-header { color: #f1f5f9; border-bottom-color: #334155; }
.knowledge-content :deep(h1) { font-size: 1.5rem; font-weight: 700; margin: 1rem 0 0.5rem; }
.knowledge-content :deep(h2) { font-size: 1.25rem; font-weight: 600; margin: 0.75rem 0 0.5rem; }
.knowledge-content :deep(h3) { font-size: 1.1rem; font-weight: 600; margin: 0.5rem 0 0.25rem; }
.knowledge-content :deep(p) { margin-bottom: 0.5rem; line-height: 1.7; }
.knowledge-content :deep(ul), .knowledge-content :deep(ol) { margin: 0.5rem 0; padding-left: 1.5rem; }
.knowledge-content :deep(li) { margin-bottom: 0.25rem; }
.knowledge-content :deep(pre) { background: #f3f4f6; border-radius: 6px; padding: 12px 16px; overflow-x: auto; margin: 0.75rem 0; font-size: 13px; }
.dark .knowledge-content :deep(pre) { background: #1f2937; }
.knowledge-content :deep(code) { font-size: 13px; background: #f3f4f6; padding: 1px 4px; border-radius: 3px; }
.dark .knowledge-content :deep(code) { background: #374151; }
.knowledge-content :deep(blockquote) { border-left: 3px solid #533afd; padding: 4px 0 4px 16px; margin: 0.75rem 0; color: #64748d; background: #f8fafc; border-radius: 0 4px 4px 0; }
.dark .knowledge-content :deep(blockquote) { background: #111827; }
.knowledge-content :deep(table) { width: 100%; border-collapse: collapse; margin: 0.75rem 0; }
.knowledge-content :deep(th), .knowledge-content :deep(td) { border: 1px solid #e5e7eb; padding: 8px 12px; text-align: left; }
.knowledge-content :deep(th) { background: #f8fafc; font-weight: 600; }
.dark .knowledge-content :deep(th) { background: #1f2937; }
.dark .knowledge-content :deep(th), .dark .knowledge-content :deep(td) { border-color: #374151; }
.knowledge-content :deep(img) { max-width: 100%; border-radius: 6px; margin: 0.5rem 0; }
.knowledge-content :deep(a) { color: #533afd; text-decoration: underline; }
</style>
