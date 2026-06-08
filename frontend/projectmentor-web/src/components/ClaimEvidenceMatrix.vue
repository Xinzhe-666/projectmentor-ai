<template>
  <div class="claim-matrix">
    <section v-if="hasAiEnhancement" class="claim-ai-panel">
      <div class="claim-ai-head">
        <div>
          <span class="claim-detail-label">{{ t('report.claimAiEnhancedLabel') }}</span>
          <h4>{{ t('report.claimAiSummary') }}</h4>
        </div>
        <el-tag v-if="aiEnhancement?.aiEnhancedAt" effect="plain">
          {{ aiEnhancement.aiEnhancedAt }}
        </el-tag>
      </div>
      <div class="claim-ai-grid">
        <section v-if="aiEnhancement?.aiSummary">
          <span class="claim-detail-label">{{ t('report.claimAiSummary') }}</span>
          <p>{{ aiEnhancement.aiSummary }}</p>
        </section>
        <section v-if="aiEnhancement?.aiRiskOverview">
          <span class="claim-detail-label">{{ t('report.claimAiRiskOverview') }}</span>
          <p>{{ aiEnhancement.aiRiskOverview }}</p>
        </section>
        <section v-if="aiEnhancement?.aiResumeStrategy">
          <span class="claim-detail-label">{{ t('report.claimAiResumeStrategy') }}</span>
          <p>{{ aiEnhancement.aiResumeStrategy }}</p>
        </section>
        <section v-if="aiEnhancement?.aiInterviewStrategy">
          <span class="claim-detail-label">{{ t('report.claimAiInterviewStrategy') }}</span>
          <p>{{ aiEnhancement.aiInterviewStrategy }}</p>
        </section>
      </div>
      <pre v-if="aiEnhancement?.aiFallbackText" class="claim-ai-fallback">{{ aiEnhancement.aiFallbackText }}</pre>
    </section>

    <div v-if="claims.length" class="claim-toolbar no-print">
      <el-select v-model="statusFilter" class="status-filter">
        <el-option
          v-for="option in statusOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <span>{{ t('report.claimEvidenceCount', { count: filteredClaims.length }) }}</span>
    </div>

    <div v-if="filteredClaims.length" class="claim-list">
      <article
        v-for="(claim, index) in filteredClaims"
        :key="claimKey(claim, index)"
        class="claim-card"
        :class="`claim-card--${claim.status.toLowerCase()}`"
      >
        <div class="claim-card-head">
          <div>
            <div class="claim-tags">
              <el-tag :type="statusTagType(claim.status)" effect="light">
                {{ statusLabel(claim.status) }}
              </el-tag>
              <el-tag type="info" effect="plain">{{ sourceLabel(claim.sourceType) }}</el-tag>
              <el-tag effect="plain">{{ formatCategory(claim.category) }}</el-tag>
            </div>
            <h4>{{ claim.claimText }}</h4>
          </div>
          <div class="claim-confidence">
            <span>{{ t('report.claimConfidence') }}</span>
            <strong>{{ claim.confidenceScore ?? 0 }}</strong>
          </div>
        </div>

        <p v-if="claim.reason" class="claim-reason">{{ claim.reason }}</p>

        <section v-if="aiItemForClaim(claim)" class="claim-ai-item">
          <div class="claim-ai-item-head">
            <span class="claim-detail-label">{{ t('report.claimAiItemTitle') }}</span>
            <el-tag effect="plain" type="success">{{ t('report.claimAiEnhancedLabel') }}</el-tag>
          </div>
          <div class="claim-ai-item-grid">
            <section v-if="aiItemForClaim(claim)?.aiExplanation">
              <span class="claim-detail-label">{{ t('report.claimAiExplanation') }}</span>
              <p>{{ aiItemForClaim(claim)?.aiExplanation }}</p>
            </section>
            <section v-if="aiItemForClaim(claim)?.saferResumeExpression">
              <span class="claim-detail-label">{{ t('report.claimAiSaferResume') }}</span>
              <p>{{ aiItemForClaim(claim)?.saferResumeExpression }}</p>
            </section>
            <section v-if="aiItemForClaim(claim)?.likelyInterviewQuestions?.length">
              <span class="claim-detail-label">{{ t('report.claimAiLikelyQuestions') }}</span>
              <ul>
                <li v-for="question in aiItemForClaim(claim)?.likelyInterviewQuestions" :key="question">
                  {{ question }}
                </li>
              </ul>
            </section>
            <section v-if="aiItemForClaim(claim)?.improvementSuggestion">
              <span class="claim-detail-label">{{ t('report.claimAiImprovement') }}</span>
              <p>{{ aiItemForClaim(claim)?.improvementSuggestion }}</p>
            </section>
          </div>
        </section>

        <div class="claim-detail-grid">
          <section>
            <span class="claim-detail-label">{{ t('report.resumeAdvice') }}</span>
            <p>{{ claim.resumeAdvice || '-' }}</p>
          </section>
          <section>
            <div class="claim-interview-head">
              <span class="claim-detail-label">{{ t('report.interviewFollowUp') }}</span>
              <el-button
                class="no-print"
                link
                type="primary"
                :icon="CopyDocument"
                @click="copyInterviewExplanation(claim)"
              >
                {{ t('report.copyInterviewExplanation') }}
              </el-button>
            </div>
            <p>{{ claim.interviewQuestion || '-' }}</p>
          </section>
        </div>

        <div class="claim-evidence-section">
          <div class="claim-evidence-title">
            <span class="claim-detail-label">{{ t('report.evidenceFiles') }}</span>
            <el-button
              v-if="claim.evidenceFiles?.length > 2"
              class="no-print"
              link
              type="primary"
              @click="toggleEvidence(claimKey(claim, index))"
            >
              {{
                isExpanded(claimKey(claim, index))
                  ? t('report.collapseEvidence')
                  : t('report.showMoreEvidence', { count: claim.evidenceFiles.length - 2 })
              }}
            </el-button>
          </div>

          <div v-if="claim.evidenceFiles?.length" class="claim-evidence-list">
            <article
              v-for="evidence in visibleEvidenceFiles(claim, claimKey(claim, index))"
              :key="`${evidence.fileId || evidence.filePath}-${evidence.filePath}`"
              class="claim-evidence-file"
            >
              <div class="claim-file-head">
                <strong>{{ evidence.filePath }}</strong>
                <el-tag
                  size="small"
                  :type="evidence.evidenceLevel === 'STRONG' ? 'success' : 'info'"
                  effect="plain"
                >
                  {{ evidenceLevelLabel(evidence.evidenceLevel) }}
                </el-tag>
              </div>
              <p v-if="evidence.reason">{{ evidence.reason }}</p>
              <pre v-if="evidence.snippet">{{ evidence.snippet }}</pre>
              <div v-if="evidence.matchedKeywords?.length" class="matched-keywords">
                <span>{{ t('report.matchedKeywords') }}</span>
                <code>{{ evidence.matchedKeywords.join(', ') }}</code>
              </div>
            </article>
          </div>
          <p v-else class="claim-no-evidence">{{ t('report.noEvidenceFiles') }}</p>
        </div>
      </article>
    </div>

    <div v-else class="claim-empty">
      {{ claims.length ? t('report.noClaimsForFilter') : t('report.noClaimEvidence') }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

import type {
  ClaimEvidenceAiEnhancement,
  ClaimEvidenceAiItem,
  ClaimEvidenceItem,
  ClaimEvidenceStatus
} from '@/types/api'

const props = withDefaults(defineProps<{
  claims?: ClaimEvidenceItem[]
  aiEnhancement?: ClaimEvidenceAiEnhancement
}>(), {
  claims: () => [],
  aiEnhancement: undefined
})

const { t } = useI18n()
const statusFilter = ref<'ALL' | ClaimEvidenceStatus>('ALL')
const expandedClaims = ref<Set<string>>(new Set())

const claims = computed(() => props.claims || [])
const aiEnhancement = computed(() => props.aiEnhancement)
const hasAiEnhancement = computed(() => Boolean(
  aiEnhancement.value?.aiEnhanced
    || aiEnhancement.value?.aiSummary
    || aiEnhancement.value?.aiRiskOverview
    || aiEnhancement.value?.aiResumeStrategy
    || aiEnhancement.value?.aiInterviewStrategy
    || aiEnhancement.value?.aiFallbackText
    || aiEnhancement.value?.aiEnhancedItems?.length
))
const aiItemsByClaim = computed(() => {
  const map = new Map<string, ClaimEvidenceAiItem>()
  for (const item of aiEnhancement.value?.aiEnhancedItems || []) {
    if (item.claimText) {
      map.set(normalizeClaimText(item.claimText), item)
    }
  }
  return map
})

const statusPriority: Record<ClaimEvidenceStatus, number> = {
  RISKY: 0,
  NO_EVIDENCE: 1,
  DOC_ONLY: 2,
  PARTIAL: 3,
  SUPPORTED: 4
}

const filteredClaims = computed(() => claims.value
  .filter((claim) => statusFilter.value === 'ALL' || claim.status === statusFilter.value)
  .slice()
  .sort((left, right) => {
    const priorityDiff = statusPriority[left.status] - statusPriority[right.status]
    if (priorityDiff !== 0) {
      return priorityDiff
    }

    return (left.confidenceScore ?? 0) - (right.confidenceScore ?? 0)
  }))

const statusOptions = computed(() => [
  { value: 'ALL', label: t('report.allClaimStatuses') },
  { value: 'RISKY', label: statusLabel('RISKY') },
  { value: 'NO_EVIDENCE', label: statusLabel('NO_EVIDENCE') },
  { value: 'DOC_ONLY', label: statusLabel('DOC_ONLY') },
  { value: 'PARTIAL', label: statusLabel('PARTIAL') },
  { value: 'SUPPORTED', label: statusLabel('SUPPORTED') }
])

function claimKey(claim: ClaimEvidenceItem, index: number) {
  return `${claim.sourceType}-${claim.claimText}-${index}`
}

function normalizeClaimText(text: string) {
  return text.trim().replace(/\s+/g, ' ').toLowerCase()
}

function aiItemForClaim(claim: ClaimEvidenceItem) {
  return aiItemsByClaim.value.get(normalizeClaimText(claim.claimText))
}

function statusLabel(status: ClaimEvidenceStatus) {
  const keys: Record<ClaimEvidenceStatus, string> = {
    SUPPORTED: 'report.claimStatus.supported',
    PARTIAL: 'report.claimStatus.partial',
    DOC_ONLY: 'report.claimStatus.docOnly',
    NO_EVIDENCE: 'report.claimStatus.noEvidence',
    RISKY: 'report.claimStatus.risky'
  }
  return t(keys[status])
}

function statusTagType(status: ClaimEvidenceStatus) {
  if (status === 'SUPPORTED') {
    return 'success'
  }
  if (status === 'PARTIAL') {
    return 'warning'
  }
  if (status === 'DOC_ONLY') {
    return 'info'
  }
  return 'danger'
}

function sourceLabel(sourceType: string) {
  const keys: Record<string, string> = {
    PROJECT_DESCRIPTION: 'report.claimSource.projectDescription',
    TECH_STACK: 'report.claimSource.techStack',
    README: 'report.claimSource.readme'
  }
  return keys[sourceType] ? t(keys[sourceType]) : sourceType
}

function evidenceLevelLabel(level: string) {
  return level === 'STRONG'
    ? t('report.strongEvidence')
    : t('report.weakEvidence')
}

function formatCategory(category: string) {
  return category.replace(/_/g, ' ')
}

function toggleEvidence(key: string) {
  const next = new Set(expandedClaims.value)
  if (next.has(key)) {
    next.delete(key)
  } else {
    next.add(key)
  }
  expandedClaims.value = next
}

function isExpanded(key: string) {
  return expandedClaims.value.has(key)
}

function visibleEvidenceFiles(claim: ClaimEvidenceItem, key: string) {
  const files = claim.evidenceFiles || []
  return isExpanded(key) ? files : files.slice(0, 2)
}

async function copyInterviewExplanation(claim: ClaimEvidenceItem) {
  const evidencePaths = (claim.evidenceFiles || [])
    .map((item) => item.filePath)
    .slice(0, 5)
    .join('\n')
  const content = [
    claim.claimText,
    `${t('report.claimStatusLabel')}: ${statusLabel(claim.status)}`,
    `${t('report.claimReason')}: ${claim.reason || '-'}`,
    `${t('report.interviewFollowUp')}: ${claim.interviewQuestion || '-'}`,
    `${t('report.evidenceFiles')}:`,
    evidencePaths || t('report.noEvidenceFiles')
  ].join('\n')

  const copied = await copyText(content)
  ElMessage[copied ? 'success' : 'error'](
    copied ? t('report.interviewExplanationCopied') : t('report.resumeCopyFailed')
  )
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // Continue with the textarea fallback for non-secure origins.
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()

  let copied = false
  try {
    copied = document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
  }
  return copied
}
</script>

<style scoped>
.claim-matrix {
  display: grid;
  gap: 14px;
}

.claim-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.claim-toolbar span {
  color: var(--pm-muted);
  font-size: 13px;
}

.status-filter {
  width: 220px;
}

.claim-list {
  display: grid;
  gap: 14px;
}

.claim-ai-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--pm-border);
  border-left: 4px solid #1f6feb;
  border-radius: 8px;
  background: #ffffff;
}

