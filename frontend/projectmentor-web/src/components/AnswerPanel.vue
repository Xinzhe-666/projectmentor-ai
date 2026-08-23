<template>
  <article class="answer-panel" aria-labelledby="evidence-answer-heading">
    <header class="answer-header">
      <div>
        <h3 id="evidence-answer-heading">{{ t('qa.v6.answer') }}</h3>
        <p>{{ record.question }}</p>
      </div>
      <div class="answer-actions">
        <StatusLabel :status="evidenceLevel" :label="evidenceLevelLabel" />
        <el-button @click="copyAnswer">{{ t('qa.v6.copyAnswer') }}</el-button>
      </div>
    </header>

    <section v-if="isInsufficient" class="insufficient-evidence" role="status">
      <div>
        <StatusLabel status="NO_EVIDENCE" :label="t('qa.v6.insufficientTitle')" />
        <p>{{ t('qa.v6.insufficientDescription') }}</p>
      </div>
      <div>
        <strong>{{ t('qa.v6.suggestedImprovement') }}</strong>
        <p>{{ t('qa.v6.improvementDescription') }}</p>
      </div>
    </section>

    <template v-else>
      <section class="answer-reading">
        <div class="answer-narrative">
          <span class="provenance-label">{{ record.aiUsed ? t('qa.v6.aiInterpretation') : t('qa.v6.ruleFinding') }}</span>
          <MarkdownBlock v-if="record.aiUsed" :content="record.answer" />
          <p v-else class="rule-finding-copy">{{ t('qa.v6.ruleFindingText') }}</p>
        </div>
        <dl class="answer-assessment">
          <div>
            <dt>{{ t('qa.v6.methodLabel') }}</dt>
            <dd>{{ record.aiUsed ? t('qa.v6.aiInterpretation') : t('qa.v6.ruleFinding') }}</dd>
          </div>
          <div>
            <dt>{{ t('qa.v6.evidenceConfidence') }}</dt>
            <dd>{{ confidenceText }}</dd>
          </div>
          <div>
            <dt>{{ t('qa.v6.sourcesLabel') }}</dt>
            <dd>{{ evidenceSummary }}</dd>
          </div>
          <div v-if="record.createTime">
            <dt>{{ t('qa.v6.answeredAt') }}</dt>
            <dd>{{ formattedDate }}</dd>
          </div>
        </dl>
      </section>

      <aside v-if="!record.aiUsed" class="ai-unavailable-note" role="note">
        <strong>{{ t('qa.v6.aiUnavailableTitle') }}</strong>
        <p>{{ t('qa.v6.aiUnavailableDescription') }}</p>
      </aside>

      <section class="evidence-section" aria-labelledby="qa-evidence-heading">
        <header class="answer-section-header">
          <div>
            <h4 id="qa-evidence-heading">{{ t('qa.v6.evidenceUsed') }}</h4>
            <p><strong>{{ t('qa.v6.ruleEvidence') }}</strong> · {{ t('qa.v6.evidenceDescription') }}</p>
          </div>
          <span>{{ t('qa.v6.sourceCount', { count: evidenceItems.length }) }}</span>
        </header>
        <EvidenceList variant="qa" :evidences="evidenceItems" />
      </section>

      <section v-if="relatedClaims.length" class="related-claims" aria-labelledby="related-claims-heading">
        <header class="answer-section-header">
          <div>
            <h4 id="related-claims-heading">{{ t('qa.v6.relatedClaims') }}</h4>
            <p>{{ t('qa.v6.relatedClaimsDescription') }}</p>
          </div>
        </header>
        <ul>
          <li v-for="claim in relatedClaims" :key="`${claim.claimText}-${claim.status}`">
            <span>{{ claim.claimText }}</span>
            <StatusLabel :status="claim.status" :label="claimStatusLabel(claim.status)" />
          </li>
        </ul>
      </section>
    </template>

    <section v-if="followUps.length" class="answer-follow-ups" aria-labelledby="qa-follow-up-heading">
      <h4 id="qa-follow-up-heading">{{ t('qa.v6.followUp') }}</h4>
      <div>
        <button v-for="followUp in followUps" :key="followUp" type="button" @click="emit('follow-up', followUp)">
          {{ followUp }}
        </button>
      </div>
    </section>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

import EvidenceList from '@/components/EvidenceList.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type { ClaimEvidenceItem, ProjectFile, ProjectQaResponse } from '@/types/api'

type QaDisplayRecord = ProjectQaResponse & {
  id?: number
  createTime?: string
}

interface QaEvidenceItem {
  filePath: string
  reason: string
  snippet: string
  sourceType: string
}

const props = defineProps<{
  record: QaDisplayRecord
  projectFiles: ProjectFile[]
  claims: ClaimEvidenceItem[]
}>()

const emit = defineEmits<{
  'follow-up': [value: string]
}>()

const { locale, t } = useI18n()

