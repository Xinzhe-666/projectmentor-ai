<template>
  <div class="empty-state">
    <el-icon :size="42"><Document /></el-icon>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <div v-if="$slots.default" class="empty-state-actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.empty-state p {
  max-width: 620px;
  line-height: 1.7;
}

.empty-state-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
