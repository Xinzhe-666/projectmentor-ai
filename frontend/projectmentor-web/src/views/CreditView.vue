<template>
  <div class="page-stack">
    <section class="panel credit-intro pm-command-panel">
      <div class="panel-body credit-intro-body">
        <div>
          <p class="eyebrow">{{ t('credits.guideEyebrow') }}</p>
          <h2>{{ t('credits.guideTitle') }}</h2>
          <p class="muted">{{ t('credits.guideDesc') }}</p>
        </div>
        <div class="credit-intro-tags">
          <el-tag type="success" effect="light">{{ t('credits.registrationGift') }}</el-tag>
          <el-tag type="info" effect="light">{{ t('credits.betaNotice') }}</el-tag>
        </div>
      </div>
    </section>

    <section class="metric-grid" v-loading="loading">
      <div class="metric-card">
        <span>{{ t('credits.plan') }}</span>
        <strong>{{ creditInfo?.planType || '-' }}</strong>
      </div>
      <div class="metric-card">
        <span>{{ t('credits.remaining') }}</span>
        <strong>{{ creditInfo?.remainingCredits ?? '-' }}</strong>
      </div>
      <div class="metric-card">
        <span>{{ t('credits.expire') }}</span>
        <strong class="small-value">{{ creditInfo?.expireTime || '-' }}</strong>
      </div>
      <div class="metric-card">
        <span>{{ t('credits.logCount') }}</span>
        <strong>{{ logs.length }}</strong>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('credits.freeTitle') }}</h2>
          <p class="muted">{{ t('credits.freeDesc') }}</p>
        </div>
        <el-tag type="success" effect="light">{{ t('credits.ruleFree') }}</el-tag>
      </div>
      <div class="panel-body">
        <div class="free-feature-grid">
          <article v-for="item in freeFeatures" :key="item">
            <span aria-hidden="true">✓</span>
            <strong>{{ item }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('credits.standardsTitle') }}</h2>
          <p class="muted">{{ t('credits.standardsDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <div class="credit-standard-grid">
          <article v-for="item in creditStandards" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ t('credits.creditUnit', { count: item.cost }) }}</strong>
          </article>
        </div>
        <el-alert
          class="credit-refund-alert"
          :title="t('credits.refundNotice')"
          type="info"
          show-icon
          :closable="false"
        />
        <div class="credit-policy-notes">
          <el-tag type="success" effect="light">{{ t('credits.ruleFree') }}</el-tag>
          <el-tag type="info" effect="light">{{ t('credits.onlyAiConsumes') }}</el-tag>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('credits.title') }}</h2>
          <p class="muted">{{ t('credits.desc') }}</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadCredits">{{ t('common.refresh') }}</el-button>
      </div>
      <div class="panel-body">
        <el-table v-if="logs.length || loading" v-loading="loading" :data="logs" stripe>
          <el-table-column prop="createTime" :label="t('credits.time')" min-width="180" />
          <el-table-column prop="operationType" :label="t('credits.operation')" min-width="180">
            <template #default="{ row }">
              {{ operationLabel(row.operationType) }}
            </template>
          </el-table-column>
          <el-table-column prop="changeAmount" :label="t('credits.change')" width="120">
            <template #default="{ row }">
              <span :class="amountClass(row.changeAmount)">
                {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="beforeAmount" :label="t('credits.before')" width="110" />
          <el-table-column prop="afterAmount" :label="t('credits.after')" width="110" />
          <el-table-column prop="remark" :label="t('credits.remark')" min-width="240" show-overflow-tooltip />
        </el-table>
        <EmptyState v-else :title="t('credits.emptyTitle')" :description="t('credits.emptyDesc')">
          <el-button type="primary" :icon="Refresh" @click="loadCredits">{{ t('credits.emptyAction') }}</el-button>
        </EmptyState>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh } from '@element-plus/icons-vue'

import { getMyCredits, listCreditLogs } from '@/api/credit'
import EmptyState from '@/components/EmptyState.vue'
import { AI_CREDIT_COSTS } from '@/constants/creditCosts'
import { useUserStore } from '@/stores/user'
import type { CreditInfo, CreditLog } from '@/types/api'

const userStore = useUserStore()
const { t } = useI18n()
const loading = ref(false)
const creditInfo = ref<CreditInfo>()
const logs = ref<CreditLog[]>([])
const freeFeatures = computed(() => [
  t('credits.freeFeatures.ruleScan'),
  t('credits.freeFeatures.upload'),
  t('credits.freeFeatures.projectManagement'),
  t('credits.freeFeatures.dashboard'),
  t('credits.freeFeatures.history'),
  t('credits.freeFeatures.share')
])
const creditStandards = computed(() => [
  { label: t('credits.costs.auditReport'), cost: AI_CREDIT_COSTS.AUDIT_REPORT },
  { label: t('credits.costs.claimEvidence'), cost: AI_CREDIT_COSTS.CLAIM_EVIDENCE },
  { label: t('credits.costs.projectQa'), cost: AI_CREDIT_COSTS.PROJECT_QA },
  { label: t('credits.costs.hallucinationCheck'), cost: AI_CREDIT_COSTS.HALLUCINATION_CHECK },
  { label: t('credits.costs.interviewSession'), cost: AI_CREDIT_COSTS.INTERVIEW_SESSION },
  { label: t('credits.costs.resumeOptimize'), cost: AI_CREDIT_COSTS.RESUME_OPTIMIZE }
])

function operationLabel(type: string) {
  const key = `credits.operations.${type}`
  const label = t(key)
  return label === key ? type : label
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
.credit-intro-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.credit-intro-body h2 {
  margin: 8px 0;
}

.credit-intro-tags {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.free-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.free-feature-grid article {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px;
  border: 1px solid rgba(20, 184, 166, 0.18);
  border-radius: 8px;
  background: rgba(240, 253, 250, 0.7);
}

.free-feature-grid span {
  color: #0f766e;
  font-weight: 900;
}

.free-feature-grid strong {
  color: #344054;
  font-size: 13px;
}

.credit-standard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.credit-standard-grid article {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.credit-standard-grid span {
  color: var(--pm-muted);
  line-height: 1.5;
}

.credit-standard-grid strong {
  color: var(--pm-primary);
  font-size: 18px;
}

.credit-policy-notes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.credit-refund-alert {
  margin-top: 14px;
}

@media (max-width: 920px) {
  .credit-standard-grid,
  .free-feature-grid {
    grid-template-columns: 1fr;
  }

  .credit-intro-body {
    align-items: flex-start;
    flex-direction: column;
  }

  .credit-intro-tags {
    justify-content: flex-start;
  }
}
</style>

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
