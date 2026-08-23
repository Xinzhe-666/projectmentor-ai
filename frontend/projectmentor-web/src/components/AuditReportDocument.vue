<template>
  <!-- Impeccable direction contract: editorial evidence dossier; graphite paper, ruled hierarchy, IBM Plex roles; no dashboard cards, charts, gradients, shadows, pills, or invented data. -->
  <article class="audit-report" :class="{ 'audit-report--public': publicMode }">
    <header class="report-document-header">
      <div class="document-title-row">
        <div>
          <div class="document-identity">
            <BrandLogo variant="compact" tone="monochrome" />
            <span class="document-type">{{ publicMode ? t('reportV5.sharedDocumentType') : t('reportV5.documentType') }}</span>
          </div>
          <h1>{{ projectName }}</h1>
        </div>
        <span v-if="publicMode" class="read-only-label">{{ t('reportV5.readOnly') }}</span>
      </div>

      <dl class="report-metadata">
        <div v-if="reportId !== undefined">
          <dt>{{ t('reportV5.reportId') }}</dt>
          <dd>#{{ reportId }}</dd>
        </div>
        <div v-if="projectId !== undefined">
          <dt>{{ t('reportV5.projectId') }}</dt>
          <dd>#{{ projectId }}</dd>
        </div>
        <div v-if="report.createTime">
          <dt>{{ t('reportV5.generated') }}</dt>
          <dd>{{ formatDate(report.createTime) }}</dd>
        </div>
        <div v-if="techStack">
          <dt>{{ t('reportV5.technology') }}</dt>
          <dd>{{ techStack }}</dd>
        </div>
        <div v-if="projectType">
          <dt>{{ t('reportV5.projectType') }}</dt>
          <dd>{{ projectType }}</dd>
        </div>
        <div>
          <dt>{{ t('reportV5.auditScore') }}</dt>
          <dd class="audit-score-value">
            <template v-if="hasTotalScore">{{ formattedTotalScore }} <small>{{ t('reportV5.outOf') }}</small></template>
            <template v-else>{{ t('reportV5.noScore') }}</template>
          </dd>
        </div>
      </dl>
    </header>

    <section class="report-section executive-section" aria-labelledby="audit-executive-title">
      <SectionHeading index="01" :title="t('reportV5.sections.executive')" title-id="audit-executive-title" />
      <div class="executive-copy">
        <MarkdownBlock v-if="report.summary" :content="report.summary" />
        <p v-else>{{ t('common.noData') }}</p>
      </div>

      <div class="audit-ledgers">
        <section aria-labelledby="status-ledger-title">
          <h3 id="status-ledger-title">{{ t('reportV5.statusLedger') }}</h3>
          <table>
            <tbody>
              <tr v-for="item in statusCounts" :key="item.status">
                <th scope="row"><StatusLabel :status="item.status" :label="statusLabel(item.status)" /></th>
                <td>{{ item.count }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section aria-labelledby="score-ledger-title">
          <h3 id="score-ledger-title">{{ t('reportV5.scoreLedger') }}</h3>
          <table>
            <tbody>
              <tr v-for="item in scoreRows" :key="item.label">
                <th scope="row">{{ item.label }}</th>
                <td>{{ item.value }}</td>
              </tr>
              <tr v-if="!scoreRows.length">
                <th scope="row">{{ t('reportV5.auditScore') }}</th>
                <td>—</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section aria-labelledby="review-mode-title">
          <h3 id="review-mode-title">{{ t('reportV5.reviewMode') }}</h3>
          <dl class="review-provenance">
            <div>
              <dt>{{ t('reportV5.ai.deterministicLabel') }}</dt>
              <dd><StatusLabel status="AVAILABLE" :label="t('reportV5.ruleReview')" /></dd>
            </div>
            <div>
              <dt>{{ t('reportV5.ai.interpretationLabel') }}</dt>
              <dd>
                <StatusLabel
                  :status="hasAiEnhancement ? 'AVAILABLE' : 'UNAVAILABLE'"
                  :label="hasAiEnhancement ? t('reportV5.aiReviewIncluded') : t('reportV5.aiReviewNotIncluded')"
                />
              </dd>
            </div>
          </dl>
        </section>
      </div>

      <section v-if="hasAuditBrief" class="audit-brief" aria-labelledby="audit-brief-title">
        <h3 id="audit-brief-title">{{ t('reportV5.brief.title') }}</h3>
        <div class="audit-brief-grid">
          <section v-if="report.strengths">
            <h4>{{ t('reportV5.brief.strengths') }}</h4>
            <MarkdownBlock :content="report.strengths" />
          </section>
          <section v-if="report.weaknesses">
            <h4>{{ t('reportV5.brief.weaknesses') }}</h4>
            <MarkdownBlock :content="report.weaknesses" />
          </section>
          <section v-if="report.suggestions">
            <h4>{{ t('reportV5.brief.suggestions') }}</h4>
            <MarkdownBlock :content="report.suggestions" />
          </section>
        </div>
      </section>
    </section>

    <section class="report-section" aria-labelledby="audit-claims-title">
      <SectionHeading
        index="02"
        :title="t('reportV5.sections.claims')"
        :description="t('reportV5.sectionDescriptions.claims')"
        title-id="audit-claims-title"
      />
      <p v-if="unsafeClaimCount" class="evidence-boundary-note">
        <StatusLabel status="RISKY" :label="t('reportV5.resume.boundaryTitle')" />
        <span>{{ t('reportV5.claims.directResumeWarning') }}</span>
      </p>
      <ClaimEvidenceMatrix
        :claims="claims"
        :show-empty-action="privateActions"
        @empty-action="emit('empty-action')"
      />
    </section>

    <section class="report-section ai-review-section" aria-labelledby="audit-ai-title">
      <SectionHeading
        index="03"
        :title="t('reportV5.sections.ai')"
        :description="t('reportV5.sectionDescriptions.ai')"
        title-id="audit-ai-title"
      />

      <template v-if="hasAiEnhancement">
        <div class="ai-provenance-row">
          <StatusLabel status="AVAILABLE" :label="t('reportV5.ai.available')" />
          <span v-if="aiEnhancement?.aiEnhancedAt">{{ t('reportV5.ai.generatedAt', { time: formatDate(aiEnhancement.aiEnhancedAt) }) }}</span>
        </div>
        <div class="ai-overview-grid">
          <section v-if="aiEnhancement?.aiSummary">
            <h3>{{ t('reportV5.ai.summary') }}</h3>
            <MarkdownBlock :content="aiEnhancement.aiSummary" />
          </section>
          <section v-if="aiEnhancement?.aiRiskOverview">
            <h3>{{ t('reportV5.ai.riskOverview') }}</h3>
            <MarkdownBlock :content="aiEnhancement.aiRiskOverview" />
          </section>
          <section v-if="aiEnhancement?.aiResumeStrategy">
            <h3>{{ t('reportV5.ai.resumeStrategy') }}</h3>
            <MarkdownBlock :content="aiEnhancement.aiResumeStrategy" />
          </section>
          <section v-if="aiEnhancement?.aiInterviewStrategy">
            <h3>{{ t('reportV5.ai.interviewStrategy') }}</h3>
            <MarkdownBlock :content="aiEnhancement.aiInterviewStrategy" />
          </section>
        </div>

        <div v-if="aiEnhancement?.aiEnhancedItems?.length" class="ai-item-list">
          <article v-for="(item, index) in aiEnhancement.aiEnhancedItems" :key="`${item.claimText}-${index}`">
            <header>
              <span>{{ t('reportV5.ai.itemReview') }} {{ pad(index + 1) }}</span>
              <h3>{{ item.claimText }}</h3>
            </header>
            <dl>
              <div v-if="item.aiExplanation">
                <dt>{{ t('reportV5.ai.explanation') }}</dt>
                <dd>{{ item.aiExplanation }}</dd>
              </div>
              <div v-if="item.saferResumeExpression">
                <dt>{{ t('reportV5.ai.saferWording') }}</dt>
                <dd>{{ item.saferResumeExpression }}</dd>
              </div>
              <div v-if="item.improvementSuggestion">
                <dt>{{ t('reportV5.ai.improvement') }}</dt>
                <dd>{{ item.improvementSuggestion }}</dd>
              </div>
            </dl>
          </article>
        </div>

        <section v-if="aiEnhancement?.aiFallbackText" class="ai-fallback">
          <h3>{{ t('reportV5.ai.fallback') }}</h3>
          <pre>{{ aiEnhancement.aiFallbackText }}</pre>
        </section>
      </template>

      <div v-else class="ai-unavailable">
        <StatusLabel status="UNAVAILABLE" :label="t('reportV5.ai.unavailable')" />
        <p>{{ t('reportV5.ai.unavailableDescription') }}</p>
        <el-button
          v-if="privateActions && claims.length"
          class="no-print"
          :loading="aiLoading"
          @click="emit('ai-enhance')"
        >
          {{ aiLoading ? t('reportV5.ai.generating') : t('reportV5.ai.generate') }}
          <span class="button-cost">{{ t('reportV5.ai.creditCost', { count: aiCreditCost }) }}</span>
        </el-button>
      </div>
    </section>

    <section class="report-section" aria-labelledby="audit-risks-title">
      <SectionHeading
        index="04"
        :title="t('reportV5.sections.risks')"
        :description="t('reportV5.sectionDescriptions.risks')"
        title-id="audit-risks-title"
      />
      <RiskList :risks="report.riskPoints" />
    </section>

    <section class="report-section" aria-labelledby="audit-rule-evidence-title">
      <SectionHeading
        index="05"
        :title="t('reportV5.sections.evidence')"
        :description="t('reportV5.sectionDescriptions.evidence')"
        title-id="audit-rule-evidence-title"
      />
      <EvidenceList :evidences="report.evidenceChain" />
    </section>

    <section class="report-section" aria-labelledby="audit-resume-title">
      <SectionHeading
        index="06"
        :title="t('reportV5.sections.resume')"
        :description="t('reportV5.sectionDescriptions.resume')"
        title-id="audit-resume-title"
      />

      <div class="resume-boundary">
        <StatusLabel status="WARNING" :label="t('reportV5.resume.boundaryTitle')" />
        <p>{{ t('reportV5.resume.boundaryText') }}</p>
      </div>

      <section v-if="claimAdvice.length" class="claim-advice-list">
        <h3>{{ t('reportV5.resume.claimAdvice') }}</h3>
        <ol>
          <li v-for="item in claimAdvice" :key="item.claim">
            <StatusLabel :status="item.status" :label="statusLabel(item.status)" />
            <strong>{{ item.claim }}</strong>
            <p>{{ item.advice }}</p>
          </li>
        </ol>
      </section>

      <section class="resume-drafts">
        <h3>{{ t('reportV5.resume.drafts') }}</h3>
        <article v-for="section in resumeSections" :key="section.key" class="resume-draft">
          <header>
            <div>
              <h4>{{ section.title }}</h4>
              <p>{{ section.description }}</p>
            </div>
            <el-button
              v-if="privateActions && section.content"
              class="no-print"
              :icon="CopyDocument"
              @click="copyResume(section.content)"
            >
              {{ t('reportV5.resume.copy') }}
            </el-button>
          </header>
          <MarkdownBlock v-if="section.content" :content="section.content" />
          <p v-else class="empty-copy">{{ t('reportV5.resume.empty') }}</p>
        </article>
      </section>
    </section>

    <section class="report-section" aria-labelledby="audit-interview-title">
      <SectionHeading
        index="07"
        :title="t('reportV5.sections.interview')"
        :description="t('reportV5.sectionDescriptions.interview')"
        title-id="audit-interview-title"
      />
      <div v-if="interviewQuestions.length" class="interview-list">
        <div v-if="privateActions" class="interview-actions no-print">
          <el-button :icon="CopyDocument" @click="copyInterviewQuestions">
            {{ t('reportV5.interview.copy') }}
          </el-button>
        </div>
        <ol>
          <li v-for="(item, index) in interviewQuestions" :key="`${item.question}-${index}`">
            <span class="interview-number">{{ pad(index + 1) }}</span>
            <div>
              <span class="interview-source">{{ item.source === 'AI' ? t('reportV5.interview.aiSource') : t('reportV5.interview.ruleSource') }}</span>
              <h3>{{ item.question }}</h3>
              <p v-if="item.claim"><strong>{{ t('reportV5.interview.relatedClaim') }}:</strong> {{ item.claim }}</p>
            </div>
          </li>
        </ol>
      </div>
      <p v-else class="section-empty">{{ t('reportV5.interview.empty') }}</p>
    </section>

    <section class="report-section methodology-section" aria-labelledby="audit-methodology-title">
      <SectionHeading index="08" :title="t('reportV5.sections.methodology')" title-id="audit-methodology-title" />
      <div class="methodology-grid">
        <section>
          <h3>{{ t('reportV5.methodology.title') }}</h3>
          <ul>
            <li>{{ t('reportV5.methodology.intro') }}</li>
            <li>{{ t('reportV5.methodology.ruleScan') }}</li>
            <li>{{ t('reportV5.methodology.aiLayer') }}</li>
          </ul>
        </section>
        <section>
          <h3>{{ t('reportV5.methodology.limitationsTitle') }}</h3>
          <ul>
            <li>{{ t('reportV5.methodology.currentMaterials') }}</li>
            <li>{{ t('reportV5.methodology.notCertification') }}</li>
            <li>{{ t('reportV5.methodology.notSecurityAudit') }}</li>
            <li>{{ t('reportV5.methodology.humanReview') }}</li>
          </ul>
        </section>
      </div>
    </section>

    <footer class="document-footer">
      <BrandLogo variant="compact" tone="monochrome" />
      <span>{{ publicMode ? t('reportV5.public.footer') : t('report.notice') }}</span>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

import BrandLogo from '@/components/BrandLogo.vue'
import ClaimEvidenceMatrix from '@/components/ClaimEvidenceMatrix.vue'
import EvidenceList from '@/components/EvidenceList.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import RiskList from '@/components/RiskList.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type {
  AnalysisReport,
  ClaimEvidenceStatus,
  PublicReport
} from '@/types/api'

type ReportDocumentData = AnalysisReport | PublicReport

const props = withDefaults(defineProps<{
  report: ReportDocumentData
  projectName: string
  reportId?: number
  projectId?: number
  techStack?: string
  projectType?: string
  publicMode?: boolean
  privateActions?: boolean
  aiLoading?: boolean
  aiCreditCost?: number
}>(), {
  reportId: undefined,
  projectId: undefined,
  techStack: '',
  projectType: '',
  publicMode: false,
  privateActions: false,
  aiLoading: false,
  aiCreditCost: 0
})

const emit = defineEmits<{
  (event: 'empty-action'): void
  (event: 'ai-enhance'): void
}>()

const { t } = useI18n()
const claims = computed(() => props.report.claimEvidenceList || [])
const aiEnhancement = computed(() => props.report.claimEvidenceAi)

const hasAiEnhancement = computed(() => Boolean(
  aiEnhancement.value?.aiEnhanced
  || aiEnhancement.value?.aiSummary
  || aiEnhancement.value?.aiRiskOverview
  || aiEnhancement.value?.aiResumeStrategy
  || aiEnhancement.value?.aiInterviewStrategy
  || aiEnhancement.value?.aiFallbackText
  || aiEnhancement.value?.aiEnhancedItems?.length
))

const statusOrder: ClaimEvidenceStatus[] = ['SUPPORTED', 'PARTIAL', 'DOC_ONLY', 'NO_EVIDENCE', 'RISKY']
const statusCounts = computed(() => statusOrder.map((status) => ({
  status,
  count: claims.value.filter((claim) => claim.status === status).length
})))

const unsafeClaimCount = computed(() => claims.value.filter((claim) =>
  ['DOC_ONLY', 'NO_EVIDENCE', 'RISKY'].includes(claim.status)
).length)

const hasTotalScore = computed(() => Number.isFinite(props.report.totalScore))
const formattedTotalScore = computed(() => Math.round(Number(props.report.totalScore)))
const scoreRows = computed(() => [
  { label: t('report.scores.runnability'), value: props.report.runnabilityScore },
  { label: t('report.scores.authenticity'), value: props.report.authenticityScore },
  { label: t('report.scores.structure'), value: props.report.structureScore },
  { label: t('report.scores.readme'), value: props.report.readmeScore },
  { label: t('report.scores.security'), value: props.report.securityScore },
  { label: t('report.scores.engineering'), value: props.report.engineeringScore },
  { label: t('report.scores.interview'), value: props.report.interviewScore }
].filter((item): item is { label: string; value: number } => Number.isFinite(item.value)))

const hasAuditBrief = computed(() => Boolean(
  props.report.strengths || props.report.weaknesses || props.report.suggestions
))

const claimAdvice = computed(() => claims.value
  .filter((claim) => claim.resumeAdvice)
  .map((claim) => ({
    claim: claim.claimText,
    status: claim.status,
    advice: claim.resumeAdvice || ''
  })))

const resumeSections = computed(() => [
  {
    key: 'basic',
    title: t('report.resumeBasic'),
    description: t('report.resumeBasicDesc'),
    content: props.report.resumeBasic
  },
  {
    key: 'standard',
    title: t('report.resumeStandard'),
    description: t('report.resumeStandardDesc'),
    content: props.report.resumeStandard
  },
  {
    key: 'advanced',
    title: t('report.resumeAdvanced'),
    description: t('report.resumeAdvancedDesc'),
    content: props.report.resumeAdvanced
  }
])

const interviewQuestions = computed(() => {
  const seen = new Set<string>()
  const questions: Array<{ question: string; claim: string; source: 'RULE' | 'AI' }> = []

  const add = (question: string | undefined, claim: string, source: 'RULE' | 'AI') => {
    const value = question?.trim()
    const key = value?.replace(/\s+/g, ' ').toLowerCase()
    if (!value || !key || seen.has(key)) return
    seen.add(key)
    questions.push({ question: value, claim, source })
  }

  for (const claim of claims.value) add(claim.interviewQuestion, claim.claimText, 'RULE')
  for (const item of aiEnhancement.value?.aiEnhancedItems || []) {
    for (const question of item.likelyInterviewQuestions || []) add(question, item.claimText, 'AI')
  }

  return questions
})

const SectionHeading = defineComponent({
  props: {
    index: { type: String, required: true },
    title: { type: String, required: true },
    description: { type: String, default: '' },
    titleId: { type: String, required: true }
  },
  setup(componentProps) {
    return () => h('header', { class: 'section-heading' }, [
      h('span', componentProps.index),
      h('div', [
        h('h2', { id: componentProps.titleId }, componentProps.title),
        componentProps.description ? h('p', componentProps.description) : null
      ])
    ])
  }
})

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function formatDate(value: string) {
  return String(value).replace('T', ' ').slice(0, 19)
}

function statusLabel(status: ClaimEvidenceStatus) {
  return t(`projects.v5.claimStatus.${status}`)
}

async function copyResume(content: string) {
  const copied = await copyText(content)
  ElMessage[copied ? 'success' : 'error'](
    copied ? t('reportV5.resume.copied') : t('reportV5.resume.copyFailed')
  )
}

async function copyInterviewQuestions() {
  const content = interviewQuestions.value
    .map((item, index) => `${index + 1}. ${item.question}\n${t('reportV5.interview.relatedClaim')}: ${item.claim}`)
    .join('\n\n')
  const copied = await copyText(content)
  ElMessage[copied ? 'success' : 'error'](
    copied ? t('reportV5.interview.copied') : t('reportV5.interview.copyFailed')
  )
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // Continue with the textarea fallback on non-secure origins.
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
.audit-report {
  width: 100%;
  min-width: 0;
  max-width: 1000px;
  margin: 0 auto;
  border: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
  color: var(--pm-ink);
}

.report-document-header {
  padding: 48px 56px 42px;
  border-top: 4px solid var(--pm-ink);
}

.document-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 28px;
}

.document-identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.document-identity .document-type {
  padding-left: 16px;
  border-left: 1px solid var(--pm-stone-strong);
}

.document-type,
.read-only-label {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  line-height: 1.4;
  text-transform: uppercase;
}

.read-only-label {
  padding-bottom: 5px;
  border-bottom: 1px solid var(--pm-stone-strong);
  white-space: nowrap;
}

.document-title-row h1 {
  max-width: 24ch;
  margin: 12px 0 0;
  color: var(--pm-ink);
  font-size: var(--pm-type-report-title);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 1.08;
  overflow-wrap: anywhere;
}

.report-metadata {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin: 34px 0 0;
  border-top: 1px solid var(--pm-ink);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.audit-report--public .report-metadata {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.audit-report--public .report-metadata > div + div {
  padding-left: 16px;
  border-left: 1px solid var(--pm-stone);
}

.report-metadata > div {
  min-width: 0;
  padding: 15px 16px 15px 0;
}

.report-metadata > div:not(:nth-child(3n + 1)) {
  padding-left: 16px;
  border-left: 1px solid var(--pm-stone);
}

.report-metadata dt,
.report-metadata dd {
  margin: 0;
}

.report-metadata dt,
.audit-ledgers h3,
.audit-brief > h3,
.audit-brief h4,
.ai-overview-grid h3,
.resume-drafts > h3,
.claim-advice-list > h3,
.methodology-grid h3 {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.report-metadata dd {
  margin-top: 6px;
  color: var(--pm-graphite);
  font-family: var(--pm-font-mono);
  font-size: 12px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.report-metadata .audit-score-value {
  color: var(--pm-ink);
  font-family: var(--pm-font-sans);
  font-size: 25px;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  letter-spacing: -0.025em;
  line-height: 1;
}

.audit-score-value small {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0;
}

.report-section {
  padding: 46px 56px 50px;
  border-top: 1px solid var(--pm-stone-strong);
  animation: reportSectionReveal var(--pm-motion-slow) var(--pm-ease-out) both;
}

.report-section:nth-of-type(2) { animation-delay: 24ms; }
.report-section:nth-of-type(3) { animation-delay: 48ms; }
.report-section:nth-of-type(4) { animation-delay: 72ms; }
.report-section:nth-of-type(5) { animation-delay: 96ms; }
.report-section:nth-of-type(6) { animation-delay: 120ms; }
.report-section:nth-of-type(7) { animation-delay: 144ms; }
.report-section:nth-of-type(8) { animation-delay: 168ms; }

@keyframes reportSectionReveal {
  from {
    opacity: 0.01;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.section-heading) {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 18px;
  margin-bottom: 30px;
}

:deep(.section-heading > span) {
  padding-top: 5px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
}

:deep(.section-heading h2) {
  margin: 0;
  color: var(--pm-ink);
  font-size: clamp(23px, 3vw, 30px);
  font-weight: 600;
  letter-spacing: -0.03em;
  line-height: 1.18;
}

:deep(.section-heading p) {
  max-width: 68ch;
  margin: 8px 0 0;
  color: var(--pm-muted);
  font-size: 14px;
  line-height: 1.65;
}

:deep(.markdown-block code) {
  font-family: var(--pm-font-mono);
}

.executive-copy {
  max-width: 72ch;
  color: var(--pm-graphite);
  font-size: 18px;
  line-height: 1.8;
}

.executive-copy > p {
  margin: 0;
}

.audit-ledgers {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin-top: 34px;
  border-top: 1px solid var(--pm-ink);
  border-bottom: 1px solid var(--pm-ink);
}

.audit-ledgers > section {
  min-width: 0;
  padding: 18px 20px 18px 0;
}

.audit-ledgers > section + section {
  padding-left: 20px;
  border-left: 1px solid var(--pm-stone-strong);
}

.audit-ledgers h3 {
  margin: 0 0 12px;
}

.audit-ledgers table {
  width: 100%;
  border-collapse: collapse;
}

.audit-ledgers th,
.audit-ledgers td {
  padding: 7px 0;
  border-top: 1px solid var(--pm-stone);
  color: var(--pm-graphite);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.5;
  text-align: left;
  vertical-align: top;
}

.audit-ledgers td {
  width: 38px;
  font-family: var(--pm-font-mono);
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.review-provenance {
  margin: 0;
}

.review-provenance div {
  padding: 9px 0;
  border-top: 1px solid var(--pm-stone);
}

.review-provenance dt {
  margin-bottom: 5px;
  color: var(--pm-muted);
  font-size: 11px;
}

.review-provenance dd {
  margin: 0;
}

.audit-brief {
  margin-top: 36px;
}

.audit-brief > h3,
.resume-drafts > h3,
.claim-advice-list > h3 {
  margin: 0 0 14px;
}

.audit-brief-grid,
.ai-overview-grid,
.methodology-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid var(--pm-stone-strong);
}

.audit-brief-grid > section,
.ai-overview-grid > section {
  min-width: 0;
  padding: 18px 20px 0 0;
}

.audit-brief-grid > section + section,
.ai-overview-grid > section + section {
  padding-left: 20px;
  border-left: 1px solid var(--pm-stone);
}

.audit-brief h4,
.ai-overview-grid h3 {
  margin: 0 0 10px;
}

.evidence-boundary-note,
.resume-boundary,
.ai-unavailable {
  margin: 0 0 24px;
  padding: 16px 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.evidence-boundary-note {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}

.evidence-boundary-note span:last-child,
.resume-boundary p,
.ai-unavailable p {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.65;
}

.ai-provenance-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 12px 0;
  border-top: 1px solid var(--pm-ink);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.ai-provenance-row > span:last-child {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.ai-overview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 28px;
}

.ai-overview-grid > section:nth-child(odd) {
  padding-left: 0;
  border-left: 0;
}

.ai-overview-grid > section:nth-child(even) {
  padding-left: 20px;
  border-left: 1px solid var(--pm-stone);
}

.ai-overview-grid > section:nth-child(n + 3) {
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid var(--pm-stone);
}

.ai-item-list {
  display: grid;
  margin-top: 34px;
}

.ai-item-list article {
  padding: 24px 0;
  border-top: 1px solid var(--pm-stone-strong);
}

.ai-item-list header > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  text-transform: uppercase;
}

.ai-item-list h3 {
  max-width: 48ch;
  margin: 8px 0 0;
  font-size: 18px;
  line-height: 1.45;
}

.ai-item-list dl {
  max-width: 75ch;
  margin: 16px 0 0;
}

.ai-item-list dl div {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 18px;
  padding: 10px 0;
  border-top: 1px solid var(--pm-stone);
}

.ai-item-list dt {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  text-transform: uppercase;
}

.ai-item-list dd {
  margin: 0;
  color: var(--pm-graphite);
  line-height: 1.7;
}

.ai-fallback {
  margin-top: 26px;
}

.ai-fallback h3 {
  font-size: 15px;
}

.ai-fallback pre {
  padding: 16px;
  overflow: auto;
  border: 1px solid var(--pm-stone-strong);
  color: var(--pm-graphite);
  font-family: var(--pm-font-mono);
  font-size: 11px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.ai-unavailable {
  display: grid;
  max-width: 72ch;
  gap: 10px;
}

.ai-unavailable .el-button {
  justify-self: start;
  min-height: 44px;
}

.button-cost {
  margin-left: 10px;
  color: inherit;
  font-family: var(--pm-font-mono);
  font-size: 9px;
  opacity: 0.72;
}

.resume-boundary {
  display: grid;
  max-width: 75ch;
  gap: 8px;
}

.claim-advice-list {
  margin-top: 30px;
}

.claim-advice-list ol,
.interview-list ol {
  margin: 0;
  padding: 0;
  list-style: none;
}

.claim-advice-list li {
  display: grid;
  grid-template-columns: minmax(128px, auto) minmax(180px, 0.7fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  padding: 16px 0;
  border-top: 1px solid var(--pm-stone);
}

.claim-advice-list strong {
  color: var(--pm-ink);
  font-size: 14px;
  line-height: 1.5;
}

.claim-advice-list p {
  margin: 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.7;
}

.resume-drafts {
  margin-top: 38px;
}

.resume-draft {
  padding: 24px 0 28px;
  border-top: 1px solid var(--pm-stone-strong);
}

.resume-draft > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.resume-draft h4 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 19px;
  font-weight: 600;
}

.resume-draft header p {
  margin: 5px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.55;
}

.resume-draft .el-button,
.interview-actions .el-button {
  min-height: 44px;
}

.empty-copy,
.section-empty {
  margin: 0;
  padding: 16px 0;
  color: var(--pm-muted);
  line-height: 1.65;
}

.interview-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 10px;
}

.interview-list li {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 16px;
  padding: 22px 0;
  border-top: 1px solid var(--pm-stone-strong);
}

.interview-number {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 11px;
}

.interview-source {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.interview-list h3 {
  max-width: 58ch;
  margin: 7px 0 0;
  color: var(--pm-ink);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.45;
}

.interview-list p {
  max-width: 75ch;
  margin: 9px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.methodology-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
}

.methodology-grid section {
  padding: 20px 28px 0 0;
}

.methodology-grid section + section {
  padding-left: 28px;
  border-left: 1px solid var(--pm-stone);
}

.methodology-grid h3 {
  margin: 0 0 12px;
}

.methodology-grid ul {
  display: grid;
  gap: 12px;
  margin: 0;
  padding-left: 18px;
}

.methodology-grid li {
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.7;
}

.document-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 56px;
  border-top: 1px solid var(--pm-ink);
}

.document-footer > span {
  max-width: 64ch;
  color: var(--pm-muted);
  font-size: 11px;
  line-height: 1.6;
  text-align: right;
}

@media (max-width: 920px) {
  .report-document-header,
  .report-section {
    padding-right: 36px;
    padding-left: 36px;
  }

  .report-metadata,
  .audit-report--public .report-metadata,
  .audit-ledgers {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .report-metadata > div:not(:nth-child(3n + 1)) {
    padding-left: 0;
    border-left: 0;
  }

  .audit-report--public .report-metadata > div + div {
    padding-left: 0;
    border-left: 0;
  }

  .report-metadata > div:nth-child(even),
  .audit-report--public .report-metadata > div:nth-child(even),
  .audit-ledgers > section:nth-child(even) {
    padding-left: 18px;
    border-left: 1px solid var(--pm-stone);
  }

  .audit-ledgers > section:nth-child(3) {
    grid-column: 1 / -1;
    padding-left: 0;
    border-top: 1px solid var(--pm-stone-strong);
    border-left: 0;
  }

  .audit-brief-grid {
    grid-template-columns: 1fr;
  }

  .audit-brief-grid > section + section {
    margin-top: 20px;
    padding-top: 20px;
    padding-left: 0;
    border-top: 1px solid var(--pm-stone);
    border-left: 0;
  }

  .document-footer {
    padding-right: 36px;
    padding-left: 36px;
  }
}

@media (max-width: 620px) {
  .audit-report {
    border-right: 0;
    border-left: 0;
  }

  .report-document-header {
    padding: 34px 20px 30px;
  }

  .report-section {
    padding: 36px 20px 40px;
  }

  .document-title-row {
    flex-direction: column;
  }

  .document-identity {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .document-identity .document-type {
    padding-left: 0;
    border-left: 0;
  }

  .document-title-row h1 {
    font-size: 31px;
  }

  .report-metadata {
    grid-template-columns: 1fr;
  }

  .audit-report--public .report-metadata {
    grid-template-columns: 1fr;
  }

  .report-metadata > div,
  .report-metadata > div:nth-child(even),
  .audit-report--public .report-metadata > div + div,
  .audit-report--public .report-metadata > div:nth-child(even) {
    padding: 12px 0;
    border-top: 1px solid var(--pm-stone);
    border-left: 0;
  }

  .report-metadata > div:first-child {
    border-top: 0;
  }

  :deep(.section-heading) {
    grid-template-columns: 32px minmax(0, 1fr);
    gap: 10px;
    margin-bottom: 24px;
  }

  .executive-copy {
    font-size: 16px;
  }

  .audit-ledgers,
  .ai-overview-grid,
  .methodology-grid {
    grid-template-columns: 1fr;
  }

  .audit-ledgers > section,
  .audit-ledgers > section:nth-child(even),
  .audit-ledgers > section:nth-child(3),
  .ai-overview-grid > section,
  .ai-overview-grid > section:nth-child(even),
  .methodology-grid section,
  .methodology-grid section + section {
    grid-column: auto;
    margin: 0;
    padding: 18px 0;
    border-top: 1px solid var(--pm-stone);
    border-left: 0;
  }

  .audit-ledgers > section:first-child,
  .ai-overview-grid > section:first-child,
  .methodology-grid section:first-child {
    border-top: 0;
  }

  .evidence-boundary-note,
  .ai-provenance-row,
  .resume-draft > header,
  .document-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-item-list dl div,
  .claim-advice-list li {
    grid-template-columns: 1fr;
    gap: 7px;
  }

  .interview-list li {
    grid-template-columns: 34px minmax(0, 1fr);
    gap: 10px;
  }

  .document-footer {
    padding: 22px 20px;
  }

  .document-footer > span {
    text-align: left;
  }
}

@media (prefers-reduced-motion: reduce) {
  .report-section {
    animation: none;
  }
}

@media print {
  @page {
    size: A4 portrait;
    margin: 14mm 14mm 18mm;
  }

  :global(html),
  :global(body),
  :global(#app) {
    background: #ffffff !important;
    color: #111111 !important;
  }

  :global(body) {
    min-width: 0;
  }

  :global(.app-sidebar),
  :global(.app-header),
  :global(.app-footer),
  :global(.el-overlay),
  :global(.el-loading-mask),
  :global(.report-page-toolbar),
  :global(.public-report-toolbar),
  .no-print {
    display: none !important;
  }

  :global(.shell),
  :global(.shell-main),
  :global(.page-container) {
    display: block !important;
    width: 100% !important;
    max-width: none !important;
    min-height: auto !important;
    margin: 0 !important;
    padding: 0 !important;
    background: #ffffff !important;
  }

  .audit-report {
    width: 100%;
    max-width: none;
    border: 0;
    color: #111111;
  }

  .report-document-header {
    padding: 0 0 9mm;
    border-top: 2pt solid #111111;
  }

  .document-title-row h1 {
    font-size: 26pt;
  }

  .report-metadata {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .audit-report--public .report-metadata {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .report-section {
    padding: 8mm 0;
    border-top-color: #777777;
    animation: none;
  }

  :deep(.section-heading) {
    margin-bottom: 6mm;
    break-inside: avoid;
    break-after: avoid;
    page-break-inside: avoid;
    page-break-after: avoid;
  }

  :deep(.section-heading h2) {
    font-size: 16pt;
  }

  .executive-copy {
    font-size: 11pt;
  }

  .audit-ledgers,
  .audit-brief-grid,
  .ai-overview-grid,
  .methodology-grid {
    break-inside: auto;
    page-break-inside: auto;
  }

  .audit-ledgers > section,
  .audit-brief-grid > section,
  .ai-overview-grid > section,
  .methodology-grid > section,
  .resume-draft header,
  .interview-list li {
    break-inside: avoid;
    page-break-inside: avoid;
  }

  .resume-draft {
    break-inside: auto;
    page-break-inside: auto;
  }

  .document-footer {
    padding: 7mm 0 0;
    border-top: 1pt solid #111111;
  }

  * {
    print-color-adjust: economy;
    -webkit-print-color-adjust: economy;
  }
}
</style>
