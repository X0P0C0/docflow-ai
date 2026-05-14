<template>
  <article class="panel">
    <div class="panel-head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
      <RouterLink to="/knowledge/articles">{{ actionText }}</RouterLink>
    </div>

    <div v-if="loading" class="state-box">正在拉取知识文章...</div>
    <ErrorTraceNotice
      v-else-if="errorMessage"
      :message="`${errorMessage}，当前先展示本地演示数据。`"
      :trace-id="errorTraceId"
    />

    <div class="article-list">
      <RouterLink
        v-for="article in items"
        :key="article.id"
        class="article-card article-link"
        :to="`/knowledge/articles/${article.id}`"
      >
        <span class="chip" :class="article.tagClass">{{ article.tag }}</span>
        <h4>{{ article.title }}</h4>
        <p>{{ article.summary }}</p>
        <div class="article-meta">
          <span>{{ article.author }}</span>
          <span>{{ article.time }}</span>
          <span>{{ article.metric }}</span>
        </div>
      </RouterLink>
    </div>
  </article>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import ErrorTraceNotice from '../common/ErrorTraceNotice.vue'
import type { KnowledgeArticleItem } from '../../types/dashboard'

defineProps<{
  items: KnowledgeArticleItem[]
  loading: boolean
  errorMessage: string
  errorTraceId: string
  title: string
  description: string
  actionText: string
}>()
</script>
