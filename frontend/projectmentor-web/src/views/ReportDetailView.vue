<template>
  <div class="page-stack report-print-root" v-loading="loading">
    <section class="panel report-cover">
      <div class="panel-body report-hero">
        <div class="report-identity">
          <p class="eyebrow">{{ t('report.eyebrow') }}</p>
          <h1>{{ reportProjectName }}</h1>
          <div class="report-meta-grid">
            <span>
              {{ t('common.reportId') }}
              <strong>#{{ report?.id || reportId }}</strong>
            </span>
            <span>
              {{ t('common.projectId') }}
              <strong>#{{ report?.projectId || '-' }}</strong>
            </span>
            <span>
              {{ t('common.techStack') }}
              <strong>{{ reportTechStack }}</strong>
            </span>
            <span>
              {{ t('common.createTime') }}
              <strong>{{ report?.createTime || '-' }}</strong>
            </span>
          </div>
          <p class="report-notice">
            {{ t('report.notice') }}
          </p>
        </div>
        <div class="report-score-card">
          <ScoreRing :score="report?.totalScore ?? 0" :title="t('common.totalScore')" />
        </div>
      </div>
      <div class="report-actions no-print">
        <el-button class="report-print-button" type="primary" :icon="Printer" @click="handlePrint">
          {{ t('common.printPdf') }}
        </el-button>
        <el-button
          :type="shareInfo?.enabled ? 'default' : 'primary'"
          :icon="shareInfo?.enabled ? Refresh : Link"
          :loading="shareLoading"
          @click="handleCreateShare"
        >
          {{ shareInfo?.enabled ? t('report.refreshShare') : t('report.createShare') }}
        </el-button>
        <el-button
          v-if="shareInfo?.enabled && fullShareUrl"
          :icon="CopyDocument"
          @click="handleCopyShare"
        >
          {{ t('report.copyShare') }}
        </el-button>
        <el-button
          v-if="shareInfo?.enabled"
          type="danger"
          plain
          :icon="Close"
          :loading="shareLoading"
          @click="handleDisableShare"
        >
          {{ t('report.disableShare') }}
        </el-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('report.summaryTitle') }}</h3>
          <p class="muted">{{ t('report.summaryDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <p class="report-summary-text">{{ report?.summary || t('report.summaryLoading') }}</p>
      </div>
    </section>

    <section v-if="shareInfo?.enabled && fullShareUrl" class="panel no-print">
      <div class="panel-title">
        <div>
          <h3>{{ t('report.shareTitle') }}</h3>
          <p class="muted">{{ t('report.shareDesc') }}</p>
        </div>
      </div>
      <div class="panel-body share-link-row">
        <el-input :model-value="fullShareUrl" readonly>
          <template #append>
            <el-button :icon="CopyDocument" @click="handleCopyShare">{{ t('common.copy') }}</el-button>
          </template>
        </el-input>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('report.radarTitle') }}</h3>
          <p class="muted">{{ t('report.radarDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <RadarScoreChart :scores="radarScores" />
        <div class="print-score-list print-only">
          <article v-for="item in scoreRows" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ formatScore(item.value) }}</strong>
          </article>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <h3>{{ t('report.contentTitle') }}</h3>
      </div>
      <div class="panel-body report-content-grid">
        <article>
          <h4>{{ t('report.strengths') }}</h4>
          <MarkdownBlock :content="report?.strengths" />
        </article>
        <article>
          <h4>{{ t('report.weaknesses') }}</h4>
          <MarkdownBlock :content="report?.weaknesses" />
        </article>
        <article>
          <h4>{{ t('common.suggestions') }}</h4>
          <MarkdownBlock :content="report?.suggestions" />
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('common.risks') }}</h3>
          <p class="muted">{{ t('report.risksDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <RiskList :risks="report?.riskPoints" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('common.evidence') }}</h3>
          <p class="muted">{{ t('report.evidenceDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <EvidenceList :evidences="report?.evidenceChain" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('report.claimEvidenceTitle') }}</h3>
          <p class="muted">{{ t('report.claimEvidenceDesc') }}</p>
        </div>
        <div class="claim-ai-action no-print">
          <el-button
            type="primary"
            plain
            :icon="MagicStick"
            :loading="claimAiLoading"
            @click="handleClaimAiEnhance"
          >
            {{ claimAiLoading ? t('report.claimAiAnalyzing') : t('report.claimAiButton') }}
          </el-button>
          <span>{{ t('report.claimAiCost') }}</span>
        </div>
      </div>
      <div
        class="panel-body"
        v-loading="claimAiLoading"
        :element-loading-text="t('report.claimAiLoading')"
      >
        <ClaimEvidenceMatrix
          :claims="report?.claimEvidenceList"
          :ai-enhancement="report?.claimEvidenceAi"
        />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('report.resumeTitle') }}</h3>
          <p class="muted">{{ t('report.resumeDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <div class="resume-copy-grid">
          <article v-for="section in resumeSections" :key="section.title" class="resume-copy-card">
            <div class="resume-copy-head">
              <div>
                <h4>{{ section.title }}</h4>
                <span>{{ section.description }}</span>
              </div>
              <el-button size="small" :icon="CopyDocument" @click="handleCopyResume(section)">
                {{ t('report.copyResume') }}
              </el-button>
            </div>
            <el-alert
              v-if="resumeNeedsAttention(section.content)"
              class="resume-boundary-alert"
              :title="t('report.resumeBoundaryNotice')"
              type="warning"
              show-icon
              :closable="false"
            />
            <MarkdownBlock :content="section.content" />
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, CopyDocument, Link, MagicStick, Printer, Refresh } from '@element-plus/icons-vue'

import { enhanceClaimEvidence, getReportDetail } from '@/api/analysis'
import { getMyCredits } from '@/api/credit'
import { getProjectDetail } from '@/api/project'
import { createReportShare, disableReportShare, getReportShare } from '@/api/share'
import ClaimEvidenceMatrix from '@/components/ClaimEvidenceMatrix.vue'
import EvidenceList from '@/components/EvidenceList.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import RadarScoreChart from '@/components/RadarScoreChart.vue'
import RiskList from '@/components/RiskList.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import { useUserStore } from '@/stores/user'
import type { AnalysisReport, Project, ReportShare } from '@/types/api'

const route = useRoute()
const { t } = useI18n()
const reportId = Number(route.params.id)
const loading = ref(false)
const shareLoading = ref(false)
const claimAiLoading = ref(false)
const report = ref<AnalysisReport>()
const project = ref<Project>()
const shareInfo = ref<ReportShare>()
const userStore = useUserStore()

const reportProjectName = computed(() => {
  if (project.value?.name) {
    return project.value.name
  }

  if (report.value?.projectId) {
    return `${t('common.projectId')} #${report.value.projectId}`
  }

  return t('common.untitledReport')
})

const reportTechStack = computed(() => project.value?.techStack || t('common.notFilled'))

const fullShareUrl = computed(() => {
  const token = shareInfo.value?.shareToken

  if (!shareInfo.value?.enabled || !token) {
    return ''
  }

  return `${window.location.origin}/share/reports/${token}`
})

const radarScores = computed(() => ({
  runnabilityScore: report.value?.runnabilityScore,
  authenticityScore: report.value?.authenticityScore,
  structureScore: report.value?.structureScore,
  readmeScore: report.value?.readmeScore,
  securityScore: report.value?.securityScore,
  engineeringScore: report.value?.engineeringScore,
  interviewScore: report.value?.interviewScore
}))

const scoreRows = computed(() => [
  { label: t('report.scores.runnability'), value: report.value?.runnabilityScore },
  { label: t('report.scores.authenticity'), value: report.value?.authenticityScore },
  { label: t('report.scores.structure'), value: report.value?.structureScore },
  { label: t('report.scores.readme'), value: report.value?.readmeScore },
  { label: t('report.scores.security'), value: report.value?.securityScore },
  { label: t('report.scores.engineering'), value: report.value?.engineeringScore },
  { label: t('report.scores.interview'), value: report.value?.interviewScore }
])

const resumeSections = computed(() => [
  {
    title: t('report.resumeBasic'),
    description: t('report.resumeBasicDesc'),
    content: report.value?.resumeBasic
  },
  {
    title: t('report.resumeStandard'),
    description: t('report.resumeStandardDesc'),
    content: report.value?.resumeStandard
  },
  {
    title: t('report.resumeAdvanced'),
    description: t('report.resumeAdvancedDesc'),
    content: report.value?.resumeAdvanced
  }
])

type ResumeSection = (typeof resumeSections.value)[number]

function formatScore(value?: number) {
  if (!Number.isFinite(value)) {
    return '-'
  }

  return `${Math.round(Number(value))} ${t('common.points')}`
}

async function loadReport() {
  loading.value = true
  try {
    report.value = await getReportDetail(reportId)
    project.value = undefined

    if (report.value.projectId) {
      try {
        project.value = await getProjectDetail(report.value.projectId)
      } catch {
        project.value = undefined
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadShareInfo() {
  shareLoading.value = true
  try {
    shareInfo.value = await getReportShare(reportId)
  } finally {
    shareLoading.value = false
  }
}

async function handleClaimAiEnhance() {
  if (!report.value?.claimEvidenceList?.length) {
    ElMessage.warning(t('report.noClaimEvidence'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('report.claimAiConfirm'),
      t('report.claimAiConfirmTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }

  claimAiLoading.value = true
  try {
    report.value = await enhanceClaimEvidence(reportId)
    await syncCredits()
    ElMessage.success(t('report.claimAiSuccess'))
  } catch {
    await syncCredits()
  } finally {
    claimAiLoading.value = false
  }
}

async function syncCredits() {
  try {
    const credits = await getMyCredits()
    userStore.updateCredits(credits.remainingCredits)
  } catch {
    // Header balance will refresh on the next normal credit fetch.
  }
}

async function handleCreateShare() {
  const wasEnabled = Boolean(shareInfo.value?.enabled)
  shareLoading.value = true

  try {
    shareInfo.value = await createReportShare(reportId)
    ElMessage.success(wasEnabled ? t('report.shareRefreshed') : t('report.shareCreated'))
  } finally {
    shareLoading.value = false
  }
}

async function handleCopyShare() {
  if (!fullShareUrl.value) {
    return
  }

  const copied = await copyText(fullShareUrl.value)
  if (copied) {
    ElMessage.success(t('report.shareCopied'))
    return
  }

  ElMessage.error(t('report.shareCopyFailed'))
}

async function handleCopyResume(section: ResumeSection) {
  const content = [
    section.title,
    section.description,
    '',
    section.content || ''
  ].join('\n')

  const copied = await copyText(content)
  if (copied) {
    ElMessage.success(t('report.resumeCopied'))
    return
  }

  ElMessage.error(t('report.resumeCopyFailed'))
}

function resumeNeedsAttention(content?: string) {
  if (!content) {
    return false
  }

  return /证据不足|不建议|缺少证据|风险|not recommend|insufficient evidence|risk/i.test(content)
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // HTTP 公网 IP 场景可能没有 Clipboard 权限，继续走 textarea fallback。
  }

  return fallbackCopyText(text)
}

function fallbackCopyText(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  textarea.style.left = '-9999px'
  textarea.style.opacity = '0'

  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  let copied = false
  try {
    copied = document.execCommand('copy')
  } catch {
    copied = false
  } finally {
    document.body.removeChild(textarea)
  }

  return copied
}

async function handleDisableShare() {
  shareLoading.value = true

  try {
    await disableReportShare(reportId)
    shareInfo.value = {
      reportId,
      enabled: false
    }
    ElMessage.success(t('report.shareDisabled'))
  } finally {
    shareLoading.value = false
  }
}

function handlePrint() {
  ElMessage.info(t('report.printTip'))
  window.setTimeout(() => window.print(), 100)
}

onMounted(() => {
  loadReport()
  loadShareInfo()
})
</script>

<style scoped>
.print-only {
  display: none;
}

.report-cover {
  overflow: hidden;
}

.report-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
}

.report-identity h1 {
  margin: 8px 0 16px;
  color: var(--pm-ink);
  font-size: 30px;
  line-height: 1.2;
}

.report-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.report-meta-grid span {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #fbfdff;
  color: var(--pm-muted);
  font-size: 12px;
}

.report-meta-grid strong {
  display: block;
  margin-top: 5px;
  color: #344054;
  font-size: 14px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.report-notice {
  margin: 16px 0 0;
  padding: 12px 14px;
  border: 1px solid rgba(31, 111, 235, 0.18);
  border-radius: 8px;
  background: #eef6ff;
  color: #245089;
  line-height: 1.7;
}

.report-score-card {
  padding: 12px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #ffffff;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--pm-border);
  background: #fbfdff;
}

.claim-ai-action {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.claim-ai-action span {
  color: var(--pm-muted);
  font-size: 12px;
}

.report-print-button {
  flex: 0 0 auto;
}

.share-link-row {
  display: block;
}

.report-summary-text {
  margin: 0;
  color: #344054;
  font-size: 17px;
  line-height: 1.85;
}

.report-content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.report-content-grid article {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.report-content-grid h4 {
  margin: 0 0 12px;
}

.print-score-list,
.resume-copy-grid {
  gap: 12px;
}

.print-score-list article,
.resume-copy-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.print-score-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.print-score-list span {
  color: var(--pm-muted);
}

.print-score-list strong {
  color: var(--pm-ink);
  font-size: 18px;
}

.resume-copy-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.resume-copy-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-copy-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.resume-copy-head h4 {
  margin: 0;
  color: var(--pm-ink);
}

.resume-copy-head span {
  display: block;
  margin-top: 6px;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.5;
}

.resume-boundary-alert {
  --el-alert-padding: 8px 10px;
}

@media (max-width: 920px) {
  .report-meta-grid,
  .report-content-grid,
  .resume-copy-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .report-hero {
    grid-template-columns: 1fr;
  }

  .report-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .claim-ai-action {
    justify-content: flex-start;
    width: 100%;
  }

  .report-identity h1 {
    font-size: 24px;
  }
}

@media print {
  @page {
    margin: 14mm;
  }

  :global(html),
  :global(body),
  :global(#app) {
    background: #ffffff !important;
    color: #111827 !important;
  }

  :global(body) {
    min-width: 0;
  }

  :global(.app-sidebar),
  :global(.app-header),
  :global(.el-overlay),
  :global(.el-loading-mask),
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

  .report-print-root {
    display: block;
    color: #111827;
    background: #ffffff;
  }

  .panel {
    margin: 0 0 12px;
    break-inside: avoid;
    page-break-inside: avoid;
    border: 1px solid #d0d5dd;
    background: #ffffff !important;
    box-shadow: none !important;
  }

  .panel-title,
  .panel-body {
    padding: 14px 16px;
  }

  .panel-title {
    border-bottom-color: #d0d5dd;
  }

  .report-hero,
  .report-content-grid,
  .print-score-list,
  .resume-copy-grid {
    display: grid !important;
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .report-identity h1 {
    color: #111827;
    font-size: 22px;
  }

  .muted,
  .eyebrow,
  :deep(.el-tag),
  :deep(.source-file) {
    color: #344054 !important;
  }

  .report-content-grid article,
  .print-score-list article,
  .resume-copy-card,
  :deep(.risk-card),
  :deep(.evidence-card),
  :deep(.empty-state),
  :deep(.text-block) {
    break-inside: avoid;
    page-break-inside: avoid;
    border-color: #d0d5dd !important;
    background: #ffffff !important;
    box-shadow: none !important;
  }

  :deep(.radar-chart) {
    min-height: 280px;
    break-inside: avoid;
    page-break-inside: avoid;
  }

  :deep(.markdown-body),
  :deep(.risk-card),
  :deep(.evidence-card),
  :deep(.text-block) {
    color: #111827 !important;
  }

  :deep(.text-block) {
    white-space: pre-wrap;
  }

  * {
    print-color-adjust: exact;
    -webkit-print-color-adjust: exact;
  }
}
</style>
