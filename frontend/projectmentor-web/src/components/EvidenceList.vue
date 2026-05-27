<template>
  <div class="evidence-list">
    <template v-if="items.length">
      <article v-for="(evidence, index) in items" :key="`${evidence.sourceFile || 'evidence'}-${index}`" class="evidence-card">
        <div class="evidence-card-head">
          <span :class="['evidence-level-badge', riskLevelClass(evidence.riskLevel)]">
            {{ normalizedRiskLevel(evidence.riskLevel) }}
          </span>
          <div class="evidence-title">
            <span>结论</span>
            <strong>{{ evidence.conclusion || '证据结论' }}</strong>
          </div>
        </div>
        <dl class="evidence-fields">
          <div v-if="evidence.sourceFile">
            <dt>sourceFile</dt>
            <dd class="source-file">{{ evidence.sourceFile }}</dd>
          </div>
          <div>
            <dt>evidence</dt>
            <dd>{{ evidence.evidence || evidence.detail || '-' }}</dd>
          </div>
          <div v-if="evidence.suggestion">
            <dt>suggestion</dt>
            <dd>{{ evidence.suggestion }}</dd>
          </div>
        </dl>
      </article>
    </template>

    <pre v-else-if="rawFallback" class="text-block">{{ rawFallback }}</pre>

    <EmptyState v-else title="暂无证据链" description="当前结果没有返回结构化证据信息。" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import EmptyState from '@/components/EmptyState.vue'
import type { RuleScanEvidence } from '@/types/api'

type EvidenceInput = string | RuleScanEvidence[] | Record<string, unknown>[] | null | undefined

const props = defineProps<{
  evidences: EvidenceInput
}>()

const parsed = computed(() => parseEvidenceInput(props.evidences))
const items = computed(() => parsed.value.items)
const rawFallback = computed(() => parsed.value.raw)

function parseEvidenceInput(value: EvidenceInput): { items: RuleScanEvidence[]; raw: string } {
  if (!value) {
    return { items: [], raw: '' }
  }

  if (Array.isArray(value)) {
    return { items: value as RuleScanEvidence[], raw: '' }
  }

  if (typeof value !== 'string') {
    return { items: [], raw: JSON.stringify(value, null, 2) }
  }

  try {
    const parsedValue = JSON.parse(value) as unknown
    if (Array.isArray(parsedValue)) {
      return { items: parsedValue as RuleScanEvidence[], raw: '' }
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
.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evidence-card {
  padding: 18px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
  box-shadow: 0 8px 22px rgba(28, 43, 68, 0.04);
}

.evidence-card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.evidence-level-badge {
  min-width: 78px;
  padding: 5px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.evidence-title {
  min-width: 0;
}

.evidence-title span {
  display: block;
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 700;
}

.evidence-title strong {
  display: block;
  margin-top: 4px;
  color: var(--pm-ink);
  font-size: 16px;
  line-height: 1.35;
}

.evidence-fields {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
}

.evidence-fields div {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(223, 230, 240, 0.74);
}

.evidence-fields dt {
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 800;
}

.evidence-fields dd {
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
  .evidence-fields div {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
