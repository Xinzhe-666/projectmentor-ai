<template>
  <div class="risk-list">
    <template v-if="items.length">
      <article v-for="(risk, index) in items" :key="`${risk.riskType || 'risk'}-${index}`" class="risk-card">
        <div class="risk-card-head">
          <span :class="['risk-level-badge', riskLevelClass(risk.riskLevel)]">
            {{ normalizedRiskLevel(risk.riskLevel) }}
          </span>
          <div class="risk-title">
            <span>{{ t('components.risk.riskType') }}</span>
            <strong>{{ risk.riskType || t('components.risk.defaultRisk') }}</strong>
          </div>
          <span v-if="risk.keyword" class="risk-keyword">{{ risk.keyword }}</span>
        </div>
        <p v-if="risk.message" class="risk-message">{{ risk.message }}</p>
        <dl class="risk-fields">
          <div v-if="risk.sourceFile">
            <dt>{{ t('components.risk.sourceFile') }}</dt>
            <dd class="source-file">{{ risk.sourceFile }}</dd>
          </div>
          <template v-if="risk.evidence">
            <div>
              <dt>{{ t('common.evidence') }}</dt>
              <dd>{{ risk.evidence }}</dd>
            </div>
          </template>
          <template v-if="risk.suggestion">
            <div>
              <dt>{{ t('common.suggestions') }}</dt>
              <dd>{{ risk.suggestion }}</dd>
            </div>
          </template>
        </dl>
      </article>
    </template>

    <pre v-else-if="rawFallback" class="text-block">{{ rawFallback }}</pre>

    <EmptyState v-else :title="t('components.risk.emptyTitle')" :description="t('components.risk.emptyDesc')" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import EmptyState from '@/components/EmptyState.vue'
import type { RuleScanRisk } from '@/types/api'

type RiskInput = string | RuleScanRisk[] | Record<string, unknown>[] | null | undefined

const props = defineProps<{
  risks: RiskInput
}>()

const { t } = useI18n()
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
  const normalized = normalizedRiskLevel(level)
  return {
    'risk-high': normalized === 'HIGH',
    'risk-medium': normalized === 'MEDIUM',
    'risk-low': normalized === 'LOW',
    'risk-info': normalized === 'INFO'
  }
}

function normalizedRiskLevel(level?: string) {
  return (level || 'INFO').toUpperCase()
}
</script>

<style scoped>
.risk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.risk-card {
  padding: 18px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
  box-shadow: 0 8px 22px rgba(28, 43, 68, 0.04);
}

.risk-card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.risk-level-badge {
  min-width: 78px;
  padding: 5px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.risk-title {
  min-width: 0;
}

.risk-title span {
  display: block;
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 700;
}

.risk-title strong {
  display: block;
  margin-top: 4px;
  color: var(--pm-ink);
  font-size: 16px;
  line-height: 1.35;
}

.risk-message {
  margin: 14px 0 12px;
  color: #344054;
  line-height: 1.75;
}

.risk-keyword {
  margin-top: 2px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1f6feb;
  font-size: 12px;
  font-weight: 700;
}

.risk-fields {
  display: grid;
  gap: 10px;
  margin: 0;
}

.risk-fields div {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(223, 230, 240, 0.74);
}

.risk-fields dt {
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 700;
}

.risk-fields dd {
  margin: 0;
  color: #344054;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.source-file {
  color: var(--pm-primary);
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 13px;
}

.risk-high {
  border-color: rgba(239, 68, 68, 0.26);
  background: rgba(239, 68, 68, 0.1);
  color: var(--risk-high);
}

.risk-medium {
  border-color: rgba(245, 158, 11, 0.28);
  background: rgba(245, 158, 11, 0.12);
  color: var(--risk-medium);
}

.risk-low {
  border-color: rgba(14, 165, 233, 0.24);
  background: rgba(14, 165, 233, 0.1);
  color: var(--risk-low);
}

.risk-info {
  border-color: rgba(100, 116, 139, 0.24);
  background: rgba(100, 116, 139, 0.1);
  color: var(--risk-info);
}

@media (max-width: 620px) {
  .risk-fields div {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
