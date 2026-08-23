<template>
  <div class="claim-review">
    <div v-if="orderedClaims.length" class="claim-review-list">
      <article
        v-for="(claim, index) in orderedClaims"
        :key="claimKey(claim, index)"
        class="claim-chapter"
      >
        <header class="claim-chapter-header">
          <div class="claim-chapter-index">
            <span>{{ t('reportV5.claims.finding', { number: pad(index + 1) }) }}</span>
            <StatusLabel :status="claim.status" :label="statusLabel(claim.status)" />
          </div>
          <h3>{{ claim.claimText }}</h3>
          <dl class="claim-chapter-meta">
            <div>
              <dt>{{ t('reportV5.claims.source') }}</dt>
              <dd>{{ sourceLabel(claim.sourceType) }}</dd>
            </div>
            <div>
              <dt>{{ t('reportV5.claims.category') }}</dt>
              <dd>{{ categoryLabel(claim.category) }}</dd>
            </div>
            <div v-if="claim.confidenceScore !== undefined">
              <dt>{{ t('reportV5.claims.confidence') }}</dt>
              <dd>{{ claim.confidenceScore }}</dd>
            </div>
          </dl>
        </header>

        <div class="claim-chapter-body">
          <section v-if="claim.sourceSnippet" class="claim-statement">
            <h4>{{ t('reportV5.claims.sourceStatement') }}</h4>
            <p>{{ claim.sourceSnippet }}</p>
          </section>

          <section class="claim-assessment">
            <h4>{{ t('reportV5.claims.assessment') }}</h4>
            <p>{{ claim.reason || '—' }}</p>
          </section>

          <section class="claim-evidence">
            <h4>{{ t('reportV5.claims.evidence') }}</h4>
            <div v-if="claim.evidenceFiles?.length" class="claim-evidence-list">
              <article
                v-for="(evidence, evidenceIndex) in claim.evidenceFiles"
                :key="`${evidence.fileId || evidence.filePath}-${evidenceIndex}`"
                class="evidence-document"
              >
                <header class="evidence-document-header">
                  <div>
                    <span>{{ t('reportV5.claims.evidenceItem', { number: pad(evidenceIndex + 1) }) }}</span>
                    <strong>{{ fileName(evidence.filePath) }}</strong>
                  </div>
                  <StatusLabel
                    :status="evidence.evidenceLevel"
                    :label="evidenceLevelLabel(evidence.evidenceLevel)"
                  />
                </header>
                <code class="evidence-path">{{ evidence.filePath }}</code>
                <dl class="evidence-metadata">
                  <div v-if="evidence.fileType">
                    <dt>{{ t('reportV5.claims.fileType') }}</dt>
                    <dd>{{ fileRole(evidence) }}</dd>
                  </div>
                  <div v-if="evidence.reason">
                    <dt>{{ t('reportV5.claims.reason') }}</dt>
                    <dd>{{ evidence.reason }}</dd>
                  </div>
                  <div v-if="evidence.matchedKeywords?.length">
                    <dt>{{ t('reportV5.claims.keywords') }}</dt>
                    <dd><code>{{ evidence.matchedKeywords.join(' · ') }}</code></dd>
                  </div>
                </dl>
                <pre v-if="evidence.snippet" tabindex="0"><code>{{ evidence.snippet }}</code></pre>
              </article>
            </div>
            <p v-else class="claim-no-evidence">{{ t('reportV5.claims.noEvidence') }}</p>
          </section>
        </div>
      </article>
    </div>

    <EmptyState
      v-else
      variant="compact"
      :title="t('reportV5.claims.noClaims')"
      :description="t('reportV5.claims.noClaimsDescription')"
    >
      <el-button v-if="showEmptyAction" type="primary" @click="emit('empty-action')">
        {{ t('reportV5.claims.returnToProject') }}
      </el-button>
    </EmptyState>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type { ClaimEvidenceFile, ClaimEvidenceItem, ClaimEvidenceStatus } from '@/types/api'

