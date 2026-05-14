<template>
  <RouterView v-slot="{ Component, route }">
    <Transition name="route-fade" mode="out-in">
      <component :is="Component" :key="route.fullPath" />
    </Transition>
  </RouterView>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>
