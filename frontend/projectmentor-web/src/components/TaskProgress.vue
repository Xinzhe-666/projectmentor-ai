<template>
  <div class="task-progress" aria-live="polite">
    <div class="task-progress-head">
      <div>
        <h3>{{ t('task.eyebrow') }}</h3>
        <p>{{ task?.message || statusText }}</p>
      </div>
      <div class="task-status-value">
        <StatusLabel :status="task?.status" :label="statusText" />
        <strong>{{ progress }}%</strong>
      </div>
    </div>

    <el-progress
      :percentage="progress"
      :status="progressStatus"
      :stroke-width="4"
      :show-text="false"
    />

    <div v-if="interrupted" class="task-message task-message--error" role="alert">
      <span>{{ t('projects.v5.errors.polling') }}</span>
      <el-button text @click="emit('retry')">{{ t('projects.v5.retry') }}</el-button>
    </div>
    <p v-else-if="task?.failReason" class="task-message task-message--error" role="alert">{{ task.failReason }}</p>
    <p v-else-if="task?.reportId" class="task-message">{{ t('task.reportGenerated', { id: task.reportId }) }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import StatusLabel from '@/components/StatusLabel.vue'
import type { AnalysisTask } from '@/types/api'

const props = defineProps<{
  task?: AnalysisTask | null
  interrupted?: boolean
}>()
const emit = defineEmits<{
  (event: 'retry'): void
}>()

const { t } = useI18n()

const progress = computed(() => {
  const value = props.task?.progress ?? 0
  return Math.max(0, Math.min(100, Math.round(value)))
})

const statusText = computed(() => {
  const statusMap: Record<string, string> = {
    PENDING: t('status.pending'),
    RUNNING: t('status.running'),
    SUCCESS: t('status.success'),
    FAILED: t('status.failed')
  }

  return statusMap[props.task?.status || ''] || t('status.waiting')
})

const progressStatus = computed(() => {
  if (props.task?.status === 'SUCCESS') {
    return 'success'
  }

  if (props.task?.status === 'FAILED') {
    return 'exception'
  }

  return undefined
})
</script>

<style scoped>
.task-progress {
  display: grid;
  gap: 10px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--pm-stone);
}

.task-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.task-progress h3 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 13px;
  font-weight: 600;
}

.task-progress-head p {
  margin: 5px 0 0;
  color: var(--pm-muted);
  font-size: 12px;
}

.task-status-value {
  display: flex;
  align-items: center;
  gap: 16px;
}

.task-status-value strong {
  color: var(--pm-ink);
  font-family: var(--pm-font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.task-progress :deep(.el-progress-bar__outer) {
  border-radius: 0;
  background: var(--pm-stone);
}

.task-progress :deep(.el-progress-bar__inner) {
  border-radius: 0;
  background: var(--pm-primary);
}

.task-message {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 12px;
  line-height: 1.55;
}

.task-message--error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--pm-risk);
}
</style>
