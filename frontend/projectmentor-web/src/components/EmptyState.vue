<template>
  <div class="empty-state">
    <el-icon :size="42"><Document /></el-icon>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

defineProps<{
  title: string
  description?: string
}>()
</script>
