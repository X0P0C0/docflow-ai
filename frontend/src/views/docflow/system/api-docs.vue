<script setup lang="ts">
import { onMounted } from "vue";

onMounted(() => {
  window.open("http://localhost:8081/swagger-ui.html", "_blank");
  window.history.back();
});
</script>

<template>
  <div class="flex items-center justify-center h-full">
    <el-empty description="正在打开 API 文档..." />
  </div>
</template>