const rawEvidenceItems = computed(() => props.record.evidences || [])
const followUps = computed(() => (props.record.suggestedFollowUps || []).slice(0, 4))
const evidenceLevel = computed(() => {
  const normalized = props.record.evidenceLevel?.toUpperCase()
  if (['STRONG', 'MEDIUM', 'WEAK', 'NONE'].includes(normalized || '')) {
    return normalized as string
  }
  return rawEvidenceItems.value.length ? 'AVAILABLE' : 'NONE'
})
const evidenceLevelLabel = computed(() => t(`qa.v6.evidenceLevels.${evidenceLevel.value}`))
const isInsufficient = computed(() => evidenceLevel.value === 'NONE' || rawEvidenceItems.value.length === 0)
const confidenceText = computed(() => typeof props.record.confidenceScore === 'number'
  ? t('qa.v6.confidenceScore', { score: props.record.confidenceScore })
  : t('qa.v6.basedOnAvailableEvidence'))
const evidenceSummary = computed(() => t(`qa.v6.evidenceSummaries.${evidenceLevel.value}`, {
  count: rawEvidenceItems.value.length
}))
const formattedDate = computed(() => formatDate(props.record.createTime))

const projectFileMap = computed(() => new Map(
  props.projectFiles.map((file) => [normalizePath(file.filePath), file])
))

const claimFileTypeMap = computed(() => {
  const map = new Map<string, string>()
  props.claims.forEach((claim) => {
    claim.evidenceFiles?.forEach((file) => {
      if (file.fileType) map.set(normalizePath(file.filePath), file.fileType)
    })
  })
  return map
})

const evidenceItems = computed<QaEvidenceItem[]>(() => rawEvidenceItems.value.map((evidence) => {
  const normalizedPath = normalizePath(evidence.filePath)
  const sourceFile = projectFileMap.value.get(normalizedPath)
  const fileType = sourceFile?.fileType || claimFileTypeMap.value.get(normalizedPath) || ''
  return {
    ...evidence,
    sourceType: t(`qa.v6.fileTypes.${fileTypeGroup(evidence.filePath, fileType)}`)
  }
}))

const relatedClaims = computed(() => {
  const paths = new Set(rawEvidenceItems.value.map((item) => normalizePath(item.filePath)).filter(Boolean))
  if (!paths.size) return []

  return props.claims
    .map((claim) => ({
      claim,
      overlap: (claim.evidenceFiles || []).filter((file) => paths.has(normalizePath(file.filePath))).length
    }))
    .filter((entry) => entry.overlap > 0)
    .sort((left, right) => right.overlap - left.overlap)
    .slice(0, 3)
    .map((entry) => entry.claim)
})

function normalizePath(path?: string) {
  return (path || '').replace(/\\/g, '/').replace(/^\.\//, '').toLowerCase()
}

function fileTypeGroup(pathValue: string, fileTypeValue: string): 'CODE' | 'CONFIG' | 'DOC' | 'OTHER' {
  const path = pathValue.toLowerCase()
  const type = fileTypeValue.toUpperCase()
  if (type === 'README' || path.endsWith('.md') || path.endsWith('.txt')) return 'DOC'
  if (
    ['CONFIG', 'POM', 'PACKAGE', 'DOCKER', 'DOCKER_COMPOSE', 'SQL', 'GITIGNORE'].includes(type)
    || ['.xml', '.yml', '.yaml', '.properties', '.sql', '.json', '.toml'].some((extension) => path.endsWith(extension))
    || path.endsWith('dockerfile')
  ) return 'CONFIG'
  if (
    ['CODE', 'CONTROLLER', 'SERVICE', 'MAPPER', 'ENTITY', 'UTIL'].includes(type)
    || ['.java', '.kt', '.js', '.jsx', '.ts', '.tsx', '.vue', '.css', '.scss', '.html', '.py', '.go', '.rs', '.c', '.cpp', '.cs', '.sh'].some((extension) => path.endsWith(extension))
  ) return 'CODE'
  return 'OTHER'
}

function claimStatusLabel(status: string) {
  const normalized = status.toUpperCase()
  return ['SUPPORTED', 'PARTIAL', 'DOC_ONLY', 'NO_EVIDENCE', 'RISKY'].includes(normalized)
    ? t(`projects.v5.claimStatus.${normalized}`)
    : normalized.replace(/_/g, ' ')
}

function formatDate(value?: string) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value.replace('T', ' ').slice(0, 19)
  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(parsed)
}

async function copyAnswer() {
  const answer = props.record.aiUsed ? props.record.answer : t('qa.v6.ruleFindingText')
  const evidenceText = evidenceItems.value
    .map((item, index) => `[E${String(index + 1).padStart(2, '0')}] ${item.filePath}\n${item.reason}`)
    .join('\n\n')
  const claimText = relatedClaims.value
    .map((claim) => `${claim.claimText} — ${claimStatusLabel(claim.status)}`)
    .join('\n')
  const text = [
    `${t('qa.v6.questionLabel')}: ${props.record.question}`,
    '',
    `${t('qa.v6.answer')}:`,
    answer,
    '',
    `${t('qa.v6.evidenceConfidence')}: ${confidenceText.value}`,
    evidenceText,
    claimText ? `\n${t('qa.v6.relatedClaims')}:\n${claimText}` : ''
  ].filter(Boolean).join('\n')

  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(t('qa.v6.answerCopied'))
  } catch {
    ElMessage.warning(t('qa.v6.copyFailed'))
  }
}
</script>

