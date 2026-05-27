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
  <div>
    <div class="mb-5 flex items-center gap-3">
      <el-button text circle @click="goBack">
        <component :is="useRenderIcon('ep:arrow-left', { width: '20px', height: '20px' })" />
      </el-button>
      <div>
        <h2 class="text-xl font-bold text-gray-900 dark:text-white">新建工单</h2>
        <p class="text-xs text-gray-500 mt-0.5">提交新的问题或任务，填写详细信息以便快速处理</p>
      </div>
    </div>

    <el-card shadow="never" class="max-w-3xl">
      <el-form :model="form" label-position="top">
        <el-form-item label="工单标题" required>
          <el-input v-model="form.title" placeholder="简要描述问题或需求" maxlength="200" show-word-limit size="large" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="opt in ticketPriorityOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="opt in ticketTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" style="width: 100%">
                <el-option v-for="opt in ticketCategoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细描述" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="详细描述问题现象、复现步骤、期望结果等" />
        </el-form-item>

        <div class="flex gap-2 pt-2">
          <el-button type="primary" :loading="loading" @click="submit" :disabled="!form.title.trim() || !form.content.trim()">
            <component :is="useRenderIcon('ep:promotion', { width: '14px', height: '14px' })" class="mr-1" />
            提交工单
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>