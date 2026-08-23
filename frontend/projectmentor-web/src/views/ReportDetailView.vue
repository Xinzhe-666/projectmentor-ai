<template>
  <div class="report-detail-page">
    <nav class="report-page-toolbar no-print" :aria-label="t('common.operation')">
      <el-button :icon="ArrowLeft" @click="goToProject">
        {{ t('reportV5.actions.backToProject') }}
      </el-button>
      <div class="report-toolbar-actions">
        <el-popover placement="bottom-end" :width="380" trigger="click">
          <template #reference>
            <el-button :icon="Link" :disabled="!report">
              {{ t('reportV5.actions.share') }}
            </el-button>
          </template>

          <section class="share-control" aria-labelledby="share-control-title">
            <header>
              <div>
                <h2 id="share-control-title">{{ t('reportV5.actions.share') }}</h2>
                <p>{{ t('reportV5.actions.shareHelp') }}</p>
              </div>
              <StatusLabel
                :status="shareInfo?.enabled ? 'AVAILABLE' : 'UNAVAILABLE'"
                :label="shareInfo?.enabled ? t('reportV5.actions.shareActive') : t('reportV5.actions.shareInactive')"
              />
            </header>

            <template v-if="shareInfo?.enabled && fullShareUrl">
              <el-input :model-value="fullShareUrl" readonly :aria-label="t('reportV5.actions.share')" />
              <div class="share-actions">
                <el-button :icon="CopyDocument" @click="handleCopyShare">
                  {{ t('reportV5.actions.copyShare') }}
                </el-button>
                <el-button :loading="shareLoading" @click="handleRotateShare">
                  {{ t('reportV5.actions.refreshShare') }}
                </el-button>
                <el-button :loading="shareLoading" @click="handleDisableShare">
                  {{ t('reportV5.actions.revokeShare') }}
                </el-button>
              </div>
            </template>
            <el-button v-else type="primary" :loading="shareLoading" @click="handleCreateShare">
              {{ t('reportV5.actions.createShare') }}
            </el-button>
          </section>
        </el-popover>

        <el-button type="primary" :icon="Printer" :disabled="!report" @click="handlePrint">
          {{ t('reportV5.actions.print') }}
        </el-button>
      </div>
    </nav>

    <section v-if="loading" class="report-loading" aria-live="polite" :aria-label="t('reportV5.states.loading')">
      <el-skeleton :rows="14" animated />
    </section>

    <section v-else-if="loadError" class="report-load-error" role="alert">
      <div>
        <h2>{{ t('reportV5.states.errorTitle') }}</h2>
        <p>{{ t('reportV5.states.errorDescription') }}</p>
      </div>
      <div>
        <el-button type="primary" @click="loadReport">{{ t('reportV5.states.retry') }}</el-button>
        <el-button @click="goToProject">{{ t('reportV5.actions.backToProject') }}</el-button>
      </div>
    </section>

    <AuditReportDocument
      v-else-if="report"
      :report="report"
      :project-name="reportProjectName"
      :report-id="report.id"
      :project-id="report.projectId"
      :tech-stack="reportTechStack"
      private-actions
      :ai-loading="claimAiLoading"
      :ai-credit-cost="AI_CREDIT_COSTS.CLAIM_EVIDENCE"
      @empty-action="goToProject"
      @ai-enhance="handleClaimAiEnhance"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CopyDocument, Link, Printer } from '@element-plus/icons-vue'

import { enhanceClaimEvidence, getReportDetail } from '@/api/analysis'
import { getMyCredits } from '@/api/credit'
import { getProjectDetail } from '@/api/project'
import { createReportShare, disableReportShare, getReportShare } from '@/api/share'
import AuditReportDocument from '@/components/AuditReportDocument.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import { AI_CREDIT_COSTS } from '@/constants/creditCosts'
import { useUserStore } from '@/stores/user'
import type { AnalysisReport, Project, ReportShare } from '@/types/api'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const reportId = Number(route.params.id)
const validReportId = Number.isInteger(reportId) && reportId > 0
const loading = ref(true)
const loadError = ref(false)
const shareLoading = ref(false)
const claimAiLoading = ref(false)
const report = ref<AnalysisReport>()
const project = ref<Project>()
const shareInfo = ref<ReportShare>()
const userStore = useUserStore()

const reportProjectName = computed(() => {
  if (project.value?.name) return project.value.name
  if (report.value?.projectId) return `${t('common.projectId')} #${report.value.projectId}`
  return t('common.untitledReport')
})

const reportTechStack = computed(() => project.value?.techStack || '')

const fullShareUrl = computed(() => {
  const token = shareInfo.value?.shareToken
  return shareInfo.value?.enabled && token
    ? `${window.location.origin}/share/reports/${token}`
    : ''
})

