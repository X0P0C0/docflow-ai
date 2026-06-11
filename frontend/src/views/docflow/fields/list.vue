<script setup lang="ts">
import { onMounted, ref } from "vue";
import { message } from "@/utils/message";
import { getCustomFields, createCustomField, updateCustomField, deleteCustomField, type CustomField } from "@/api/customField";

defineOptions({ name: "DocflowCustomFields" });
const loading = ref(false);
const fields = ref<CustomField[]>([]);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = ref({ name: "", fieldType: "TEXT", required: false, options: "", categoryId: null as number | null, sort: 0 });

const fieldTypes = [
  { label: "文本", value: "TEXT" },
  { label: "数字", value: "NUMBER" },
  { label: "日期", value: "DATE" },
  { label: "下拉选择", value: "SELECT" },
  { label: "多选", value: "MULTI_SELECT" },
  { label: "备注", value: "TEXTAREA" }
];

async function loadFields() {
  loading.value = true;
  try { const { code, data } = await getCustomFields(); if (code === 200) fields.value = data; } catch {}
  finally { loading.value = false; }
}

function openCreate() {
  editingId.value = null;
  form.value = { name: "", fieldType: "TEXT", required: false, options: "", categoryId: null, sort: 0 };
  dialogVisible.value = true;
}
function openEdit(f: CustomField) {
  editingId.value = f.id;
  form.value = { name: f.name, fieldType: f.fieldType, required: f.required, options: f.options || "", categoryId: f.categoryId || null, sort: f.sort };
  dialogVisible.value = true;
}

async function handleSave() {
  if (!form.value.name.trim()) { message("请填写名称", { type: "warning" }); return; }
  try {
    if (editingId.value) { await updateCustomField(editingId.value, form.value); }
    else { await createCustomField(form.value); }
    dialogVisible.value = false;
    message(editingId.value ? "更新成功" : "创建成功", { type: "success" });
    loadFields();
  } catch (e) { message("操作失败", { type: "error" }); }
}

async function handleDelete(f: CustomField) {
  try { await deleteCustomField(f.id); message("已删除", { type: "success" }); loadFields(); }
  catch { message("删除失败", { type: "error" }); }
}

onMounted(loadFields);
</script>
<template>
  <div class="p-4">
    <div class="mg-top"><h1 class="mg-title">自定义字段</h1><el-button type="primary" @click="openCreate">+ 新建字段</el-button></div>
    <div class="mg-card" v-loading="loading">
      <div v-for="f in fields" :key="f.id" class="mg-item">
        <div class="mg-item-header">
          <span class="mg-item-name">{{ f.name }}</span>
          <div class="flex items-center gap-2">
            <el-tag type="info" size="small" effect="plain">{{ fieldTypes.find(t => t.value === f.fieldType)?.label || f.fieldType }}</el-tag>
            <el-tag v-if="f.required" size="small" type="warning">必填</el-tag>
            <el-button size="small" text @click="openEdit(f)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(f)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && !fields.length" description="暂无自定义字段" />
    </div>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑字段' : '新建字段'" width="480px">
      <div class="mg-form">
        <div class="mg-field"><label>名称 *</label><el-input v-model="form.name" /></div>
        <div class="mg-field"><label>类型</label>
          <el-select v-model="form.fieldType" class="w-full"><el-option v-for="t in fieldTypes" :key="t.value" :label="t.label" :value="t.value" /></el-select>
        </div>
        <div class="mg-field" v-if="form.fieldType === 'SELECT' || form.fieldType === 'MULTI_SELECT'">
          <label>选项 (逗号分隔)</label><el-input v-model="form.options" placeholder="选项1,选项2,选项3" />
        </div>
        <div class="flex gap-3 items-center">
          <el-checkbox v-model="form.required">必填字段</el-checkbox>
          <div class="mg-field flex-1"><label>排序</label><el-input-number v-model="form.sort" :min="0" :max="100" /></div>
        </div>
      </div>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">{{ editingId ? '保存' : '创建' }}</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
.mg-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.mg-title { font-size: 22px; font-weight: 800; color: #0d253d; margin: 0; }
.mg-card { background: #fff; border: 1px solid #e8ecf1; border-radius: 10px; overflow: hidden; }
.mg-item { padding: 14px 20px; border-bottom: 1px solid #f1f5f9; }
.mg-item:last-child { border-bottom: none; }
.mg-item-header { display: flex; align-items: center; justify-content: space-between; }
.mg-item-name { font-size: 14px; font-weight: 600; color: #0d253d; }
.mg-form { display: flex; flex-direction: column; gap: 14px; }
.mg-field { display: flex; flex-direction: column; gap: 4px; }
.mg-field label { font-size: 13px; font-weight: 600; color: #374151; }
.dark .mg-title { color: #f1f5f9; }
.dark .mg-card { background: #1e293b; border-color: #334155; }
.dark .mg-item { border-bottom-color: #334155; }
.dark .mg-item-name { color: #f1f5f9; }
</style>
