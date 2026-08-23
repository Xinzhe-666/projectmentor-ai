<template>
  <div class="empty-state" :class="`empty-state--${variant}`" role="status">
    <el-icon :size="20"><Document /></el-icon>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-state-actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    variant?: 'default' | 'compact'
  }>(),
  {
    variant: 'default'
  }
)
</script>

<style scoped>
.empty-state {
  min-height: 180px;
  padding: 28px;
  border: 0;
  border-top: 1px solid var(--pm-stone);
  border-bottom: 1px solid var(--pm-stone);
  border-radius: 0;
  background: var(--pm-surface);
  color: var(--pm-muted);
  text-align: center;
}

.empty-state h3 {
  margin: 10px 0 0;
  color: var(--pm-ink);
  font-size: 16px;
  font-weight: 600;
}

.empty-state p {
  margin: 6px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.6;
}

.empty-state-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.empty-state--compact {
  min-height: 156px;
  justify-items: start;
  padding: 24px 0;
  text-align: left;
}

.empty-state--compact .empty-state-actions {
  justify-content: flex-start;
}
</style>
