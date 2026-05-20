<template>
  <div class="page-stack">
    <section class="metric-grid" v-loading="loading">
      <div class="metric-card">
        <span>当前套餐</span>
        <strong>{{ creditInfo?.planType || '-' }}</strong>
      </div>
      <div class="metric-card">
        <span>剩余额度</span>
        <strong>{{ creditInfo?.remainingCredits ?? '-' }}</strong>
      </div>
      <div class="metric-card">
        <span>到期时间</span>
        <strong class="small-value">{{ creditInfo?.expireTime || '-' }}</strong>
      </div>
      <div class="metric-card">
        <span>流水数量</span>
        <strong>{{ logs.length }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>额度流水</h2>
          <p class="muted">查看注册赠送、报告生成、失败返还和管理员调整记录。</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadCredits">刷新</el-button>
      </div>
      <div class="panel-body">
        <el-table v-if="logs.length || loading" v-loading="loading" :data="logs" stripe>
          <el-table-column prop="createTime" label="时间" min-width="180" />
          <el-table-column prop="operationType" label="操作" min-width="180">
            <template #default="{ row }">
              {{ operationLabel(row.operationType) }}
            </template>
          </el-table-column>
          <el-table-column prop="changeAmount" label="变化" width="120">
            <template #default="{ row }">
              <span :class="amountClass(row.changeAmount)">
                {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="beforeAmount" label="变更前" width="110" />
          <el-table-column prop="afterAmount" label="变更后" width="110" />
          <el-table-column prop="remark" label="备注" min-width="240" show-overflow-tooltip />
        </el-table>
        <EmptyState v-else title="暂无额度流水" description="额度发生变化后，记录会出现在这里。" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

import { getMyCredits, listCreditLogs } from '@/api/credit'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import type { CreditInfo, CreditLog } from '@/types/api'

const userStore = useUserStore()
const loading = ref(false)
const creditInfo = ref<CreditInfo>()
const logs = ref<CreditLog[]>([])

const operationMap: Record<string, string> = {
  REGISTER_GIFT: '注册赠送',
  GENERATE_ANALYSIS_REPORT: '生成审计报告',
  GENERATE_ANALYSIS_REPORT_REFUND: '失败返还',
  ADMIN_ADD: '管理员增加'
}

function operationLabel(type: string) {
  return operationMap[type] || type
}

function amountClass(amount: number) {
  return amount >= 0 ? 'amount-positive' : 'amount-negative'
}

async function loadCredits() {
  loading.value = true
  try {
    const [info, creditLogs] = await Promise.all([getMyCredits(), listCreditLogs()])
    creditInfo.value = info
    logs.value = creditLogs
    userStore.updateCredits(info.remainingCredits)
  } finally {
    loading.value = false
  }
}

onMounted(loadCredits)
</script>

<style scoped>
.amount-positive {
  color: var(--pm-green);
  font-weight: 800;
}

.amount-negative {
  color: var(--pm-red);
  font-weight: 800;
}
</style>