<style scoped>
.answer-panel {
  min-width: 0;
  border-top: 1px solid var(--pm-ink);
}

.answer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 28px;
  padding: 24px 0;
  border-bottom: 1px solid var(--pm-stone-strong);
}

.answer-header h3,
.answer-section-header h4,
.answer-follow-ups h4 {
  margin: 0;
  color: var(--pm-ink);
  font-weight: 600;
  letter-spacing: -0.015em;
}

.answer-header h3 {
  font-size: 20px;
}

.answer-header p {
  max-width: 70ch;
  margin: 9px 0 0;
  color: var(--pm-graphite);
  font-size: 16px;
  line-height: 1.65;
}

.answer-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 16px;
}

.answer-actions :deep(.el-button) {
  min-height: 44px;
}

.answer-reading {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.answer-narrative {
  min-width: 0;
  padding: 28px 28px 30px 0;
  border-right: 1px solid var(--pm-stone);
}

.provenance-label,
.answer-assessment dt,
.answer-section-header > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  line-height: 1.45;
  text-transform: uppercase;
}

.answer-narrative :deep(.markdown-block),
.rule-finding-copy {
  max-width: 72ch;
  margin-top: 16px;
}

.rule-finding-copy {
  margin-bottom: 0;
  color: var(--pm-graphite);
  font-size: 16px;
  line-height: 1.75;
}

.answer-assessment {
  display: grid;
  align-content: start;
  margin: 0;
  padding: 14px 0 14px 28px;
}

.answer-assessment > div {
  display: grid;
  gap: 5px;
  padding: 13px 0;
  border-top: 1px solid var(--pm-stone);
}

.answer-assessment > div:first-child {
  border-top: 0;
}

.answer-assessment dd {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 13px;
  line-height: 1.6;
}

.ai-unavailable-note {
  display: grid;
  grid-template-columns: minmax(150px, 0.24fr) minmax(0, 1fr);
  gap: 24px;
  padding: 18px 0;
  border-bottom: 1px solid var(--pm-stone-strong);
}

.ai-unavailable-note strong {
  color: var(--pm-partial);
  font-size: 13px;
}

.ai-unavailable-note p {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 13px;
  line-height: 1.65;
}

.insufficient-evidence {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-bottom: 1px solid var(--pm-stone-strong);
}

.insufficient-evidence > div {
  min-width: 0;
  padding: 28px 28px 30px 0;
}

.insufficient-evidence > div + div {
  padding-right: 0;
  padding-left: 28px;
  border-left: 1px solid var(--pm-stone);
}

.insufficient-evidence strong {
  color: var(--pm-ink);
  font-size: 13px;
}

.insufficient-evidence p {
  max-width: 58ch;
  margin: 12px 0 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.7;
}

.evidence-section,
.related-claims,
.answer-follow-ups {
  padding-top: 34px;
}

.answer-section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 16px;
}

.answer-section-header h4,
.answer-follow-ups h4 {
  font-size: 16px;
}

.answer-section-header p {
  max-width: 68ch;
  margin: 7px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.answer-section-header p strong {
  color: var(--pm-primary-dark);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.related-claims ul {
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--pm-ink);
  list-style: none;
}

.related-claims li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  min-height: 64px;
  padding: 12px 0;
  border-bottom: 1px solid var(--pm-stone);
}

.related-claims li > span {
  color: var(--pm-ink);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.55;
}

.answer-follow-ups {
  padding-bottom: 4px;
}

.answer-follow-ups > div {
  display: grid;
  margin-top: 13px;
  border-top: 1px solid var(--pm-stone-strong);
}

.answer-follow-ups button {
  min-height: 48px;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid var(--pm-stone);
  background: transparent;
  color: var(--pm-primary-dark);
  cursor: pointer;
  font: 500 14px/1.55 var(--pm-font-sans);
  text-align: left;
  transition: color var(--pm-motion-fast) ease, padding-left var(--pm-motion-fast) ease;
}

.answer-follow-ups button:hover {
  padding-left: 6px;
  color: var(--pm-ink);
}

@media (max-width: 900px) {
  .answer-reading {
    grid-template-columns: 1fr;
  }

  .answer-narrative {
    padding-right: 0;
    border-right: 0;
    border-bottom: 1px solid var(--pm-stone);
  }

  .answer-assessment {
    padding-left: 0;
  }
}

@media (max-width: 620px) {
  .answer-header,
  .answer-section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .answer-actions {
    width: 100%;
    justify-content: space-between;
  }

  .insufficient-evidence,
  .ai-unavailable-note {
    grid-template-columns: 1fr;
  }

  .insufficient-evidence > div,
  .insufficient-evidence > div + div {
    padding: 22px 0;
    border-left: 0;
  }

  .insufficient-evidence > div + div {
    border-top: 1px solid var(--pm-stone);
  }

  .related-claims li {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .answer-follow-ups button {
    transition: none;
  }
}
</style>
