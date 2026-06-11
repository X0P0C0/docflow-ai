<script setup lang="ts">
import { ref, onMounted } from "vue";
import { message } from "@/utils/message";

interface FeatureFlag {
  key: string;
  label: string;
  enabled: boolean;
  description: string;
}

const loading = ref(false);
const flags = ref<FeatureFlag[]>([
  { key: "feature.ai-suggestion", label: "AI 建议", enabled: true, description: "启用 AI 智能回复建议功能" },
  { key: "feature.auto-assign", label: "自动分配", enabled: true, description: "根据工单类型自动分配处理人" },
  { key: "feature.satisfaction", label: "满意度评价", enabled: true, description: "允许用户对已解决工单进行满意度评分" },
  { key: "feature.knowledge-sync", label: "知识同步", enabled: false, description: "工单解决后自动同步到知识库" },
  { key: "feature.webhook", label: "Webhook 通知", enabled: false, description: "支持外部 Webhook 事件推送" },
  { key: "feature.email-notify", label: "邮件通知", enabled: false, description: "工单变更时发送邮件通知" }
]);

async function toggleFlag(flag: FeatureFlag) {
  loading.value = true;
  try {
    flag.enabled = !flag.enabled;
    message(`已${flag.enabled ? "启用" : "禁用"}: ${flag.label}`, { type: "success" });
  } catch (e) {
    flag.enabled = !flag.enabled;
    message("操作失败", { type: "error" });
  } finally {
    loading.value = false;
  }
}

onMounted(() => {});
</script>

<template>
  <div class="p-4">
    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-semibold">功能开关</span>
          <el-tag type="info" size="small">动态配置</el-tag>
        </div>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div v-for="flag in flags" :key="flag.key"
          class="flex items-start gap-4 p-4 rounded-lg border transition-all hover:shadow-sm"
          :class="flag.enabled ? 'border-green-200 bg-green-50' : 'border-gray-200 bg-gray-50'">
          <el-switch v-model="flag.enabled" :loading="loading" @change="toggleFlag(flag)" />
          <div class="flex-1">
            <div class="font-medium">{{ flag.label }}</div>
            <div class="text-sm text-gray-500 mt-1">{{ flag.description }}</div>
          </div>
          <el-tag :type="flag.enabled ? 'success' : 'info'" size="small">
            {{ flag.enabled ? '已启用' : '已禁用' }}
          </el-tag>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";

:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
}
</style>