.claim-ai-head,
.claim-ai-item-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.claim-ai-head h4 {
  margin: 6px 0 0;
  color: var(--pm-ink);
  font-size: 17px;
}

.claim-ai-grid,
.claim-ai-item-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.claim-ai-grid section,
.claim-ai-item-grid section {
  min-width: 0;
  padding: 12px;
  border: 1px solid #eaecf0;
  border-radius: 7px;
  background: #fbfdff;
}

.claim-ai-grid p,
.claim-ai-item-grid p,
.claim-ai-item-grid ul {
  margin: 8px 0 0;
  color: #475467;
  line-height: 1.7;
}

.claim-ai-item-grid ul {
  padding-left: 18px;
}

.claim-ai-fallback {
  max-width: 100%;
  margin: 0;
  padding: 10px;
  overflow-x: auto;
  border-radius: 6px;
  background: #101828;
  color: #f2f4f7;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.claim-ai-item {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(31, 111, 235, 0.18);
  border-radius: 8px;
  background: #f8fbff;
}

.claim-card {
  padding: 18px;
  border: 1px solid var(--pm-border);
  border-left: 4px solid #98a2b3;
  border-radius: 8px;
  background: #ffffff;
}

.claim-card--supported {
  border-left-color: #12b76a;
}

.claim-card--partial {
  border-left-color: #f79009;
}

