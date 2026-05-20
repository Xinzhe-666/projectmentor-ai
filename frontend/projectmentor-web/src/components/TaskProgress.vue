<template>
  <div class="task-progress">
    <div class="task-progress-head">
      <div>
        <p class="eyebrow">Analysis Task</p>
        <h3>{{ task?.status || '未开始' }}</h3>
      </div>
      <el-tag :type="statusType" effect="light">{{ statusText }}</el-tag>
    </div>

    <el-progress
      :percentage="progress"
      :status="progressStatus"
      :stroke-width="12"
      striped
      striped-flow
    />

    <p v-if="task?.message" class="muted">{{ task.message }}</p>
    <el-alert
      v-if="task?.failReason"
      :title="task.failReason"
      type="error"
      show-icon
      :closable="false"
    />
    <el-alert
      v-if="task?.reportId"
      :title="`报告已生成：#${task.reportId}`"
      type="success"
      show-icon
      :closable="false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { AnalysisTask } from '@/types/api'

const props = defineProps<{
  task?: AnalysisTask | null
}>()

const progress = computed(() => {
  const value = props.task?.progress ?? 0
  return Math.max(0, Math.min(100, Math.round(value)))
})

const statusText = computed(() => {
  const statusMap: Record<string, string> = {
    PENDING: '排队中',
    RUNNING: '分析中',
    SUCCESS: '已完成',
    FAILED: '失败'
  }

  return statusMap[props.task?.status || ''] || '等待任务'
})

const statusType = computed(() => {
  if (props.task?.status === 'SUCCESS') {
    return 'success'
  }

  if (props.task?.status === 'FAILED') {
    return 'danger'
  }

  if (props.task?.status === 'RUNNING') {
    return 'primary'
  }

  return 'info'
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
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.task-progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.task-progress h3 {
  margin: 4px 0 0;
}
</style>
