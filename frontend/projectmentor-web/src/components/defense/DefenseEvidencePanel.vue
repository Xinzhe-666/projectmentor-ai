<template>
  <section class="defense-evidence" :aria-labelledby="headingId">
    <header class="defense-evidence__header">
      <div>
        <h3 :id="headingId">{{ panelTitle }}</h3>
        <p>{{ panelDescription }}</p>
      </div>
    </header>

    <div class="defense-evidence__group">
      <h4>{{ t('defense.evidence.claims') }}</h4>
      <ul v-if="claims.length" class="defense-evidence__claims">
        <li v-for="claim in claims" :key="claim">{{ claim }}</li>
      </ul>
      <p v-else class="defense-evidence__empty">{{ t('defense.evidence.noClaims') }}</p>
    </div>

    <div class="defense-evidence__group">
      <h4>{{ mode === 'review' ? t('defense.review.matchedEvidence') : t('defense.evidence.sources') }}</h4>
      <ol v-if="evidence.length" class="defense-evidence__sources">
        <li v-for="(source, index) in evidence" :key="source.fileId || source.filePath || index">
          <div class="defense-evidence__source-heading">
            <code>{{ source.filePath || t('defense.evidence.sourceFallback') }}</code>
            <StatusLabel
              v-if="source.evidenceLevel"
              :status="source.evidenceLevel"
              :label="formatEvidenceLevel(source.evidenceLevel)"
            />
          </div>
          <p v-if="source.reason" class="defense-evidence__reason">{{ source.reason }}</p>
          <pre v-if="source.snippet"><code>{{ source.snippet }}</code></pre>
        </li>
      </ol>
      <p v-else class="defense-evidence__empty">
        {{ mode === 'review' ? t('defense.review.noMatchedEvidence') : t('defense.evidence.noSources') }}
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'

import StatusLabel from '@/components/StatusLabel.vue'
import type { DefenseEvidenceReference } from '@/types/api'

const props = withDefaults(
  defineProps<{
    claims: string[]
    evidence: DefenseEvidenceReference[]
    mode?: 'question' | 'review'
  }>(),
  {
    mode: 'question'
  }
)

const { t } = useI18n()
const headingId = `defense-evidence-${useId()}`

const panelTitle = computed(() =>
  props.mode === 'review' ? t('defense.review.finding') : t('defense.evidence.title')
)
const panelDescription = computed(() =>
  props.mode === 'review' ? t('defense.review.description') : t('defense.evidence.description')
)

function formatEvidenceLevel(value: string) {
  return value.replace(/_/g, ' ')
}
</script>

<style scoped>
.defense-evidence {
  min-width: 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.defense-evidence__header,
.defense-evidence__group {
  padding: 18px 20px;
}

.defense-evidence__header {
  border-bottom: 1px solid var(--pm-stone);
}

.defense-evidence h3,
.defense-evidence h4 {
  margin: 0;
  color: var(--pm-ink);
}

.defense-evidence h3 {
  font-size: 16px;
  letter-spacing: -0.015em;
}

.defense-evidence h4 {
  font-size: 12px;
  font-weight: 600;
}

.defense-evidence__header p {
  margin: 7px 0 0;
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.6;
}

.defense-evidence__group + .defense-evidence__group {
  border-top: 1px solid var(--pm-stone);
}

.defense-evidence__claims,
.defense-evidence__sources {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
}

.defense-evidence__claims li {
  position: relative;
  padding: 10px 0 10px 16px;
  border-top: 1px solid var(--pm-stone);
  color: var(--pm-graphite);
  font-size: 13px;
  line-height: 1.6;
}

.defense-evidence__claims li::before {
  position: absolute;
  top: 17px;
  left: 0;
  width: 6px;
  height: 6px;
  background: var(--pm-primary);
  content: '';
}

.defense-evidence__sources > li {
  padding: 13px 0;
  border-top: 1px solid var(--pm-stone);
}

.defense-evidence__source-heading {
  display: flex;
  min-width: 0;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.defense-evidence__source-heading > code {
  min-width: 0;
  color: var(--pm-primary-dark);
  font-family: var(--pm-font-mono);
  font-size: 11px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.defense-evidence__reason,
.defense-evidence__empty {
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.6;
}

.defense-evidence__reason {
  margin: 8px 0 0;
}

.defense-evidence__empty {
  margin: 12px 0 0;
}

.defense-evidence pre {
  max-height: 180px;
  margin: 10px 0 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid var(--pm-stone);
  border-radius: var(--pm-radius-sm);
  background: var(--pm-paper);
  color: var(--pm-graphite);
  white-space: pre-wrap;
}

.defense-evidence pre code {
  font-family: var(--pm-font-mono);
  font-size: 11px;
  line-height: 1.65;
}

@media (max-width: 520px) {
  .defense-evidence__header,
  .defense-evidence__group {
    padding: 16px 0;
  }

  .defense-evidence {
    background: transparent;
  }
}
</style>