const props = withDefaults(defineProps<{
  claims?: ClaimEvidenceItem[]
  showEmptyAction?: boolean
}>(), {
  claims: () => [],
  showEmptyAction: false
})

const emit = defineEmits<{
  (event: 'empty-action'): void
}>()

const { t } = useI18n()

const statusPriority: Record<ClaimEvidenceStatus, number> = {
  RISKY: 0,
  NO_EVIDENCE: 1,
  PARTIAL: 2,
  DOC_ONLY: 3,
  SUPPORTED: 4
}

const orderedClaims = computed(() => (props.claims || [])
  .map((claim, index) => ({ claim, index }))
  .sort((left, right) => {
    const priority = statusPriority[left.claim.status] - statusPriority[right.claim.status]
    return priority || left.index - right.index
  })
  .map(({ claim }) => claim))

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function claimKey(claim: ClaimEvidenceItem, index: number) {
  return `${claim.sourceType}-${claim.claimText}-${index}`
}

function statusLabel(status: ClaimEvidenceStatus) {
  return t(`projects.v5.claimStatus.${status}`)
}

function sourceLabel(sourceType: string) {
  const knownSources = ['PROJECT_DESCRIPTION', 'TECH_STACK', 'README']
  return knownSources.includes(sourceType)
    ? t(`projects.v5.claimSource.${sourceType}`)
    : sourceType.replace(/_/g, ' ')
}

function categoryLabel(category: string) {
  const normalized = category.toUpperCase()
  const knownCategories = [
    'AUTH', 'DATABASE', 'CACHE', 'AI', 'RAG_OR_QA', 'FILE_UPLOAD', 'REPORT', 'INTERVIEW',
    'ADMIN', 'CREDIT', 'DEPLOYMENT', 'FRONTEND', 'SECURITY', 'PERFORMANCE',
    'BUSINESS_OR_PRODUCT', 'GENERAL'
  ]
  return knownCategories.includes(normalized)
    ? t(`reportV5.enums.claimCategory.${normalized}`)
    : normalized.replace(/_/g, ' ')
}

function fileName(path: string) {
  return path.split(/[\\/]/).filter(Boolean).pop() || path
}

function fileRole(file: Pick<ClaimEvidenceFile, 'filePath' | 'fileType'>) {
  const type = file.fileType?.toUpperCase() || ''
  const path = file.filePath.toLowerCase()
  let role = 'OTHER'

  if (type === 'README' || path.endsWith('.md')) {
    role = 'DOC'
  } else if (
    ['CONFIG', 'POM', 'PACKAGE', 'DOCKER', 'DOCKER_COMPOSE', 'SQL', 'GITIGNORE'].includes(type)
    || ['.xml', '.yml', '.yaml', '.properties', '.sql', '.json'].some((extension) => path.endsWith(extension))
    || path.endsWith('dockerfile')
  ) {
    role = 'CONFIG'
  } else if (
    ['CODE', 'CONTROLLER', 'SERVICE', 'MAPPER', 'ENTITY', 'UTIL'].includes(type)
    || ['.java', '.kt', '.js', '.jsx', '.ts', '.tsx', '.vue', '.css', '.scss', '.html', '.py', '.go', '.rs', '.c', '.cpp', '.cs', '.sh'].some((extension) => path.endsWith(extension))
  ) {
    role = 'CODE'
  }

  return t(`projects.v5.evidence.fileTypes.${role}`)
}

function evidenceLevelLabel(level: string) {
  const normalized = level.toUpperCase()
  return ['STRONG', 'MEDIUM', 'WEAK', 'NONE'].includes(normalized)
    ? t(`reportV5.enums.evidenceLevel.${normalized}`)
    : normalized.replace(/_/g, ' ')
}
</script>

<style scoped>
.claim-review-list {
  display: grid;
  gap: 0;
}

