<template>
  <div :class="wrapperClass">
    <span>{{ message }}</span>
    <button
      v-if="traceId"
      class="error-trace-button"
      type="button"
      @click="copyTraceId"
    >
      {{ copied ? '已复制追踪号' : `复制追踪号 ${traceId}` }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  message: string
  traceId?: string
  inline?: boolean
}>(), {
  traceId: '',
  inline: false,
})

const copied = ref(false)
const wrapperClass = computed(() => (
  props.inline
    ? 'error-trace error-trace-inline'
    : 'state-box state-warning error-trace'
))

watch(
  () => props.traceId,
  () => {
    copied.value = false
  },
)

async function copyTraceId() {
  if (!props.traceId) {
    return
  }

  try {
    await navigator.clipboard.writeText(props.traceId)
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 1800)
  } catch {
    copied.value = false
  }
}
</script>

<style scoped>
.error-trace {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.error-trace-inline {
  color: #b42318;
  font-size: 0.9rem;
}

.error-trace-button {
  border: 0;
  border-radius: 999px;
  padding: 0.35rem 0.7rem;
  background: rgba(255, 255, 255, 0.72);
  color: inherit;
  font: inherit;
  cursor: pointer;
  transition: transform 180ms ease, background 180ms ease;
}

.error-trace-button:hover {
  transform: translateY(-1px);
  background: rgba(255, 255, 255, 0.92);
}
</style>
