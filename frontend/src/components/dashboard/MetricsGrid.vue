<template>
  <section class="metrics-grid">
    <article v-for="item in items" :key="item.title" class="metric-card">
      <span class="chip" :class="item.chipClass">{{ item.tag }}</span>
      <strong>{{ item.value }}</strong>
      <h3>{{ item.title }}</h3>
      <p>{{ item.desc }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import type { MetricItem } from '../../types/dashboard'

defineProps<{
  items: MetricItem[]
}>()
</script>
