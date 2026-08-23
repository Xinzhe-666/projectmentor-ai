<template>
  <div class="risk-findings">
    <template v-if="items.length">
      <article v-for="(risk, index) in items" :key="`${risk.riskType || 'risk'}-${index}`" class="risk-finding">
        <header>
          <span>{{ t('reportV5.risk.finding', { number: pad(index + 1) }) }}</span>
          <StatusLabel :status="normalizedRiskLevel(risk.riskLevel)" :label="riskLevelLabel(risk.riskLevel)" />
        </header>
        <h3>{{ risk.riskType || t('components.risk.defaultRisk') }}</h3>
        <p v-if="risk.message" class="risk-observation">{{ risk.message }}</p>
        <dl>
          <div v-if="risk.keyword">
            <dt>{{ t('reportV5.risk.keyword') }}</dt>
            <dd><code>{{ risk.keyword }}</code></dd>
          </div>
          <div v-if="risk.sourceFile">
            <dt>{{ t('reportV5.risk.source') }}</dt>
            <dd><code class="source-file">{{ risk.sourceFile }}</code></dd>
          </div>
          <div v-if="risk.evidence">
            <dt>{{ t('reportV5.risk.evidence') }}</dt>
            <dd>{{ risk.evidence }}</dd>
          </div>
          <div v-if="risk.suggestion">
            <dt>{{ t('reportV5.risk.recommendation') }}</dt>
            <dd>{{ risk.suggestion }}</dd>
          </div>
        </dl>
      </article>
    </template>

    <section v-else-if="rawFallback" class="legacy-narrative">
      <h3>{{ t('reportV5.risk.legacy') }}</h3>
      <pre>{{ rawFallback }}</pre>
    </section>

    <p v-else class="section-empty">{{ t('reportV5.risk.empty') }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import StatusLabel from '@/components/StatusLabel.vue'
import type { RuleScanRisk } from '@/types/api'

type RiskInput = string | RuleScanRisk[] | Record<string, unknown>[] | null | undefined

const props = defineProps<{ risks: RiskInput }>()
const { t } = useI18n()
const parsed = computed(() => parseRiskInput(props.risks))
const items = computed(() => parsed.value.items)
const rawFallback = computed(() => parsed.value.raw)

function parseRiskInput(value: RiskInput): { items: RuleScanRisk[]; raw: string } {
  if (!value) return { items: [], raw: '' }
  if (Array.isArray(value)) return { items: value as RuleScanRisk[], raw: '' }
  if (typeof value !== 'string') return { items: [], raw: '' }

  try {
    const parsedValue = JSON.parse(value) as unknown
    return Array.isArray(parsedValue)
      ? { items: parsedValue as RuleScanRisk[], raw: '' }
      : { items: [], raw: '' }
  } catch {
    return { items: [], raw: value.trim() }
  }
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

function pad(value: number) {
  return String(value).padStart(2, '0')
}
</script>

<style scoped>
.risk-findings {
  display: grid;
}

.risk-finding {
  padding: 28px 0 30px;
  border-top: 1px solid var(--pm-stone-strong);
}

.risk-finding:first-child {
  border-top-color: var(--pm-ink);
}

.risk-finding header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.risk-finding header > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.risk-finding h3,
.legacy-narrative h3 {
  margin: 12px 0 0;
  color: var(--pm-ink);
  font-size: 19px;
  font-weight: 600;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.risk-observation {
  max-width: 72ch;
  margin: 14px 0 0;
  color: var(--pm-graphite);
  font-size: 15px;
  line-height: 1.75;
}

.risk-finding dl {
  display: grid;
  max-width: 75ch;
  margin: 18px 0 0;
}

.risk-finding dl div {
  display: grid;
  grid-template-columns: minmax(110px, 0.22fr) minmax(0, 1fr);
  gap: 18px;
  padding: 11px 0;
  border-top: 1px solid var(--pm-stone);
}

.risk-finding dt {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.risk-finding dd {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.risk-finding code,
.legacy-narrative pre {
  font-family: var(--pm-font-mono);
  font-size: 11px;
}

.source-file {
  color: var(--pm-primary-deep);
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
  overflow-wrap: anywhere;
}

.section-empty {
  color: var(--pm-muted);
  line-height: 1.65;
}

@media (max-width: 620px) {
  .risk-finding dl div {
    grid-template-columns: 1fr;
    gap: 5px;
  }
}

@media print {
  .risk-finding header,
  .risk-finding h3 {
    break-after: avoid;
    page-break-after: avoid;
  }

  .risk-finding dl div {
    break-inside: avoid;
    page-break-inside: avoid;
  }
}
</style>
