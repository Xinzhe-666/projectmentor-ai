<template>
  <section class="panel admin-usage-panel" v-loading="loading">
    <div class="panel-title">
      <div>
        <h3>{{ t('admin.aiUsageTitle') }}</h3>
        <p class="muted">{{ t('admin.aiUsageDesc') }}</p>
      </div>
      <el-button @click="loadOverview">{{ t('common.refresh') }}</el-button>
    </div>

    <div class="panel-body usage-stack">
      <div class="usage-metrics">
        <div v-for="metric in usageMetrics" :key="metric.label" class="usage-metric">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>

      <div class="usage-table-grid">
        <div>
          <h4>{{ t('admin.topModulesToday') }}</h4>
          <el-table :data="overview?.topModulesToday || []" stripe :empty-text="t('common.noData')">
            <el-table-column :label="t('admin.module')" min-width="210">
              <template #default="{ row }">{{ operationLabel(row.module) }}</template>
            </el-table-column>
            <el-table-column prop="aiCalls" :label="t('admin.aiCalls')" width="110" />
            <el-table-column prop="creditsConsumed" :label="t('admin.consumed')" width="130" />
          </el-table>
        </div>

        <div>
          <h4>{{ t('admin.topUsersToday') }}</h4>
          <el-table :data="overview?.topUsersToday || []" stripe :empty-text="t('common.noData')">
            <el-table-column prop="username" :label="t('common.username')" min-width="130" />
            <el-table-column prop="email" :label="t('common.email')" min-width="190" show-overflow-tooltip />
            <el-table-column prop="aiCalls" :label="t('admin.aiCalls')" width="100" />
            <el-table-column prop="creditsConsumed" :label="t('admin.consumed')" width="120" />
          </el-table>
        </div>
      </div>

      <div>
        <h4>{{ t('admin.recentAiCreditLogs') }}</h4>
        <el-table :data="overview?.recentCreditLogs || []" stripe :empty-text="t('admin.noCreditLogs')">
          <el-table-column prop="userId" :label="t('common.userId')" width="100" />
          <el-table-column prop="username" :label="t('common.username')" min-width="120" />
          <el-table-column :label="t('admin.module')" min-width="210">
            <template #default="{ row }">{{ operationLabel(row.module) }}</template>
          </el-table-column>
          <el-table-column prop="type" :label="t('admin.transactionType')" width="110">
            <template #default="{ row }">{{ creditTypeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="amount" :label="t('credits.change')" width="100">
            <template #default="{ row }">
              <el-tag :type="row.amount > 0 ? 'success' : 'warning'" effect="light">
                {{ row.amount > 0 ? `+${row.amount}` : row.amount }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="balanceAfter" :label="t('admin.balanceAfter')" width="120" />
          <el-table-column prop="createdAt" :label="t('credits.time')" min-width="170" />
        </el-table>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { getAiUsageOverview } from '@/api/admin'
import type { AdminAiUsageOverview } from '@/types/api'

const { t, te } = useI18n()
const loading = ref(false)
const overview = ref<AdminAiUsageOverview | null>(null)

const usageMetrics = computed(() => [
  { label: t('admin.aiCallsToday'), value: overview.value?.todayAiCalls ?? '-' },
  { label: t('admin.creditsConsumedToday'), value: overview.value?.todayCreditsConsumed ?? '-' },
  { label: t('admin.refundsToday'), value: overview.value?.todayRefundCount ?? '-' },
  { label: t('admin.creditsRefundedToday'), value: overview.value?.todayRefundCredits ?? '-' },
  { label: t('admin.totalAiCalls'), value: overview.value?.totalAiCalls ?? '-' },
  { label: t('admin.totalCreditsConsumed'), value: overview.value?.totalCreditsConsumed ?? '-' },
  { label: t('admin.totalRefunds'), value: overview.value?.totalRefundCount ?? '-' },
  { label: t('admin.totalCreditsRefunded'), value: overview.value?.totalRefundCredits ?? '-' }
])

function operationLabel(operation: string) {
  const key = `credits.operations.${operation}`
  return te(key) ? t(key) : operation
}

function creditTypeLabel(type?: string) {
  if (!type) {
    return '-'
  }
  const key = `admin.creditTypes.${type}`
  return te(key) ? t(key) : type
}

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await getAiUsageOverview()
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<style scoped>
.admin-usage-panel {
  overflow: hidden;
}

.usage-stack {
  display: grid;
  gap: 22px;
}

.usage-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.usage-metric {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(31, 111, 235, 0.12);
  border-radius: 10px;
  background: linear-gradient(145deg, rgba(238, 246, 255, 0.9), rgba(240, 253, 250, 0.7));
}

.usage-metric span {
  color: #667085;
  font-size: 13px;
  font-weight: 700;
}

.usage-metric strong {
  color: #182230;
  font-size: 26px;
}

.usage-table-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.usage-stack h4 {
  margin: 0 0 10px;
}

@media (max-width: 900px) {
  .usage-metrics,
  .usage-table-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .usage-metrics,
  .usage-table-grid {
    grid-template-columns: 1fr;
  }
}
</style>
