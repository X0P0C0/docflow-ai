<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { getKnowledgeArticles, deleteKnowledgeArticle, type KnowledgeArticle } from "@/api/knowledge";
import dayjs from "dayjs";

defineOptions({ name: "DocflowKnowledgeList" });

const router = useRouter();
const loading = ref(false);
const currentPage = ref(1);
const pageSize = ref(20);
const totalArticles = ref(0);
const deleting = ref<number | null>(null);
const searchKeyword = ref("");
const articles = ref<KnowledgeArticle[]>([]);
const activeStatus = ref<number | undefined>(undefined);

const statusFilters = [
  { label: "全部", value: undefined },
  { label: "草稿", value: 0 },
  { label: "已发布", value: 1 },
  { label: "已归档", value: 2 }
];

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

const filteredArticles = computed(() => articles.value);

const stats = computed(() => ({
  total: totalArticles.value,
  published: totalArticles.value,
  draft: 0
}));

function formatDate(value?: string | null) {
  return value ? dayjs(value).format("MM-DD HH:mm") : "-";
}

function goTo(path: string) { router.push(path); }

async function loadArticles() {
  loading.value = true;
  try {
    const { code, message: errorMessage, data } = await getKnowledgeArticles({
      keyword: searchKeyword.value.trim() || undefined,
      status: activeStatus.value,
      page: currentPage.value,
      size: pageSize.value
    });
    if (code !== 200) throw new Error(errorMessage || "加载失败");
    articles.value = data.records;
    totalArticles.value = data.total;
  } catch (error) {
    message(error instanceof Error ? error.message : "加载失败", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function handlePageChange(page: number) {
  currentPage.value = page;
  loadArticles();
}

function handleSizeChange(size: number) {
  pageSize.value = size;
  currentPage.value = 1;
  loadArticles();
}

async function handleDelete(id: number) {
  deleting.value = id;
  try {
    await deleteKnowledgeArticle(id);
    articles.value = articles.value.filter(a => a.id !== id);
    message("已删除", { type: "success" });
  } catch {
    message("删除失败", { type: "error" });
  } finally {
    deleting.value = null;
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null;
watch(searchKeyword, () => {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    currentPage.value = 1;
    loadArticles();
  }, 400);
});

watch(activeStatus, () => {
  currentPage.value = 1;
  loadArticles();
});

onMounted(() => loadArticles());
</script>

<template>
  <div>
    <div class="mb-5 flex items-center justify-between">
      <div>
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">知识库</h2>
        <p class="mt-0.5 text-sm text-gray-400">{{ totalArticles }} 篇文章</p>
      </div>
      <el-button type="primary" @click="goTo('/knowledge/articles/create')">
        <component :is="useRenderIcon('ep:plus', { width: '16px', height: '16px' })" class="mr-1" />
        新建文章
      </el-button>
    </div>

    <!-- Search & Filter -->
    <div class="flex items-center gap-3 mb-4">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文章..."
        clearable
        class="max-w-sm"
      >
        <template #prefix>
          <component :is="useRenderIcon('ep:search', { width: '14px', height: '14px' })" />
        </template>
      </el-input>
      <el-radio-group v-model="activeStatus" size="small">
        <el-radio-button v-for="f in statusFilters" :key="f.label" :value="f.value">
          {{ f.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- Article List -->
    <el-card shadow="never" v-loading="loading" class="overflow-hidden">
      <div v-if="filteredArticles.length" class="divide-y divide-gray-100 dark:divide-gray-700">
        <div
          v-for="article in filteredArticles"
          :key="article.id"
          class="group flex items-start gap-4 py-4 px-2 -mx-2 rounded-lg cursor-pointer transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
          @click="goTo(`/knowledge/articles/${article.id}`)"
        >
          <!-- Status indicator -->
          <div class="shrink-0 mt-0.5">
            <div
              class="w-2 h-2 rounded-full"
              :class="{
                'bg-green-500': article.status === 1,
                'bg-yellow-400': article.status === 0,
                'bg-gray-300': article.status === 2
              }"
            />
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-sm">{{ article.title }}</span>
              <el-tag :type="statusTagType(article.status)" size="small" effect="light">
                {{ statusLabel(article.status) }}
              </el-tag>
            </div>
            <p v-if="article.summary" class="text-xs text-gray-400 line-clamp-2 mb-2">
              {{ article.summary }}
            </p>
            <div class="flex items-center gap-3 text-xs text-gray-400">
              <span v-if="article.sourceTicket" class="inline-flex items-center gap-1">
                <component :is="useRenderIcon('ep:link', { width: '12px', height: '12px' })" />
                <span class="font-mono">{{ article.sourceTicket.ticketNo }}</span>
              </span>
              <span>{{ article.viewCount || 0 }} 次浏览</span>
              <span>{{ formatDate(article.updateTime) }}</span>
            </div>
          </div>

          <!-- Actions -->
          <div class="shrink-0 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <el-button text size="small" @click.stop="goTo(`/knowledge/articles/${article.id}/edit`)">
              <component :is="useRenderIcon('ep:edit', { width: '16px', height: '16px' })" />
            </el-button>
            <el-button text size="small" type="danger" :loading="deleting === article.id" @click.stop="handleDelete(article.id)">
              <component :is="useRenderIcon('ep:delete', { width: '16px', height: '16px' })" />
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无文章" :image-size="60" />

      <div v-if="totalArticles > 0" class="flex justify-center pt-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="totalArticles"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>