<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getSystemHealth, type SystemHealth } from "@/api/system";

defineOptions({ name: "DocflowSystemHealth" });

const loading = ref(false);
const health = ref<SystemHealth | null>(null);

async function loadHealth() {
  loading.value = true;
  try {
    const { code, data } = await getSystemHealth();
    if (code === 200) health.value = data;
  } catch { /* silently fail */ }
  finally { loading.value = false; }
}

function statusColor(status: string) {
  if (status === "UP" || status === "HEALTHY" || status === "CLOSED") return "#10b981";
  if (status === "DEGRADED" || status === "HALF_OPEN") return "#f59e0b";
  return "#ef4444";
}

function statusType(status: string) {
  if (status === "UP" || status === "HEALTHY" || status === "CLOSED") return "success";
  if (status === "DEGRADED" || status === "HALF_OPEN") return "warning";
  return "danger";
}

onMounted(loadHealth);
</script>

<template>
  <div class="p-4" v-loading="loading">
    <div class="hp-top">
      <h1 class="hp-title">系统健康监控</h1>
      <el-button @click="loadHealth" :loading="loading">刷新</el-button>
    </div>

    <template v-if="health">
      <!-- Overview -->
      <div class="hp-overview">
        <div class="hp-status-badge" :style="{ background: statusColor(health.status) + '15', color: statusColor(health.status), borderColor: statusColor(health.status) + '40' }">
          <span class="hp-status-dot" :style="{ background: statusColor(health.status) }" />
          {{ health.status }}
        </div>
        <span class="hp-timestamp">更新于 {{ new Date(health.timestamp).toLocaleString() }}</span>
      </div>

      <!-- JVM -->
      <div class="hp-section">
        <h2 class="hp-section-title">JVM 内存</h2>
        <div class="hp-card">
          <div class="hp-metric">
            <span class="hp-metric-label">堆内存使用</span>
            <span class="hp-metric-value">{{ health.jvm.heapUsedMB }} MB / {{ health.jvm.heapMaxMB }} MB</span>
          </div>
          <el-progress
            :percentage="Math.round((health.jvm.heapUsedMB / health.jvm.heapMaxMB) * 100)"
            :color="health.jvm.heapUsedMB / health.jvm.heapMaxMB > 0.8 ? '#ef4444' : '#10b981'"
          />
          <div class="hp-metric mt-3">
            <span class="hp-metric-label">运行时间</span>
            <span class="hp-metric-value">{{ Math.round(health.jvm.uptimeMinutes) }} 分钟</span>
          </div>
        </div>
      </div>

      <!-- Redis -->
      <div class="hp-section">
        <h2 class="hp-section-title">Redis</h2>
        <div class="hp-card">
          <div class="hp-metric">
            <span class="hp-metric-label">连接状态</span>
            <el-tag :type="statusType(health.redis.status)" size="small" effect="light">{{ health.redis.status }}</el-tag>
          </div>
        </div>
      </div>

      <!-- Circuit Breakers -->
      <div class="hp-section" v-if="Object.keys(health.circuitBreakers).length">
        <h2 class="hp-section-title">熔断器状态</h2>
        <div class="hp-cb-grid">
          <div v-for="(cb, name) in health.circuitBreakers" :key="name" class="hp-card">
            <div class="hp-cb-header">
              <span class="hp-cb-name">{{ name }}</span>
              <el-tag :type="statusType(cb.state)" size="small" effect="light">{{ cb.state }}</el-tag>
            </div>
            <div class="hp-metric">
              <span class="hp-metric-label">失败率</span>
              <span class="hp-metric-value">{{ cb.failureRate }}%</span>
            </div>
            <div class="hp-metric">
              <span class="hp-metric-label">缓冲调用数</span>
              <span class="hp-metric-value">{{ cb.bufferedCalls }}</span>
            </div>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="无法获取健康信息" />
  </div>
</template>

<style scoped>
@import "@/styles/animations.css";
.hp-top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.hp-title {
  font-size: 22px; font-weight: 800; color: #0d253d; margin: 0;
}
.hp-overview {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px;
}
.hp-status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 20px;
  font-size: 13px; font-weight: 700; border: 1px solid;
}
.hp-status-dot {
  width: 8px; height: 8px; border-radius: 50%;
}
.hp-timestamp {
  font-size: 12px; color: #94a3b8;
}
.hp-section {
  margin-bottom: 20px;
}
.hp-section-title {
  font-size: 15px; font-weight: 700; color: #0d253d;
  margin: 0 0 10px 0;
}
.hp-card {
  background: #fff; border: 1px solid #e8ecf1; border-radius: 10px;
  padding: 16px;
}
.hp-cb-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}
.hp-cb-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.hp-cb-name {
  font-size: 14px; font-weight: 600; color: #0d253d;
}
.hp-metric {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px;
}
.hp-metric-label {
  font-size: 13px; color: #64748d;
}
.hp-metric-value {
  font-size: 13px; font-weight: 600; color: #0d253d;
}

.dark .hp-title,
.dark .hp-section-title,
.dark .hp-cb-name { color: #f1f5f9; }
.dark .hp-card { background: #1e293b; border-color: #334155; }
.dark .hp-metric-value { color: #f1f5f9; }

/* System page animations */
:deep(.el-card) {
  animation: fadeInUp 0.4s ease-out forwards;
}

:deep(.el-table) {
  animation: fadeInUp 0.5s ease-out forwards;
  animation-delay: 0.1s;
  opacity: 0;
}

/* Health status indicator */
.status-healthy {
  animation: pulse 2s ease-in-out infinite;
  color: #10b981;
}

/* Feature toggle animation */
:deep(.el-switch) {
  transition: all 0.3s ease;
}

:deep(.el-switch:hover) {
  transform: scale(1.05);
}

</style>
