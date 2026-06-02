<template>
  <div class="page-stack">
    <section class="panel" v-loading="loading">
      <div class="panel-title">
        <div>
          <h2>{{ project?.name || t('projects.detailTitle') }}</h2>
          <p class="muted">{{ project?.description || t('projects.noDescription') }}</p>
        </div>
        <el-tag :type="statusTagType(project?.status)" effect="light">{{ project?.status || 'PENDING' }}</el-tag>
      </div>
      <div class="panel-body detail-grid">
        <div>
          <span class="muted">{{ t('common.projectName') }}</span>
          <p>{{ project?.name || '-' }}</p>
        </div>
        <div>
          <span class="muted">{{ t('common.github') }}</span>
          <p class="link-text">{{ project?.githubUrl || '-' }}</p>
        </div>
        <div>
          <span class="muted">{{ t('common.description') }}</span>
          <p>{{ project?.description || '-' }}</p>
        </div>
        <div>
          <span class="muted">{{ t('common.techStack') }}</span>
          <p>{{ project?.techStack || '-' }}</p>
        </div>
        <div>
          <span class="muted">{{ t('common.projectType') }}</span>
          <p>{{ project?.projectType || '-' }}</p>
        </div>
        <div>
          <span class="muted">{{ t('common.createTime') }}</span>
          <p>{{ project?.createTime || '-' }}</p>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('projects.readmeTitle') }}</h3>
          <p class="muted">{{ t('projects.readmeDesc') }}</p>
        </div>
        <el-button type="primary" :loading="savingReadme" @click="handleSaveReadme">{{ t('projects.saveReadme') }}</el-button>
      </div>
      <div class="panel-body readme-editor">
        <el-input
          v-model="readmeContent"
          type="textarea"
          :rows="10"
          :placeholder="t('projects.readmePlaceholder')"
        />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('projects.zipTitle') }}</h3>
          <p class="muted">{{ t('projects.zipDesc') }}</p>
        </div>
        <div class="zip-upload-actions">
          <el-upload accept=".zip" :show-file-list="false" :before-upload="beforeZipUpload">
            <el-button :icon="Upload" :loading="uploading">{{ t('projects.uploadZip') }}</el-button>
          </el-upload>
          <p class="muted upload-tip">{{ t('projects.zipTip') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <div v-if="uploadResult" class="upload-result">
          <div class="score-grid">
            <div class="metric-card">
              <span>{{ t('projects.savedFiles') }}</span>
              <strong>{{ uploadResult.savedFileCount }}</strong>
            </div>
            <div class="metric-card">
              <span>{{ t('projects.skippedFiles') }}</span>
              <strong>{{ uploadResult.skippedFileCount }}</strong>
            </div>
          </div>

          <div v-if="skipReasonEntries.length" class="skip-reason-list">
            <el-tag
              v-for="item in skipReasonEntries"
              :key="item.reason"
              effect="light"
              type="info"
            >
              {{ item.label }}：{{ item.count }}
            </el-tag>
          </div>

          <div v-if="uploadResult.warnings?.length" class="warning-list">
            <el-alert
              v-for="warning in uploadResult.warnings"
              :key="warning"
              :title="warning"
              type="warning"
              show-icon
              :closable="false"
            />
          </div>

          <el-table :data="uploadResult.files" stripe>
            <el-table-column prop="filePath" :label="t('projects.filePath')" min-width="260" show-overflow-tooltip />
            <el-table-column prop="fileType" :label="t('projects.fileType')" width="150" />
            <el-table-column prop="contentLength" :label="t('projects.contentLength')" width="120" />
          </el-table>
        </div>
        <EmptyState v-else :title="t('projects.uploadEmptyTitle')" :description="t('projects.uploadEmptyDesc')" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('projects.projectFiles') }}</h3>
          <p class="muted">{{ t('projects.projectFilesDesc') }}</p>
        </div>
        <el-button :icon="Refresh" :loading="fileLoading" @click="loadFiles">{{ t('common.refresh') }}</el-button>
      </div>
      <div class="panel-body">
        <el-table v-if="files.length || fileLoading" v-loading="fileLoading" :data="files" stripe>
          <el-table-column prop="filePath" :label="t('projects.filePath')" min-width="280" show-overflow-tooltip />
          <el-table-column prop="fileType" :label="t('projects.fileType')" width="150" />
          <el-table-column prop="contentLength" :label="t('projects.contentLength')" width="120" />
          <el-table-column prop="updateTime" :label="t('common.updateTime')" min-width="180" />
        </el-table>
        <EmptyState v-else :title="t('projects.noFilesTitle')" :description="t('projects.noFilesDesc')" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('projects.ruleScanTitle') }}</h3>
          <p class="muted">{{ t('projects.ruleScanDesc') }}</p>
        </div>
        <el-button :icon="Search" :loading="scanning" @click="handleScan">{{ t('projects.ruleScan') }}</el-button>
      </div>
      <div class="panel-body">
        <div v-if="scanResult" class="scan-result">
          <div class="score-grid">
            <div class="metric-card">
              <span>{{ t('projects.riskTotal') }}</span>
              <strong>{{ scanResult.totalRiskCount }}</strong>
            </div>
            <div class="metric-card">
              <span>{{ t('projects.highRisk') }}</span>
              <strong>{{ scanResult.highRiskCount }}</strong>
            </div>
            <div class="metric-card">
              <span>{{ t('projects.mediumRisk') }}</span>
              <strong>{{ scanResult.mediumRiskCount }}</strong>
            </div>
            <div class="metric-card">
              <span>{{ t('projects.lowRisk') }}</span>
              <strong>{{ scanResult.lowRiskCount }}</strong>
            </div>
          </div>

          <div class="subsection">
            <h4>{{ t('common.risks') }}</h4>
            <RiskList :risks="scanResult.risks" />
          </div>

          <div class="subsection">
            <h4>{{ t('common.evidence') }}</h4>
            <EvidenceList :evidences="scanResult.evidences" />
          </div>

          <div class="subsection">
            <h4>{{ t('common.suggestions') }}</h4>
            <el-tag v-for="suggestion in scanResult.suggestions" :key="suggestion" class="suggestion-tag" effect="light">
              {{ suggestion }}
            </el-tag>
          </div>
        </div>
        <EmptyState v-else :title="t('projects.notScannedTitle')" :description="t('projects.notScannedDesc')" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('projects.asyncTitle') }}</h3>
          <p class="muted">{{ t('projects.asyncDesc') }}</p>
        </div>
        <el-button type="primary" :icon="Cpu" :loading="analyzing" @click="handleStartAnalyze">{{ t('projects.startAnalyze') }}</el-button>
      </div>
      <div class="panel-body async-panel">
        <TaskProgress v-if="task" :task="task" />
        <EmptyState v-else :title="t('projects.noTaskTitle')" :description="t('projects.noTaskDesc')" />
        <div v-if="task?.status === 'SUCCESS' && task.reportId" class="toolbar">
          <el-button type="primary" @click="router.push(`/reports/${task.reportId}`)">{{ t('projects.viewReport') }}</el-button>
        </div>
      </div>
    </section>

    <ProjectQaPanel :project-id="projectId" :has-project-files="files.length > 0" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Cpu, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, type UploadProps } from 'element-plus'