async function loadReport() {
  loading.value = true
  loadError.value = false
  report.value = undefined
  project.value = undefined

  if (!validReportId) {
    loadError.value = true
    loading.value = false
    return
  }

  try {
    report.value = await getReportDetail(reportId, true)
    if (report.value.projectId) {
      try {
        project.value = await getProjectDetail(report.value.projectId)
      } catch {
        project.value = undefined
      }
    }
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

async function loadShareInfo() {
  if (!validReportId) {
    shareInfo.value = undefined
    return
  }

  shareLoading.value = true
  try {
    shareInfo.value = await getReportShare(reportId)
  } catch {
    shareInfo.value = { reportId, enabled: false }
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
      t('credits.confirmAiUse', { count: AI_CREDIT_COSTS.CLAIM_EVIDENCE }),
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
    // Header balance refreshes during the next normal credit request.
  }
}

async function handleCreateShare() {
  shareLoading.value = true
  try {
    shareInfo.value = await createReportShare(reportId)
    ElMessage.success(t('report.shareCreated'))
  } finally {
    shareLoading.value = false
  }
}

async function handleRotateShare() {
  try {
    await ElMessageBox.confirm(
      t('reportV5.actions.rotateWarning'),
      t('reportV5.actions.refreshShare'),
      {
        confirmButtonText: t('reportV5.actions.confirmRotate'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }

  shareLoading.value = true
  try {
    shareInfo.value = await createReportShare(reportId)
    ElMessage.success(t('report.shareRefreshed'))
  } finally {
    shareLoading.value = false
  }
}

async function handleCopyShare() {
  if (!fullShareUrl.value) return
  const copied = await copyText(fullShareUrl.value)
  ElMessage[copied ? 'success' : 'error'](
    copied ? t('report.shareCopied') : t('report.shareCopyFailed')
  )
}

async function handleDisableShare() {
  try {
    await ElMessageBox.confirm(
      t('reportV5.actions.revokeWarning'),
      t('reportV5.actions.revokeShare'),
      {
        confirmButtonText: t('reportV5.actions.confirmRevoke'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }

  shareLoading.value = true
  try {
    await disableReportShare(reportId)
    shareInfo.value = { reportId, enabled: false }
    ElMessage.success(t('report.shareDisabled'))
  } finally {
    shareLoading.value = false
  }
}

function handlePrint() {
  ElMessage.info(t('report.printTip'))
  window.setTimeout(() => window.print(), 100)
}

function goToProject() {
  const projectId = report.value?.projectId
  router.push(projectId ? `/projects/${projectId}` : '/projects')
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

onMounted(() => {
  if (!validReportId) {
    loadError.value = true
    loading.value = false
    return
  }

  loadReport()
  loadShareInfo()
})
</script>

<style scoped>
.report-detail-page {
  display: grid;
  min-width: 0;
  gap: var(--pm-space-5);
}

.report-page-toolbar {
  position: sticky;
  top: 12px;
  z-index: 8;
  display: flex;
  width: 100%;
  min-width: 0;
  max-width: 1000px;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0 auto;
  padding: 7px 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-paper);
}

.report-toolbar-actions,
.share-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.report-page-toolbar :deep(.el-button),
.share-actions :deep(.el-button) {
  min-height: 44px;
}

.report-loading,
.report-load-error {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 48px 56px;
  border: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.report-loading {
  min-height: 620px;
}

.report-load-error {
  display: flex;
  min-height: 280px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 28px;
}

.report-load-error h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 24px;
}

.report-load-error p {
  max-width: 58ch;
  margin: 8px 0 0;
  color: var(--pm-muted);
  line-height: 1.65;
}

.report-load-error > div:last-child {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.share-control {
  display: grid;
  gap: 16px;
  padding: 4px;
}

.share-control header {
  display: grid;
  gap: 12px;
}

.share-control h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 17px;
}

.share-control p {
  margin: 5px 0 0;
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.55;
}

@media (max-width: 620px) {
  .report-page-toolbar {
    top: 0;
    align-items: stretch;
    gap: 6px;
    padding: 8px 0;
  }

  .report-page-toolbar > .el-button {
    min-width: 44px;
  }

  .report-toolbar-actions {
    gap: 6px;
    flex-wrap: nowrap;
  }

  .report-toolbar-actions :deep(.el-button) {
    min-width: 44px;
    padding-right: 7px;
    padding-left: 7px;
    font-size: 12px;
  }

  .report-loading,
  .report-load-error {
    padding: 32px 20px;
  }

  .report-load-error {
    flex-direction: column;
  }
}
</style>
