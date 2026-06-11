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
const viewMode = ref<"card" | "table">("card");

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

const statusTagType = (s?: number | null): "success" | "info" | "warning" => {
  if (s === 1) return "success";
  if (s === 2) return "info";
  return "warning";
};

const stats = computed(() => ({
  total: totalArticles.value,
  showing: articles.value.length
}));

function formatDate(value?: string | null) {
  return value ? dayjs(value).format("MM-DD HH:mm") : "-";
}

function goTo(path: string) { router.push(path); }

async function loadArticles() {
  loading.value = true;
  try {
    const { code, data } = await getKnowledgeArticles({
      keyword: searchKeyword.value.trim() || undefined,
      status: activeStatus.value,
      page: currentPage.value,
      size: pageSize.value
    });
    if (code === 200) {
      articles.value = (data as any)?.records || (Array.isArray(data) ? data : []);
      totalArticles.value = data?.total || articles.value.length;
    }
  } finally { loading.value = false; }
}

async function handleDelete(article: KnowledgeArticle) {
  deleting.value = article.id;
  try {
    const { code } = await deleteKnowledgeArticle(article.id);
    if (code === 200) { message("删除成功", { type: "success" }); loadArticles(); }
  } finally { deleting.value = null; }
}

let searchTimer: ReturnType<typeof setTimeout>;
function onSearchInput() {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => { currentPage.value = 1; loadArticles(); }, 300);
}

function handleStatusChange(val: number | undefined) {
  activeStatus.value = val;
  currentPage.value = 1;
  loadArticles();
}

watch([currentPage, pageSize], loadArticles);
onMounted(loadArticles);
</script>

<template>
  <div class="p-4">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold">知识库</h2>
        <span class="text-sm text-gray-400">共 {{ stats.total }} 篇文章</span>
      </div>
      <el-button type="primary" @click="goTo('/knowledge/editor')">
        <el-icon class="mr-1"><component :is="useRenderIcon('ep:plus')" /></el-icon>
        新建文章
      </el-button>
    </div>

    <!-- Search & Filters -->
    <el-card shadow="never" class="mb-4">
      <div class="flex flex-wrap items-center gap-3">
        <el-input v-model="searchKeyword" placeholder="搜索文章标题、内容..." clearable
          style="width: 320px" @input="onSearchInput"
          :prefix-icon="useRenderIcon('ep:search')" />
        <el-radio-group v-model="activeStatus" @change="handleStatusChange">
          <el-radio-button v-for="f in statusFilters" :key="f.label" :value="f.value">
            {{ f.label }}
          </el-radio-button>
        </el-radio-group>
        <div class="ml-auto flex items-center gap-2">
          <el-button :type="viewMode === 'card' ? 'primary' : 'default'" size="small" @click="viewMode = 'card'">
            <el-icon><component :is="useRenderIcon('ep:grid')" /></el-icon>
          </el-button>
          <el-button :type="viewMode === 'table' ? 'primary' : 'default'" size="small" @click="viewMode = 'table'">
            <el-icon><component :is="useRenderIcon('ep:list')" /></el-icon>
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- Card View -->
    <div v-if="viewMode === 'card'" v-loading="loading">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <el-card v-for="article in articles" :key="article.id" shadow="hover"
          class="cursor-pointer hover:shadow-md transition-shadow"
          @click="goTo(`/knowledge/detail?id=${article.id}`)">
          <div class="flex items-start justify-between mb-2">
            <el-tag :type="statusTagType(article.status)" size="small">{{ statusLabel(article.status) }}</el-tag>
            <el-dropdown trigger="click" @command="(cmd: string) => cmd === 'delete' && handleDelete(article)">
              <el-icon class="cursor-pointer text-gray-400 hover:text-gray-600">
                <component :is="useRenderIcon('ep:more-filled')" />
              </el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" @click="goTo(`/knowledge/editor?id=${article.id}`)">编辑</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <h3 class="font-semibold text-base mb-2 line-clamp-2">{{ article.title }}</h3>
          <p v-if="article.summary" class="text-sm text-gray-500 line-clamp-3 mb-3">{{ article.summary }}</p>
          <div class="flex items-center justify-between text-xs text-gray-400">
            <span>{{ article.authorUserId || '未知作者' }}</span>
            <span>{{ formatDate(article.publishTime || article.updateTime) }}</span>
          </div>
        </el-card>
      </div>
      <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />
    </div>

    <!-- Table View -->
    <el-card v-else shadow="never" v-loading="loading">
      <el-table :data="articles" @row-click="(row: any) => goTo(`/knowledge/detail?id=${row.id}`)">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="100" />
        <el-table-column label="发布时间" width="140">
          <template #default="{ row }">{{ formatDate(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" text @click.stop="goTo(`/knowledge/editor?id=${row.id}`)">编辑</el-button>
            <el-button size="small" text type="danger" :loading="deleting === row.id" @click.stop="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Pagination -->
    <div class="flex justify-end mt-4" v-if="totalArticles > pageSize">
      <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
        :total="totalArticles" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" />
    </div>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.line-clamp-3 { display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }

/* Knowledge list animations */
:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
}

.article-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}

/* Grid view card animation */
.grid-view .el-card {
  animation: scaleIn 0.4s ease-out forwards;
  opacity: 0;
}

.grid-view .el-card:nth-child(1) { animation-delay: 0.1s; }
.grid-view .el-card:nth-child(2) { animation-delay: 0.15s; }
.grid-view .el-card:nth-child(3) { animation-delay: 0.2s; }
.grid-view .el-card:nth-child(4) { animation-delay: 0.25s; }
.grid-view .el-card:nth-child(5) { animation-delay: 0.3s; }
.grid-view .el-card:nth-child(6) { animation-delay: 0.35s; }
</style>