.claim-card--doc_only {
  border-left-color: #667085;
}

.claim-card--no_evidence,
.claim-card--risky {
  border-left-color: #f04438;
}

.claim-card-head,
.claim-file-head,
.claim-interview-head,
.claim-evidence-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.claim-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.claim-card h4 {
  margin: 12px 0 0;
  color: var(--pm-ink);
  font-size: 17px;
  line-height: 1.65;
}

.claim-confidence {
  min-width: 58px;
  text-align: right;
}

.claim-confidence span,
.claim-detail-label {
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 600;
}

.claim-confidence strong {
  display: block;
  margin-top: 4px;
  color: var(--pm-ink);
  font-size: 22px;
}

.claim-reason {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f8fafc;
  color: #475467;
  line-height: 1.7;
}

.claim-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.claim-detail-grid section {
  padding: 12px;
  border: 1px solid #eaecf0;
  border-radius: 7px;
  background: #fbfdff;
}

.claim-detail-grid p,
.claim-evidence-file p {
  margin: 8px 0 0;
  color: #475467;
  line-height: 1.7;
}

.claim-evidence-section {
  margin-top: 16px;
}

.claim-evidence-list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.claim-evidence-file {
  padding: 12px;
  border: 1px solid #eaecf0;
  border-radius: 7px;
  background: #fbfdff;
}