.claim-chapter {
  min-width: 0;
  padding: 34px 0 40px;
  border-top: 1px solid var(--pm-stone-strong);
}

.claim-chapter:first-child {
  border-top-color: var(--pm-ink);
}

.claim-chapter-header,
.claim-chapter-body {
  min-width: 0;
}

.claim-chapter-index,
.evidence-document-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--pm-space-4);
}

.claim-chapter-index > span,
.evidence-document-header > div > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.claim-chapter h3 {
  max-width: 34ch;
  margin: 14px 0 18px;
  color: var(--pm-ink);
  font-size: clamp(21px, 2.8vw, 28px);
  font-weight: 600;
  letter-spacing: -0.025em;
  line-height: 1.28;
  overflow-wrap: anywhere;
}

.claim-chapter-meta,
.evidence-metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 28px;
  margin: 0;
}

.claim-chapter-meta div,
.evidence-metadata div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.claim-chapter-meta dt,
.evidence-metadata dt,
.claim-chapter-body h4 {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.claim-chapter-meta dd,
.evidence-metadata dd {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.claim-chapter-body {
  display: grid;
  max-width: 75ch;
  gap: 24px;
  margin-top: 28px;
}

.claim-chapter-body h4 {
  margin: 0 0 8px;
}

.claim-chapter-body p {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 15px;
  line-height: 1.75;
  overflow-wrap: anywhere;
}

.claim-statement {
  padding: 16px 18px;
  border: 1px solid var(--pm-stone);
  background: var(--pm-paper);
}

.claim-evidence-list {
  display: grid;
  gap: 18px;
}

.evidence-document {
  min-width: 0;
  padding-top: 16px;
  border-top: 1px solid var(--pm-stone);
}

.evidence-document-header {
  align-items: flex-start;
}

.evidence-document-header > div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.evidence-document-header strong {
  color: var(--pm-ink);
  font-size: 15px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.evidence-path {
  display: block;
  margin-top: 9px;
  color: var(--pm-primary-deep);
  font-family: var(--pm-font-mono);
  font-size: 11px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.evidence-metadata {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.evidence-metadata div {
  grid-template-columns: minmax(86px, 0.22fr) minmax(0, 1fr);
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--pm-stone);
}

.evidence-metadata code {
  font-family: var(--pm-font-mono);
  font-size: 11px;
}

.evidence-document pre {
  max-width: 100%;
  margin: 16px 0 0;
  padding: 18px;
  overflow: auto;
  border: 1px solid var(--pm-inspection-rule);
  border-radius: var(--pm-radius-sm);
  background: var(--pm-inspection);
  color: var(--pm-inspection-text);
  font-family: var(--pm-font-mono);
  font-size: 11px;
  line-height: 1.7;
  tab-size: 2;
  white-space: pre;
}

.claim-no-evidence {
  padding: 14px 0;
  border-top: 1px solid var(--pm-stone);
  border-bottom: 1px solid var(--pm-stone);
  color: var(--pm-muted) !important;
}

@media (max-width: 620px) {
  .claim-chapter {
    padding: 28px 0 32px;
  }

  .claim-chapter-index,
  .evidence-document-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .claim-chapter-meta {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evidence-metadata div {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}

@media print {
  .claim-chapter {
    padding: 8mm 0;
    break-inside: auto;
    page-break-inside: auto;
  }

  .claim-chapter-header,
  .claim-statement,
  .claim-assessment,
  .evidence-document-header {
    break-inside: avoid;
    page-break-inside: avoid;
  }

  .claim-chapter h3 {
    font-size: 18pt;
  }

  .evidence-document {
    break-inside: auto;
    page-break-inside: auto;
  }

  .evidence-document pre {
    overflow: visible;
    border-color: #8b919a;
    background: #ffffff !important;
    color: #111111 !important;
    font-size: 8pt;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }
}
</style>
