<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { useRenderIcon } from "@/components/ReIcon/src/hooks";
import { createTicket } from "@/api/tickets";
import { ticketPriorityOptions, ticketTypeOptions, ticketCategoryOptions } from "@/constants/tickets";

defineOptions({ name: "DocflowTicketCreate" });

const router = useRouter();
const loading = ref(false);

const form = ref({
  title: "",
  priority: 2,
  type: "INCIDENT",
  categoryId: 1,
  content: ""
});

async function submit() {
  if (!form.value.title.trim() || !form.value.content.trim()) return;
  loading.value = true;
  try {
    const { code, message: errorMessage, data } = await createTicket({
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      type: form.value.type,
      categoryId: form.value.categoryId,
      priority: form.value.priority
    });
    if (code !== 200) throw new Error(errorMessage || "创建失败");
    message("工单创建成功", { type: "success" });
    router.push(`/tickets/${data.id}`);
  } catch (error) {
    message(error instanceof Error ? error.message : "创建失败", { type: "error" });
  } finally {
    loading.value = false;
  }
}

function goBack() { router.push("/tickets/list"); }
</script>

<template>
  <div class="tc-page">
    <!-- Header -->
    <div class="tc-top">
      <el-button text circle @click="goBack" class="tc-back">
        <component :is="useRenderIcon('ep:arrow-left', { width: '18px', height: '18px' })" />
      </el-button>
      <div>
        <h2 class="tc-hero-title">新建工单</h2>
        <p class="tc-hero-sub">提交新的问题或任务，填写详细信息以便快速处理</p>
      </div>
    </div>

    <!-- Form Card -->
    <div class="tc-form-card">
      <el-form :model="form" label-position="top" class="tc-form">
        <el-form-item label="工单标题" required>
          <el-input
            v-model="form.title"
            placeholder="简要描述问题或需求"
            maxlength="200"
            show-word-limit
            size="large"
            class="tc-title-input"
          />
        </el-form-item>

        <div class="tc-row-3">
          <el-form-item label="优先级">
            <el-select v-model="form.priority" style="width: 100%">
              <el-option
                v-for="opt in ticketPriorityOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.type" style="width: 100%">
              <el-option
                v-for="opt in ticketTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" style="width: 100%">
              <el-option
                v-for="opt in ticketCategoryOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="详细描述" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="详细描述问题现象、复现步骤、期望结果等"
          />
        </el-form-item>

        <div class="tc-actions">
          <el-button @click="goBack">取消</el-button>
          <el-button
            type="primary"
            :loading="loading"
            @click="submit"
            :disabled="!form.title.trim() || !form.content.trim()"
          >
            <component :is="useRenderIcon('ep:promotion', { width: '14px', height: '14px' })" class="mr-1" />
            提交工单
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* === Ticket Create: Stripe Design Language === */
.tc-page { max-width: 720px; }

.tc-top {
  display: flex; align-items: center; gap: 10px; margin-bottom: 16px;
}
.tc-back { color: #64748d; }
.tc-back:hover { color: #0d253d; }
.tc-hero-title {
  font-size: 20px; font-weight: 700; color: #0d253d; margin: 0; line-height: 1;
}
.tc-hero-sub {
  font-size: 13px; color: #94a3b8; margin: 4px 0 0;
}

.tc-form-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 8px;
  padding: 24px 28px;
}
.tc-form :deep(.el-form-item__label) {
  font-size: 13px; font-weight: 600; color: #374151; padding-bottom: 4px;
}
.tc-form :deep(.el-input__wrapper),
.tc-form :deep(.el-textarea__inner) {
  border-radius: 6px;
}
.tc-form :deep(.el-select) { width: 100%; }

.tc-row-3 {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
}

.tc-title-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  border-bottom: 2px solid #e5e7eb;
  border-radius: 0;
  padding: 0 0 4px 0;
}
.tc-title-input :deep(.el-input__wrapper:hover) { border-bottom-color: #533afd; }
.tc-title-input :deep(.el-input__wrapper.is-focus) { border-bottom-color: #533afd; box-shadow: none !important; }
.tc-title-input :deep(.el-input__inner) { font-size: 16px; font-weight: 600; }

.tc-actions {
  display: flex; gap: 8px; justify-content: flex-end;
  padding-top: 8px; border-top: 1px solid #f1f5f9; margin-top: 4px;
}
.tc-actions :deep(.el-button--primary) {
  background: #533afd; border-color: #533afd;
}
.tc-actions :deep(.el-button--primary:hover) {
  background: #4338ca; border-color: #4338ca;
}

.dark .tc-hero-title { color: #f1f5f9; }
.dark .tc-hero-sub { color: #94a3b8; }
.dark .tc-back { color: #94a3b8; }
.dark .tc-back:hover { color: #f1f5f9; }
.dark .tc-form-card { background: #1e293b; border-color: #334155; }
.dark .tc-form :deep(.el-form-item__label) { color: #d1d5db; }
.dark .tc-title-input :deep(.el-input__wrapper) { border-bottom-color: #374151; }
.dark .tc-title-input :deep(.el-input__wrapper:hover),
.dark .tc-title-input :deep(.el-input__wrapper.is-focus) { border-bottom-color: #818cf8; }
.dark .tc-actions { border-top-color: #334155; }
</style>