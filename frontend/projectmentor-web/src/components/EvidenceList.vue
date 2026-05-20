<template>
  <div class="evidence-list">
    <template v-if="items.length">
      <article v-for="(evidence, index) in items" :key="`${evidence.sourceFile || 'evidence'}-${index}`" class="evidence-card">
        <div class="evidence-card-head">
          <el-tag :class="riskLevelClass(evidence.riskLevel)" effect="light">
            {{ evidence.riskLevel || 'INFO' }}
          </el-tag>
          <strong>{{ evidence.conclusion || '证据结论' }}</strong>
        </div>
        <p v-if="evidence.sourceFile" class="source-file">{{ evidence.sourceFile }}</p>
        <p>{{ evidence.detail || '-' }}</p>
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
.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.evidence-card {
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.evidence-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.evidence-card p {
  margin: 10px 0 0;
  line-height: 1.75;
}

.source-file {
  color: var(--pm-primary);
  font-size: 13px;
}
</style>