.claim-file-head strong {
  min-width: 0;
  color: #344054;
  overflow-wrap: anywhere;
}

.claim-evidence-file pre {
  max-width: 100%;
  margin: 10px 0 0;
  padding: 10px;
  overflow-x: auto;
  border-radius: 6px;
  background: #101828;
  color: #f2f4f7;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.matched-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 9px;
  color: var(--pm-muted);
  font-size: 12px;
}

.matched-keywords code {
  color: #344054;
}

.claim-no-evidence,
.claim-empty {
  color: var(--pm-muted);
}

.claim-empty {
  padding: 28px 18px;
  border: 1px dashed var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
  text-align: center;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .claim-toolbar,
  .claim-card-head,
  .claim-detail-grid,
  .claim-ai-grid,
  .claim-ai-item-grid {
    display: grid;
    grid-template-columns: 1fr;
  }

  .status-filter {
    width: 100%;
  }

  .claim-confidence {
    text-align: left;
  }
}

@media print {
  .claim-card,
  .claim-evidence-file,
  .claim-detail-grid section {
    break-inside: avoid;
    page-break-inside: avoid;
    border-color: #d0d5dd;
    background: #ffffff;
  }

  .claim-evidence-file pre {
    border: 1px solid #d0d5dd;
    background: #ffffff;
    color: #111827;
  }
}
</style>
