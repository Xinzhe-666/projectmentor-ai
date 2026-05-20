<template>
  <div class="risk-list">
    <template v-if="items.length">
      <article v-for="(risk, index) in items" :key="`${risk.riskType || 'risk'}-${index}`" class="risk-card">
        <div class="risk-card-head">
          <el-tag :class="riskLevelClass(risk.riskLevel)" effect="light">
            {{ risk.riskLevel || 'INFO' }}
          </el-tag>
          <strong>{{ risk.riskType || '风险点' }}</strong>
          <span v-if="risk.keyword" class="risk-keyword">{{ risk.keyword }}</span>
        </div>
        <p>{{ risk.message || '-' }}</p>
        <dl>
          <template v-if="risk.evidence">
            <dt>证据</dt>
            <dd>{{ risk.evidence }}</dd>
          </template>
          <template v-if="risk.suggestion">
            <dt>建议</dt>
            <dd>{{ risk.suggestion }}</dd>
          </template>
        </dl>
      </article>
    </template>

    <pre v-else-if="rawFallback" class="text-block">{{ rawFallback }}</pre>

    <EmptyState v-else title="暂无风险点" description="当前结果没有返回结构化风险信息。" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import type { RuleScanRisk } from '@/types/api'

type RiskInput = string | RuleScanRisk[] | Record<string, unknown>[] | null | undefined

const props = defineProps<{
  risks: RiskInput
}>()

const parsed = computed(() => parseRiskInput(props.risks))
const items = computed(() => parsed.value.items)
const rawFallback = computed(() => parsed.value.raw)

function parseRiskInput(value: RiskInput): { items: RuleScanRisk[]; raw: string } {
  if (!value) {
    return { items: [], raw: '' }
  }

  if (Array.isArray(value)) {
    return { items: value as RuleScanRisk[], raw: '' }
  }

  if (typeof value !== 'string') {
    return { items: [], raw: JSON.stringify(value, null, 2) }
  }

  try {
    const parsedValue = JSON.parse(value) as unknown
    if (Array.isArray(parsedValue)) {
      return { items: parsedValue as RuleScanRisk[], raw: '' }
    }

    return { items: [], raw: JSON.stringify(parsedValue, null, 2) }
  } catch {
    return { items: [], raw: value }
  }
}

function riskLevelClass(level?: string) {
  const normalized = (level || 'INFO').toUpperCase()
  return {
    'risk-high': normalized === 'HIGH',
    'risk-medium': normalized === 'MEDIUM',
    'risk-low': normalized === 'LOW',
    'risk-info': normalized === 'INFO'
  }
}
</script>

<style scoped>
.risk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-card {
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.risk-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.risk-card p {
  margin: 12px 0;
  line-height: 1.75;
}

.risk-keyword {
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1f6feb;
  font-size: 12px;
}

.risk-card dl {
  display: grid;
  gap: 6px;
  margin: 0;
}

.risk-card dt {
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 700;
}

.risk-card dd {
  margin: 0;
  color: #344054;
  line-height: 1.7;
}
</style>