import { getTask, scanProject, startAnalyze } from '@/api/analysis'
import { getProjectDetail, listProjectFiles, saveReadme, uploadZip } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import EvidenceList from '@/components/EvidenceList.vue'
import ProjectQaPanel from '@/components/ProjectQaPanel.vue'
import RiskList from '@/components/RiskList.vue'
import TaskProgress from '@/components/TaskProgress.vue'
import type { AnalysisTask, Project, ProjectFile, RuleScanResult, UploadZipResult } from '@/types/api'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const projectId = Number(route.params.id)

const loading = ref(false)
const fileLoading = ref(false)
const savingReadme = ref(false)
const uploading = ref(false)
const scanning = ref(false)
const analyzing = ref(false)
const readmeContent = ref('')
const project = ref<Project>()
const files = ref<ProjectFile[]>([])
const uploadResult = ref<UploadZipResult>()
const scanResult = ref<RuleScanResult>()
const task = ref<AnalysisTask>()
let pollTimer: number | undefined

const MAX_ZIP_SIZE_BYTES = 200 * 1024 * 1024

const skipReasonLabels = computed<Record<string, string>>(() => ({
  FILTERED_DIRECTORY: t('projects.skipReason.FILTERED_DIRECTORY'),
  NOT_WHITELIST: t('projects.skipReason.NOT_WHITELIST'),
  BINARY_FILE: t('projects.skipReason.BINARY_FILE'),
  FILE_TOO_LARGE: t('projects.skipReason.FILE_TOO_LARGE'),
  VALID_FILE_LIMIT: t('projects.skipReason.VALID_FILE_LIMIT'),
  TOTAL_TEXT_LIMIT: t('projects.skipReason.TOTAL_TEXT_LIMIT'),
  DANGEROUS_PATH: t('projects.skipReason.DANGEROUS_PATH'),
  ENTRY_LIMIT: t('projects.skipReason.ENTRY_LIMIT')
}))

