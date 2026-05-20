<template>
  <div class="page-stack">
    <section class="metric-grid" v-if="creditInfo">
      <div class="metric-card">
        <span>当前套餐</span>
        <strong>{{ creditInfo.planType }}</strong>
      </div>
      <div class="metric-card">
        <span>剩余额度</span>
        <strong>{{ creditInfo.remainingCredits }}</strong>
      </div>
      <div class="metric-card">
        <span>用户 ID</span>
        <strong>{{ creditInfo.userId }}</strong>
      </div>
      <div class="metric-card">
        <span>到期时间</span>
        <strong class="small-value">{{ creditInfo.expireTime || '-' }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>额度流水</h2>
          <p class="muted">查看 AI 审计和面试深挖消耗记录。</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadCredits">刷新</el-button>
      </div>
      <div class="panel-body">
        <el-table v-loading="loading" :data="logs" stripe>
          <el-table-column prop="createTime" label="时间" min-width="180" />
          <el-table-column prop="operationType" label="操作" width="160" />
          <el-table-column prop="changeAmount" label="变化" width="100" />
          <el-table-column prop="beforeAmount" label="变更前" width="100" />
          <el-table-column prop="afterAmount" label="变更后" width="100" />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

import { getMyCredits, listCreditLogs } from '@/api/credit'
import { useUserStore } from '@/stores/user'
import type { CreditInfo, CreditLog } from '@/types/api'

const userStore = useUserStore()
const loading = ref(false)
const creditInfo = ref<CreditInfo>()
const logs = ref<CreditLog[]>([])

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
.small-value {
  font-size: 16px;
}
</style>
