<template>
  <div class="project-evidence-page">
    <header class="project-header" aria-labelledby="project-title">
      <div v-if="loading" class="project-header-loading" aria-live="polite">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else-if="projectError" class="project-header-error" role="alert">
        <div>
          <strong>{{ t('projects.v5.errors.project') }}</strong>
          <p>{{ t('projects.v5.workspaceDescription') }}</p>
        </div>
        <el-button @click="loadProject">{{ t('projects.v5.retry') }}</el-button>
      </div>
      <template v-else>
        <div class="project-title-row">
          <div class="project-title-copy">
            <div class="project-title-line">
              <h2 id="project-title">{{ project?.name || t('common.unnamedProject') }}</h2>
              <StatusLabel v-if="project?.status" :status="project.status" :label="projectStatusLabel(project.status)" />
            </div>
            <p>{{ project?.description || t('projects.noDescription') }}</p>
          </div>
          <div class="project-primary-actions">
            <el-button v-if="showDefenseEntry" class="defense-entry" @click="openDefense">
              {{ t('defense.entry') }}
            </el-button>
            <div class="audit-action">
              <el-button type="primary" :loading="analyzing" :disabled="activeTask" @click="handleStartAnalyze">
                {{ t('projects.v5.runAudit') }}
              </el-button>
              <span>{{ t('projects.v5.auditCost', { count: AI_CREDIT_COSTS.AUDIT_REPORT }) }}</span>
            </div>
            <el-dropdown trigger="click" @command="handleProjectCommand">
              <el-button :icon="MoreFilled" :aria-label="t('projects.v5.moreActions')" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="reports">{{ t('projects.v5.viewReports') }}</el-dropdown-item>
                  <el-dropdown-item command="claim-check">{{ t('projects.v5.checkClaims') }}</el-dropdown-item>
                  <el-dropdown-item v-if="project?.githubUrl" command="github">{{ t('projects.v5.openGithub') }}</el-dropdown-item>
                  <el-dropdown-item divided command="delete" :disabled="activeTask">{{ t('projects.v5.deleteProject') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <div class="project-metadata" :aria-label="t('projects.v5.metadataLabel')">
          <span v-if="project?.projectType">{{ project.projectType }}</span>
          <span v-if="project?.techStack">{{ project.techStack }}</span>
          <span>{{ t('projects.v5.updated', { time: formatDate(project?.updateTime || project?.createTime) }) }}</span>
          <span>{{ t('projects.v5.filesCount', { count: files.length }) }}</span>
          <span>{{ t('projects.v5.reportsCount', { count: reportTotal }) }}</span>
        </div>
      </template>
      <TaskProgress v-if="task" :task="task" :interrupted="pollingInterrupted" @retry="resumePolling" />
    </header>

    <nav class="project-navigation" :aria-label="t('projects.v5.navigationLabel')">
      <button
        v-for="item in navigationItems"
        :key="item.value"
        type="button"
        :class="{ 'is-active': activeSection === item.value }"
        :aria-current="activeSection === item.value ? 'page' : undefined"
        @click="activeSection = item.value"
      >
        {{ item.label }}
      </button>
    </nav>

    <main class="project-workspace-content">
      <ProjectEvidenceWorkspace
        v-if="activeSection === 'evidence'"
        :files="files"
        :claims="latestReport?.claimEvidenceList || []"
        :loading="reportLoading"
        :error="reportError"
        :files-loading="fileLoading"
        :files-error="fileError"
        :report-id="latestReport?.id"
        :report-created-at="latestReport?.createTime"
        @retry="loadReportHistory"
        @retry-files="loadFiles"
        @run-audit="handleStartAnalyze"
        @open-sources="activeSection = 'files'"
        @view-report="openReport"
      />

      <section v-else-if="activeSection === 'overview'" class="overview-surface" aria-labelledby="project-overview-title">
        <header class="section-heading">
          <div>
            <h2 id="project-overview-title">{{ t('projects.v5.overview.title') }}</h2>
            <p>{{ t('projects.v5.overview.description') }}</p>
          </div>
        </header>
        <div class="overview-grid">
          <section class="overview-section project-brief">
            <h3>{{ t('projects.v5.overview.brief') }}</h3>
            <p>{{ project?.description || t('projects.noDescription') }}</p>
            <dl>
              <div v-if="project?.projectType"><dt>{{ t('common.projectType') }}</dt><dd>{{ project.projectType }}</dd></div>
              <div v-if="project?.techStack"><dt>{{ t('common.techStack') }}</dt><dd>{{ project.techStack }}</dd></div>
              <div v-if="project?.githubUrl">
                <dt>{{ t('common.github') }}</dt>
                <dd><a :href="project.githubUrl" target="_blank" rel="noreferrer">{{ project.githubUrl }}</a></dd>
              </div>
            </dl>
          </section>
          <section class="overview-section latest-audit">
            <div class="overview-section-heading">
              <h3>{{ t('projects.v5.overview.latestAudit') }}</h3>
              <el-button v-if="latestReport" text @click="openReport(latestReport.id)">{{ t('projects.v5.overview.viewAudit') }}</el-button>
            </div>
            <div v-if="reportLoading" class="overview-loading"><el-skeleton :rows="4" animated /></div>
            <div v-else-if="reportError" class="inline-error" role="alert">
              <span>{{ t('projects.v5.errors.reports') }}</span>
              <el-button text @click="loadReportHistory">{{ t('projects.v5.retry') }}</el-button>
            </div>
            <template v-else-if="latestReport">
              <dl class="audit-ledger">
                <div v-if="latestReport.totalScore !== undefined"><dt>{{ t('projects.v5.overview.totalScore') }}</dt><dd>{{ formatScore(latestReport.totalScore) }}</dd></div>
                <div><dt>{{ t('projects.v5.overview.claims') }}</dt><dd>{{ claimSummary.total }}</dd></div>
                <div><dt>{{ t('projects.v5.overview.supported') }}</dt><dd>{{ claimSummary.supported }}</dd></div>
                <div><dt>{{ t('projects.v5.overview.attention') }}</dt><dd>{{ claimSummary.attention }}</dd></div>
              </dl>
              <p v-if="latestReport.summary" class="latest-audit-summary">{{ latestReport.summary }}</p>
            </template>
            <EmptyState v-else variant="compact" :title="t('projects.v5.overview.noAudit')" :description="t('projects.v5.overview.noAuditDescription')">
              <el-button type="primary" @click="handleStartAnalyze">{{ t('projects.v5.runAudit') }}</el-button>
            </EmptyState>
          </section>
        </div>

        <section class="rule-scan-section" aria-labelledby="rule-scan-title">
          <div class="rule-scan-heading">
            <div>
              <h3 id="rule-scan-title">{{ t('projects.v5.overview.freeScan') }}</h3>
              <p>{{ t('projects.v5.overview.freeScanDescription') }}</p>
            </div>
            <el-button :loading="scanning" @click="handleScan">{{ t('projects.v5.overview.runFreeScan') }}</el-button>
          </div>
          <template v-if="scanResult">
            <dl class="scan-ledger">
              <div><dt>{{ t('projects.v5.overview.riskTotal') }}</dt><dd>{{ scanResult.totalRiskCount }}</dd></div>
              <div><dt>{{ t('projects.v5.overview.highRisk') }}</dt><dd>{{ scanResult.highRiskCount }}</dd></div>
              <div><dt>{{ t('projects.v5.overview.mediumRisk') }}</dt><dd>{{ scanResult.mediumRiskCount }}</dd></div>
              <div><dt>{{ t('projects.v5.overview.lowRisk') }}</dt><dd>{{ scanResult.lowRiskCount }}</dd></div>
            </dl>
            <div class="scan-results-grid">
              <section>
                <h4>{{ t('projects.v5.overview.riskFindings') }}</h4>
                <ul v-if="scanResult.risks.length" class="scan-list">
                  <li v-for="(risk, index) in scanResult.risks" :key="`${risk.riskType}-${index}`">
                    <StatusLabel :status="risk.riskLevel" :label="risk.riskLevel" />
                    <strong>{{ risk.message }}</strong>
                    <code v-if="risk.sourceFile">{{ risk.sourceFile }}</code>
                    <p v-if="risk.suggestion">{{ risk.suggestion }}</p>
                  </li>
                </ul>
                <p v-else class="muted">{{ t('projects.v5.overview.noRisks') }}</p>
              </section>
              <section>
                <h4>{{ t('projects.v5.overview.ruleEvidence') }}</h4>
                <ul class="scan-list">
                  <li v-for="(evidence, index) in scanResult.evidences" :key="`${evidence.conclusion}-${index}`">
                    <strong>{{ evidence.conclusion }}</strong>
                    <code v-if="evidence.sourceFile">{{ evidence.sourceFile }}</code>
                    <p v-if="evidence.detail || evidence.evidence">{{ evidence.detail || evidence.evidence }}</p>
                  </li>
                </ul>
              </section>
            </div>
            <details v-if="scanResult.suggestions.length" class="scan-suggestions">
              <summary>{{ t('projects.v5.overview.suggestions') }}</summary>
              <ul><li v-for="suggestion in scanResult.suggestions" :key="suggestion">{{ suggestion }}</li></ul>
            </details>
          </template>
        </section>
      </section>

      <ProjectSourceManager
        v-else-if="activeSection === 'files'"
        :project-id="projectId"
        :files="files"
        :loading="fileLoading"
        :error="fileError"
        @refresh="loadFiles"
      />

      <section v-else-if="activeSection === 'qa'" class="qa-surface" aria-labelledby="evidence-qa-title">
        <header class="section-heading">
          <div><h2 id="evidence-qa-title">{{ t('projects.v5.qa.title') }}</h2><p>{{ t('projects.v5.qa.description') }}</p></div>
        </header>
        <ProjectQaPanel
          :project-id="projectId"
          :project-name="project?.name || t('common.unnamedProject')"
          :has-project-files="files.length > 0"
          :project-files="files"
          :claims="latestReport?.claimEvidenceList || []"
          @open-sources="activeSection = 'files'"
        />
      </section>

      <section v-else class="interview-surface" aria-labelledby="interview-preparation-title">
        <header class="section-heading interview-heading">
          <div><h2 id="interview-preparation-title">{{ t('projects.v5.interview.title') }}</h2><p>{{ t('projects.v5.interview.description') }}</p></div>
          <el-button type="primary" @click="router.push(`/interview?projectId=${projectId}`)">{{ t('projects.v5.interview.start') }}</el-button>
        </header>
        <div v-if="interviewLoading" class="interview-loading"><el-skeleton :rows="5" animated /></div>
        <div v-else-if="interviewError" class="inline-error" role="alert">
          <span>{{ t('projects.v5.errors.interviews') }}</span>
          <el-button text @click="loadInterviews">{{ t('projects.v5.retry') }}</el-button>
        </div>
        <div v-else-if="recentInterviews.length" class="interview-list">
          <article v-for="session in recentInterviews" :key="session.sessionId">
            <div>
              <StatusLabel :status="session.status" :label="interviewStatusLabel(session.status)" />
              <strong>{{ t('projects.v5.interview.questions', { answered: session.answeredCount, total: session.questionCount }) }}</strong>
              <span>{{ formatDate(session.updateTime || session.createTime) }}</span>
            </div>
            <span v-if="session.totalScore !== undefined" class="interview-score">{{ formatScore(session.totalScore) }}</span>
            <el-button text @click="router.push(`/interview?sessionId=${session.sessionId}`)">{{ t('projects.v5.interview.view') }}</el-button>
          </article>
        </div>
        <EmptyState v-else variant="compact" :title="t('projects.v5.interview.noSessions')" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { MoreFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getReportDetail, getTask, listMyReports, scanProject, startAnalyze } from '@/api/analysis'
import { getMyCredits } from '@/api/credit'
import { listInterviewSessions } from '@/api/interview'
import { deleteProject, getProjectDetail, listProjectFiles } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import ProjectEvidenceWorkspace from '@/components/ProjectEvidenceWorkspace.vue'
import ProjectQaPanel from '@/components/ProjectQaPanel.vue'
import ProjectSourceManager from '@/components/ProjectSourceManager.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import TaskProgress from '@/components/TaskProgress.vue'
import { AI_CREDIT_COSTS } from '@/constants/creditCosts'
import { useUserStore } from '@/stores/user'
import { useExperienceStore } from '@/stores/experience'
import type { AnalysisReport, AnalysisTask, InterviewSessionListItem, Project, ProjectFile, ReportListItem, RuleScanResult } from '@/types/api'

type ProjectSection = 'overview' | 'evidence' | 'files' | 'qa' | 'interview'
type ProjectCommand = 'reports' | 'claim-check' | 'github' | 'delete'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const experienceStore = useExperienceStore()
const projectId = Number(route.params.id)

const activeSection = ref<ProjectSection>('evidence')
const loading = ref(false)
const projectError = ref(false)
const fileLoading = ref(false)
const fileError = ref(false)
const reportLoading = ref(false)
const reportError = ref(false)
const interviewLoading = ref(false)
const interviewError = ref(false)
const scanning = ref(false)
const analyzing = ref(false)
const pollingInterrupted = ref(false)
const project = ref<Project>()
const files = ref<ProjectFile[]>([])
const recentReports = ref<ReportListItem[]>([])
const reportTotal = ref(0)
const recentInterviews = ref<InterviewSessionListItem[]>([])
const latestReport = ref<AnalysisReport>()
const scanResult = ref<RuleScanResult>()
const task = ref<AnalysisTask>()
let pollTimer: number | undefined

const navigationItems = computed<Array<{ value: ProjectSection; label: string }>>(() => [
  { value: 'overview', label: t('projects.v5.nav.overview') },
  { value: 'evidence', label: t('projects.v5.nav.evidence') },
  { value: 'files', label: t('projects.v5.nav.files') },
  { value: 'qa', label: t('projects.v5.nav.qa') },
  { value: 'interview', label: t('projects.v5.nav.interview') }
])

const activeTask = computed(() => ['PENDING', 'RUNNING'].includes(task.value?.status || ''))
const showDefenseEntry = computed(() => experienceStore.activeExperienceMode === 'workbench')
const claimSummary = computed(() => {
  const claims = latestReport.value?.claimEvidenceList || []
  const supported = claims.filter((claim) => claim.status === 'SUPPORTED').length
  return { total: claims.length, supported, attention: claims.length - supported }
})

async function loadProject() {
  loading.value = true
  projectError.value = false
  try { project.value = await getProjectDetail(projectId) } catch { projectError.value = true } finally { loading.value = false }
}

async function loadFiles() {
  fileLoading.value = true
  fileError.value = false
  try { files.value = await listProjectFiles(projectId) } catch { fileError.value = true } finally { fileLoading.value = false }
}

async function loadReportHistory() {
  reportLoading.value = true
  reportError.value = false
  try {
    const page = await listMyReports({ projectId, page: 1, size: 5 })
    recentReports.value = page.records
    reportTotal.value = page.total
    latestReport.value = page.records[0] ? await getReportDetail(page.records[0].reportId) : undefined
  } catch { reportError.value = true } finally { reportLoading.value = false }
}

async function loadInterviews() {
  interviewLoading.value = true
  interviewError.value = false
  try { recentInterviews.value = (await listInterviewSessions({ projectId, page: 1, size: 5 })).records }
  catch { interviewError.value = true } finally { interviewLoading.value = false }
}

async function handleScan() {
  scanning.value = true
  try { scanResult.value = await scanProject(projectId); ElMessage.success(t('projects.scanDone')) } finally { scanning.value = false }
}

async function handleStartAnalyze() {
  if (activeTask.value) return
  try {
    await ElMessageBox.confirm(t('credits.confirmAiUse', { count: AI_CREDIT_COSTS.AUDIT_REPORT }), t('report.claimAiConfirmTitle'), {
      confirmButtonText: t('common.confirm'), cancelButtonText: t('common.cancel'), type: 'warning'
    })
  } catch { return }
  analyzing.value = true
  try {
    task.value = await startAnalyze(projectId)
    pollingInterrupted.value = false
    activeSection.value = 'evidence'
    ElMessage.success(t('projects.taskStarted'))
    startPolling(task.value.taskId)
  } finally { analyzing.value = false }
}

function startPolling(taskId: number) {
  clearPolling()
  pollTimer = window.setInterval(async () => {
    try {
      const latestTask = await getTask(taskId)
      task.value = latestTask
      pollingInterrupted.value = false
      if (latestTask.status === 'SUCCESS') {
        clearPolling()
        await Promise.allSettled([syncCredits(), loadProject(), loadReportHistory()])
        ElMessage.success(t('projects.reportDone'))
      } else if (latestTask.status === 'FAILED') {
        clearPolling()
        await syncCredits()
        ElMessage.error(latestTask.failReason || t('status.failed'))
      }
    } catch { clearPolling(); pollingInterrupted.value = true }
  }, 1500)
}

function resumePolling() {
  if (task.value?.taskId) { pollingInterrupted.value = false; startPolling(task.value.taskId) }
}

async function syncCredits() {
  try { userStore.updateCredits((await getMyCredits()).remainingCredits) } catch { /* Refresh on next normal fetch. */ }
}

function clearPolling() {
  if (pollTimer) { window.clearInterval(pollTimer); pollTimer = undefined }
}

function handleProjectCommand(command: ProjectCommand) {
  if (command === 'reports') return void router.push(`/reports?projectId=${projectId}`)
  if (command === 'claim-check') return void router.push(`/hallucination?projectId=${projectId}`)
  if (command === 'github' && project.value?.githubUrl) return void window.open(project.value.githubUrl, '_blank', 'noopener,noreferrer')
  if (command === 'delete') handleDeleteProject()
}

async function handleDeleteProject() {
  if (activeTask.value) { ElMessage.warning(t('projects.v5.deleteBlocked')); return }
  try {
    await ElMessageBox.confirm(t('projects.deleteConfirm'), t('projects.deleteTitle'), {
      type: 'warning', confirmButtonText: t('common.delete'), cancelButtonText: t('common.cancel')
    })
  } catch { return }
  await deleteProject(projectId)
  ElMessage.success(t('projects.deleted'))
  router.push('/projects')
}

function openReport(reportId: number) { router.push(`/reports/${reportId}`) }
function openDefense() { router.push(`/projects/${projectId}/defense`) }
function formatDate(value?: string) { return value ? String(value).replace('T', ' ').slice(0, 19) : '—' }
function formatScore(value?: number) { return Number.isFinite(value) ? Math.round(Number(value)) : '—' }

function projectStatusLabel(status: string) {
  const keyMap: Record<string, string> = { PENDING: 'status.pending', ANALYZING: 'status.analyzing', FINISHED: 'status.finished', FAILED: 'status.failed' }
  return keyMap[status] ? t(keyMap[status]) : status.replace(/_/g, ' ')
}

function interviewStatusLabel(status?: string) {
  if (!status) return t('status.waiting')
  const keyMap: Record<string, string> = { RUNNING: 'status.running', FINISHED: 'status.finished', FAILED: 'status.failed' }
  return keyMap[status] ? t(keyMap[status]) : status.replace(/_/g, ' ')
}

onMounted(() => { loadProject(); loadFiles(); loadReportHistory(); loadInterviews() })
onUnmounted(clearPolling)
</script>

<style scoped>
.project-evidence-page { min-width: 0; }
.project-evidence-page :deep(.el-button) { min-height: 44px; }
.project-header { padding: 4px 0 22px; border-bottom: 1px solid var(--pm-stone-strong); }
.project-header-loading, .project-header-error { min-height: 126px; }
.project-header-error, .inline-error { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; }
.project-header-error strong, .inline-error { color: var(--pm-risk); }
.project-header-error p { margin: 6px 0 0; color: var(--pm-muted); line-height: 1.6; }
.project-title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 32px; }
.project-title-copy { min-width: 0; }
.project-title-line { display: flex; min-width: 0; align-items: center; gap: 14px; }
.project-title-line h2 { min-width: 0; margin: 0; overflow-wrap: anywhere; color: var(--pm-ink); font-size: var(--pm-type-project-title); font-weight: 600; letter-spacing: -0.03em; line-height: 1.08; }
.project-title-copy > p { max-width: 72ch; margin: 10px 0 0; color: var(--pm-graphite); font-size: 15px; line-height: 1.65; }
.project-primary-actions { display: flex; flex: 0 0 auto; align-items: flex-start; gap: 8px; }
.project-primary-actions :deep(.el-button) { min-height: 44px; }
.audit-action { display: grid; justify-items: end; gap: 6px; }
.audit-action span { color: var(--pm-muted); font-family: var(--pm-font-mono); font-size: 9px; line-height: 1.4; }
.project-metadata { display: flex; flex-wrap: wrap; gap: 8px 22px; margin-top: 18px; color: var(--pm-muted); font-family: var(--pm-font-mono); font-size: 10px; line-height: 1.5; }
.project-navigation { position: sticky; z-index: 10; top: 76px; display: flex; overflow-x: auto; border-bottom: 1px solid var(--pm-stone); background: var(--pm-paper); scrollbar-width: none; }
.project-navigation::-webkit-scrollbar { display: none; }
.project-navigation button { position: relative; min-width: max-content; min-height: 50px; padding: 0 18px; border: 0; background: transparent; color: var(--pm-muted); cursor: pointer; font: 600 12px var(--pm-font-sans); transition: color var(--pm-motion-fast) ease; }
.project-navigation button:first-child { padding-left: 0; }
.project-navigation button::after { position: absolute; inset: auto 18px -1px; height: 1px; background: transparent; content: ''; }
.project-navigation button:first-child::after { left: 0; }
.project-navigation button:hover, .project-navigation button.is-active { color: var(--pm-ink); }
.project-navigation button.is-active::after { background: var(--pm-ink); }
.project-workspace-content { padding-top: 28px; }
.section-heading, .interview-heading, .rule-scan-heading, .overview-section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; }
.section-heading { margin-bottom: 22px; }
.section-heading h2, .overview-surface h3, .rule-scan-section h3, .scan-results-grid h4 { margin: 0; color: var(--pm-ink); }
.section-heading h2 { font-size: var(--pm-type-section-title); letter-spacing: -0.02em; }
.section-heading p, .rule-scan-heading p { max-width: 68ch; margin: 7px 0 0; color: var(--pm-muted); font-size: 14px; line-height: 1.6; }
.overview-grid { display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(330px, 0.9fr); border-top: 1px solid var(--pm-stone-strong); border-bottom: 1px solid var(--pm-stone-strong); background: var(--pm-surface); }
.overview-section { min-width: 0; padding: 24px; }
.project-brief { border-right: 1px solid var(--pm-stone); }
.overview-section h3, .rule-scan-section h3 { font-size: 16px; }
.project-brief > p, .latest-audit-summary { margin: 16px 0 0; color: var(--pm-graphite); line-height: 1.75; }
.project-brief dl, .audit-ledger, .scan-ledger { display: grid; margin: 20px 0 0; border-top: 1px solid var(--pm-stone); }
.project-brief dl div, .audit-ledger div, .scan-ledger div { display: flex; min-width: 0; align-items: baseline; justify-content: space-between; gap: 18px; padding: 12px 0; border-bottom: 1px solid var(--pm-stone); }
.project-brief dt, .audit-ledger dt, .scan-ledger dt { color: var(--pm-muted); font-size: 12px; }
.project-brief dd, .audit-ledger dd, .scan-ledger dd { min-width: 0; margin: 0; color: var(--pm-ink); text-align: right; }
.project-brief dd { overflow-wrap: anywhere; font-size: 13px; }
.project-brief a { color: var(--pm-primary-dark); text-decoration-thickness: 1px; text-underline-offset: 3px; }
.audit-ledger { grid-template-columns: repeat(2, minmax(0, 1fr)); border-left: 1px solid var(--pm-stone); }
.audit-ledger div { display: grid; justify-content: stretch; gap: 5px; padding: 14px; border-right: 1px solid var(--pm-stone); }
.audit-ledger dd, .scan-ledger dd { font-family: var(--pm-font-mono); font-size: 20px; font-variant-numeric: tabular-nums; text-align: left; }
.overview-loading, .interview-loading { margin-top: 18px; }
.inline-error { margin-top: 16px; padding: 18px 0; border-top: 1px solid var(--pm-stone); border-bottom: 1px solid var(--pm-stone); }
.rule-scan-section { margin-top: 40px; padding-top: 24px; border-top: 1px solid var(--pm-stone-strong); }
.scan-ledger { grid-template-columns: repeat(4, minmax(0, 1fr)); border-left: 1px solid var(--pm-stone); }
.scan-ledger div { display: grid; justify-content: stretch; padding: 14px; border-right: 1px solid var(--pm-stone); }
.scan-results-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 22px; border-top: 1px solid var(--pm-stone-strong); border-bottom: 1px solid var(--pm-stone-strong); }
.scan-results-grid > section { min-width: 0; padding: 20px; }
.scan-results-grid > section:first-child { border-right: 1px solid var(--pm-stone); }
.scan-results-grid h4 { font-size: 14px; }
.scan-list { margin: 14px 0 0; padding: 0; list-style: none; }
.scan-list li { display: grid; gap: 7px; padding: 13px 0; border-top: 1px solid var(--pm-stone); }
.scan-list strong { color: var(--pm-ink); font-size: 13px; line-height: 1.55; }
.scan-list code { color: var(--pm-muted); font-family: var(--pm-font-mono); font-size: 10px; overflow-wrap: anywhere; }
.scan-list p { margin: 0; color: var(--pm-graphite); font-size: 12px; line-height: 1.65; }
.scan-suggestions { margin-top: 18px; padding-top: 14px; border-top: 1px solid var(--pm-stone); }
.scan-suggestions summary { min-height: 36px; color: var(--pm-primary-dark); cursor: pointer; font-weight: 600; }
.scan-suggestions ul { color: var(--pm-graphite); line-height: 1.7; }
.interview-list { border-top: 1px solid var(--pm-stone-strong); }
.interview-list article { display: grid; min-height: 76px; grid-template-columns: minmax(0, 1fr) auto auto; align-items: center; gap: 24px; padding: 12px 0; border-bottom: 1px solid var(--pm-stone); }
.interview-list article > div { display: flex; min-width: 0; flex-wrap: wrap; align-items: center; gap: 9px 16px; }
.interview-list strong { color: var(--pm-ink); font-size: 14px; }
.interview-list article > div > span:last-child { color: var(--pm-muted); font-family: var(--pm-font-mono); font-size: 10px; }
.interview-score { color: var(--pm-ink); font-family: var(--pm-font-mono); font-size: 20px; font-variant-numeric: tabular-nums; }
@media (max-width: 900px) {
  .overview-grid, .scan-results-grid { grid-template-columns: 1fr; }
  .project-title-row { display: grid; }
  .project-primary-actions { justify-content: flex-start; }
  .audit-action { justify-items: start; }
  .project-brief, .scan-results-grid > section:first-child { border-right: 0; border-bottom: 1px solid var(--pm-stone); }
}
@media (max-width: 700px) {
  .project-title-line, .section-heading, .interview-heading, .rule-scan-heading, .project-header-error { align-items: flex-start; flex-direction: column; }
  .project-navigation { top: 72px; margin-right: -22px; margin-left: -22px; padding-right: 22px; padding-left: 22px; }
  .project-workspace-content { padding-top: 24px; }
  .overview-section { padding: 20px 0; }
  .overview-grid { background: transparent; }
  .audit-ledger, .scan-ledger { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .interview-list article { grid-template-columns: 1fr auto; }
  .interview-list article .el-button { grid-column: 1 / -1; justify-self: start; }
}
@media (max-width: 520px) {
  .project-navigation { top: 60px; margin-right: -16px; margin-left: -16px; padding-right: 16px; padding-left: 16px; }
  .project-title-line h2 { font-size: 28px; }
  .project-primary-actions, .audit-action, .audit-action .el-button { width: 100%; }
  .project-primary-actions .defense-entry { flex: 1 1 100%; }
  .project-primary-actions > .el-dropdown { flex: 0 0 auto; }
}
@media (prefers-reduced-motion: reduce) { .project-navigation button { transition: none; } }
</style>