const skipReasonEntries = computed(() => {
  const skippedByReason = uploadResult.value?.skippedByReason || {}

  return Object.entries(skippedByReason)
    .filter(([, count]) => count > 0)
    .map(([reason, count]) => ({
      reason,
      label: skipReasonLabels.value[reason] || reason,
      count
    }))
})

function statusTagType(status?: string) {
  const statusMap: Record<string, 'info' | 'primary' | 'success' | 'danger' | 'warning'> = {
    PENDING: 'info',
    ANALYZING: 'primary',
    FINISHED: 'success',
    FAILED: 'danger'
  }

  return statusMap[status || 'PENDING'] || 'info'
}

async function loadProject() {
  loading.value = true
  try {
    project.value = await getProjectDetail(projectId)
  } finally {
    loading.value = false
  }
}

async function loadFiles() {
  fileLoading.value = true
  try {
    files.value = await listProjectFiles(projectId)
    const readme = files.value.find((file) => /readme/i.test(file.filePath))
    if (readme?.content && !readmeContent.value.trim()) {
      readmeContent.value = readme.content
    }
  } finally {
    fileLoading.value = false
  }
}

async function handleSaveReadme() {
  if (!readmeContent.value.trim()) {
    ElMessage.warning(t('projects.readmeRequired'))
    return
  }

  savingReadme.value = true
  try {
    await saveReadme(projectId, readmeContent.value)
    ElMessage.success(t('projects.readmeSaved'))
    loadFiles()
  } finally {
    savingReadme.value = false
  }
}

const beforeZipUpload: UploadProps['beforeUpload'] = async (rawFile) => {
  if (!rawFile.name.toLowerCase().endsWith('.zip')) {
    ElMessage.warning(t('projects.onlyZip'))
    return false
  }

  if (rawFile.size > MAX_ZIP_SIZE_BYTES) {
    ElMessage.error(t('projects.zipTooLarge'))
    return false
  }

  uploading.value = true
  try {
    uploadResult.value = await uploadZip(projectId, rawFile)
    ElMessage.success(t('projects.savedFileMessage', { count: uploadResult.value.savedFileCount }))
    loadFiles()
  } finally {
    uploading.value = false
  }

  return false
}

async function handleScan() {
  scanning.value = true
  try {
    scanResult.value = await scanProject(projectId)
    ElMessage.success(t('projects.scanDone'))
  } finally {
    scanning.value = false
  }
}

async function handleStartAnalyze() {
  analyzing.value = true
  try {
    task.value = await startAnalyze(projectId)
    ElMessage.success(t('projects.taskStarted'))
    startPolling(task.value.taskId)
  } finally {
    analyzing.value = false
  }
}

function startPolling(taskId: number) {
  clearPolling()
  pollTimer = window.setInterval(async () => {
    try {
      const latestTask = await getTask(taskId)
      task.value = latestTask

      if (latestTask.status === 'SUCCESS') {
        clearPolling()
        ElMessage.success(t('projects.reportDone'))
      }

      if (latestTask.status === 'FAILED') {
        clearPolling()
        ElMessage.error(latestTask.failReason || '分析任务失败')
      }
    } catch {
      clearPolling()
    }
  }, 1500)
}

function clearPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

onMounted(() => {
  loadProject()
  loadFiles()
})

onUnmounted(clearPolling)
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.detail-grid p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.link-text {
  overflow-wrap: anywhere;
  color: var(--pm-primary);
}

.readme-editor,
.upload-result,
.scan-result,
.async-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.zip-upload-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  max-width: 360px;
}

.upload-tip {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  text-align: right;
}

.warning-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skip-reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skip-reason-list .el-tag {
  height: auto;
  padding: 6px 10px;
  line-height: 1.5;
  white-space: normal;
}

.subsection h4 {
  margin: 0 0 12px;
}

.suggestion-tag {
  margin: 0 8px 8px 0;
  max-width: 100%;
  white-space: normal;
  height: auto;
  padding: 6px 10px;
  line-height: 1.5;
}

@media (max-width: 860px) {
  .detail-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 620px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .zip-upload-actions {
    align-items: flex-start;
    max-width: none;
  }

  .upload-tip {
    text-align: left;
  }
}
</style>
