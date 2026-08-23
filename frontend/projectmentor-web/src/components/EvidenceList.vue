<template>
  <div class="rule-evidence-list">
    <template v-if="variant === 'qa' && qaItems.length">
      <article v-for="(evidence, index) in qaItems" :key="`${evidence.filePath || 'evidence'}-${index}`" class="qa-evidence-reference">
        <header>
          <span>{{ t('qa.v6.evidenceItem', { number: pad(index + 1) }) }}</span>
          <span class="qa-source-type">{{ evidence.sourceType || t('qa.v6.fileTypes.OTHER') }}</span>
        </header>
        <h5>{{ fileName(evidence.filePath) }}</h5>
        <dl>
          <div>
            <dt>{{ t('qa.v6.source') }}</dt>
            <dd><code>{{ evidence.filePath || '—' }}</code></dd>
          </div>
          <div>
            <dt>{{ t('qa.v6.reason') }}</dt>
            <dd>{{ evidence.reason || '—' }}</dd>
          </div>
          <div class="qa-snippet-row">
            <dt>{{ t('qa.v6.snippet') }}</dt>
            <dd><pre>{{ evidence.snippet || t('qa.v6.noSnippet') }}</pre></dd>
          </div>
        </dl>
      </article>
    </template>

    <p v-else-if="variant === 'qa'" class="section-empty">{{ t('qa.v6.insufficientDescription') }}</p>

    <template v-else-if="items.length">
      <article v-for="(evidence, index) in items" :key="`${evidence.sourceFile || 'evidence'}-${index}`" class="rule-evidence">
        <header>
          <span>{{ t('reportV5.evidence.item', { number: pad(index + 1) }) }}</span>
          <StatusLabel
            v-if="evidence.riskLevel"
            :status="normalizedRiskLevel(evidence.riskLevel)"
            :label="riskLevelLabel(evidence.riskLevel)"
          />
        </header>
        <h3>{{ evidence.conclusion || t('components.evidence.defaultConclusion') }}</h3>
        <dl>
          <div v-if="evidence.sourceFile">
            <dt>{{ t('reportV5.evidence.source') }}</dt>
            <dd><code>{{ evidence.sourceFile }}</code></dd>
          </div>
          <div v-if="evidence.evidence || evidence.detail">
            <dt>{{ t('reportV5.evidence.observed') }}</dt>
            <dd>{{ evidence.evidence || evidence.detail }}</dd>
          </div>
          <div v-if="evidence.suggestion">
            <dt>{{ t('reportV5.evidence.recommendation') }}</dt>
            <dd>{{ evidence.suggestion }}</dd>
          </div>
        </dl>
      </article>
    </template>

    <section v-else-if="rawFallback" class="legacy-narrative">
      <h3>{{ t('reportV5.evidence.legacy') }}</h3>
      <pre>{{ rawFallback }}</pre>
    </section>

    <p v-else class="section-empty">{{ t('reportV5.evidence.empty') }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import StatusLabel from '@/components/StatusLabel.vue'
import type { RuleScanEvidence } from '@/types/api'

interface QaEvidenceReference {
  filePath: string
  reason: string
  snippet: string
  sourceType?: string
}

type EvidenceInput = string | RuleScanEvidence[] | QaEvidenceReference[] | Record<string, unknown>[] | null | undefined

const props = withDefaults(defineProps<{
  evidences: EvidenceInput
  variant?: 'rule' | 'qa'
}>(), {
  variant: 'rule'
})
const { t } = useI18n()
const variant = computed(() => props.variant)
const parsed = computed(() => parseEvidenceInput(props.evidences))
const items = computed(() => parsed.value.items)
const rawFallback = computed(() => parsed.value.raw)
const qaItems = computed(() => Array.isArray(props.evidences) ? props.evidences as QaEvidenceReference[] : [])

function parseEvidenceInput(value: EvidenceInput): { items: RuleScanEvidence[]; raw: string } {
  if (!value) return { items: [], raw: '' }
  if (Array.isArray(value)) return { items: value as RuleScanEvidence[], raw: '' }
  if (typeof value !== 'string') return { items: [], raw: '' }

  try {
    const parsedValue = JSON.parse(value) as unknown
    return Array.isArray(parsedValue)
      ? { items: parsedValue as RuleScanEvidence[], raw: '' }
      : { items: [], raw: '' }
  } catch {
    return { items: [], raw: value.trim() }
  }
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function fileName(path?: string) {
  return path?.split(/[\\/]/).filter(Boolean).pop() || t('qa.v6.fileTypes.OTHER')
}

function normalizedRiskLevel(level?: string) {
  return (level || 'INFO').toUpperCase()
}

function riskLevelLabel(level?: string) {
  const normalized = normalizedRiskLevel(level)
  return ['HIGH', 'MEDIUM', 'LOW', 'INFO'].includes(normalized)
    ? t(`reportV5.enums.riskLevel.${normalized}`)
    : normalized.replace(/_/g, ' ')
}
</script>

<style scoped>
.rule-evidence-list {
  display: grid;
}

.rule-evidence {
  padding: 26px 0 28px;
  border-top: 1px solid var(--pm-stone-strong);
}

.qa-evidence-reference {
  padding: 22px 0 26px;
  border-top: 1px solid var(--pm-stone-strong);
}

.qa-evidence-reference:first-child {
  border-top-color: var(--pm-ink);
}

.qa-evidence-reference header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  line-height: 1.4;
  text-transform: uppercase;
}

.qa-source-type {
  color: var(--pm-primary-dark);
}

.qa-evidence-reference h5 {
  margin: 12px 0 0;
  color: var(--pm-ink);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.qa-evidence-reference dl {
  display: grid;
  margin: 15px 0 0;
}

.qa-evidence-reference dl > div {
  display: grid;
  grid-template-columns: minmax(110px, 0.2fr) minmax(0, 1fr);
  gap: 18px;
  padding: 10px 0;
  border-top: 1px solid var(--pm-stone);
}

.qa-evidence-reference dt {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.05em;
  line-height: 1.5;
  text-transform: uppercase;
}

.qa-evidence-reference dd {
  min-width: 0;
  margin: 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.qa-evidence-reference code,
.qa-evidence-reference pre {
  font-family: var(--pm-font-mono);
}

.qa-evidence-reference code {
  color: var(--pm-primary-deep);
  font-size: 11px;
  overflow-wrap: anywhere;
}

.qa-snippet-row dd {
  min-width: 0;
}

.qa-evidence-reference pre {
  max-height: 260px;
  margin: 0;
  overflow: auto;
  padding: 14px 16px;
  border: 1px solid var(--pm-inspection-rule);
  border-radius: var(--pm-radius-sm);
  background: var(--pm-inspection);
  color: var(--pm-inspection-text);
  font-size: 11px;
  line-height: 1.75;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.rule-evidence:first-child {
  border-top-color: var(--pm-ink);
}

.rule-evidence header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.rule-evidence header > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.rule-evidence h3,
.legacy-narrative h3 {
  max-width: 48ch;
  margin: 12px 0 0;
  color: var(--pm-ink);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.rule-evidence dl {
  display: grid;
  max-width: 75ch;
  margin: 16px 0 0;
}

.rule-evidence dl div {
  display: grid;
  grid-template-columns: minmax(110px, 0.22fr) minmax(0, 1fr);
  gap: 18px;
  padding: 10px 0;
  border-top: 1px solid var(--pm-stone);
}

.rule-evidence dt {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.rule-evidence dd {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.rule-evidence code,
.legacy-narrative pre {
  color: var(--pm-primary-deep);
  font-family: var(--pm-font-mono);
  font-size: 11px;
  overflow-wrap: anywhere;
}

.legacy-narrative,
.section-empty {
  margin: 0;
  padding: 22px 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.legacy-narrative pre {
  margin: 14px 0 0;
  color: var(--pm-graphite);
  line-height: 1.7;
  white-space: pre-wrap;
}

.section-empty {
  color: var(--pm-muted);
  line-height: 1.65;
}

@media (max-width: 620px) {
  .rule-evidence dl div,
  .qa-evidence-reference dl > div {
    grid-template-columns: 1fr;
    gap: 5px;
  }

  .qa-evidence-reference pre {
    padding: 12px;
  }
}

@media print {
  .rule-evidence header,
  .rule-evidence h3,
  .rule-evidence dl div {
    break-inside: avoid;
    page-break-inside: avoid;
  }
}
</style>
