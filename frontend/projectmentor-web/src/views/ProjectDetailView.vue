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

          <div class="file-list-section">
            <div class="file-list-header">
              <div>
                <h4>{{ t('projects.savedFiles') }}</h4>
                <p class="muted">{{ filePageSummary(filteredUploadSavedFiles.length, uploadSavedFilePage, uploadSavedFilePageSize) }}</p>
              </div>
              <div class="file-list-filters">
                <el-input
                  v-model="uploadSavedFileKeyword"
                  clearable
                  :placeholder="t('projects.fileSearchPlaceholder')"
                />
                <el-select v-model="uploadSavedFileType" :placeholder="t('projects.fileTypeFilter')">
                  <el-option :label="t('projects.fileTypeAll')" value="" />
                  <el-option v-for="option in fileTypeGroupOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
              </div>
            </div>
            <el-table :data="paginatedUploadSavedFiles" stripe max-height="420">
              <el-table-column prop="filePath" :label="t('projects.filePath')" min-width="260" show-overflow-tooltip />
              <el-table-column :label="t('projects.fileTypeGroup')" width="130">
                <template #default="{ row }">{{ fileTypeGroupLabel(fileTypeGroup(row)) }}</template>
              </el-table-column>
              <el-table-column prop="fileType" :label="t('projects.fileType')" width="150" />
              <el-table-column prop="contentLength" :label="t('projects.contentLength')" width="120" />
            </el-table>
            <div class="file-list-pagination">
              <el-pagination
                v-if="filteredUploadSavedFiles.length > 0"
                v-model:current-page="uploadSavedFilePage"
                v-model:page-size="uploadSavedFilePageSize"
                background
                layout="total, sizes, prev, pager, next"
                :page-sizes="FILE_PAGE_SIZES"
                :total="filteredUploadSavedFiles.length"
              />
            </div>
          </div>

          <div class="file-list-section">
            <div class="file-list-header">
              <div>
                <h4>{{ t('projects.skippedFiles') }}</h4>
                <p class="muted">{{ filePageSummary(filteredUploadSkippedFiles.length, uploadSkippedFilePage, uploadSkippedFilePageSize) }}</p>
              </div>
              <div class="file-list-filters">
                <el-input
                  v-model="uploadSkippedFileKeyword"
                  clearable
                  :placeholder="t('projects.fileSearchPlaceholder')"
                />
                <el-select v-model="uploadSkippedFileType" :placeholder="t('projects.fileTypeFilter')">
                  <el-option :label="t('projects.fileTypeAll')" value="" />
                  <el-option v-for="option in fileTypeGroupOptions" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
              </div>
            </div>
            <el-table :data="paginatedUploadSkippedFiles" stripe max-height="420" :empty-text="t('projects.noMatchedFiles')">
              <el-table-column prop="filePath" :label="t('projects.filePath')" min-width="260" show-overflow-tooltip />
              <el-table-column :label="t('projects.fileTypeGroup')" width="130">
                <template #default="{ row }">{{ fileTypeGroupLabel(fileTypeGroup(row)) }}</template>
              </el-table-column>
              <el-table-column :label="t('projects.skipReasonTitle')" width="170">
                <template #default="{ row }">{{ skipReasonLabel(row.reason) }}</template>
              </el-table-column>
            </el-table>
            <div class="file-list-pagination">
              <el-pagination
                v-if="filteredUploadSkippedFiles.length > 0"
                v-model:current-page="uploadSkippedFilePage"
                v-model:page-size="uploadSkippedFilePageSize"
                background
                layout="total, sizes, prev, pager, next"
                :page-sizes="FILE_PAGE_SIZES"
                :total="filteredUploadSkippedFiles.length"
              />
            </div>
          </div>
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
        <div v-if="files.length || fileLoading" class="file-list-section">
          <div class="file-list-header">
            <div>
              <h4>{{ t('projects.projectFiles') }}</h4>
              <p class="muted">{{ filePageSummary(filteredProjectFiles.length, projectFilePage, projectFilePageSize) }}</p>
            </div>
            <div class="file-list-filters">
              <el-input
                v-model="projectFileKeyword"
                clearable
                :placeholder="t('projects.fileSearchPlaceholder')"
              />
              <el-select v-model="projectFileType" :placeholder="t('projects.fileTypeFilter')">
                <el-option :label="t('projects.fileTypeAll')" value="" />
                <el-option v-for="option in fileTypeGroupOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </div>
          </div>
          <el-table v-loading="fileLoading" :data="paginatedProjectFiles" stripe max-height="460" :empty-text="t('projects.noMatchedFiles')">
            <el-table-column prop="filePath" :label="t('projects.filePath')" min-width="280" show-overflow-tooltip />
            <el-table-column :label="t('projects.fileTypeGroup')" width="130">
              <template #default="{ row }">{{ fileTypeGroupLabel(fileTypeGroup(row)) }}</template>
            </el-table-column>
            <el-table-column prop="fileType" :label="t('projects.fileType')" width="150" />
            <el-table-column prop="contentLength" :label="t('projects.contentLength')" width="120" />
            <el-table-column prop="updateTime" :label="t('common.updateTime')" min-width="180" />
          </el-table>
          <div class="file-list-pagination">
            <el-pagination
              v-if="filteredProjectFiles.length > 0"
              v-model:current-page="projectFilePage"
              v-model:page-size="projectFilePageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="FILE_PAGE_SIZES"
              :total="filteredProjectFiles.length"
            />
          </div>
        </div>
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
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
import type { AnalysisTask, ParsedProjectFile, Project, ProjectFile, RuleScanResult, SkippedProjectFile, UploadZipResult } from '@/types/api'

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
const FILE_PAGE_SIZES = [20, 50, 100]

type FileTypeGroup = '' | 'CODE' | 'CONFIG' | 'DOC' | 'OTHER'
type FileListRow = {
  filePath: string
  fileType?: string
  contentLength?: number
}

const uploadSavedFileKeyword = ref('')
const uploadSavedFileType = ref<FileTypeGroup>('')
const uploadSavedFilePage = ref(1)
const uploadSavedFilePageSize = ref(20)
const uploadSkippedFileKeyword = ref('')
const uploadSkippedFileType = ref<FileTypeGroup>('')
const uploadSkippedFilePage = ref(1)
const uploadSkippedFilePageSize = ref(20)
const projectFileKeyword = ref('')
const projectFileType = ref<FileTypeGroup>('')
const projectFilePage = ref(1)
const projectFilePageSize = ref(20)

const fileTypeGroupOptions = computed<Array<{ label: string; value: FileTypeGroup }>>(() => [
  { label: t('projects.fileTypeGroups.CODE'), value: 'CODE' },
  { label: t('projects.fileTypeGroups.CONFIG'), value: 'CONFIG' },
  { label: t('projects.fileTypeGroups.DOC'), value: 'DOC' },
  { label: t('projects.fileTypeGroups.OTHER'), value: 'OTHER' }
])

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

const filteredUploadSavedFiles = computed(() =>
  filterFileRows(uploadResult.value?.files || [], uploadSavedFileKeyword.value, uploadSavedFileType.value)
)

const paginatedUploadSavedFiles = computed(() =>
  paginateRows(filteredUploadSavedFiles.value, uploadSavedFilePage.value, uploadSavedFilePageSize.value)
)

const uploadSkippedFiles = computed(() => uploadResult.value?.skippedFiles || [])

const filteredUploadSkippedFiles = computed(() =>
  filterSkippedRows(uploadSkippedFiles.value, uploadSkippedFileKeyword.value, uploadSkippedFileType.value)
)

const paginatedUploadSkippedFiles = computed(() =>
  paginateRows(filteredUploadSkippedFiles.value, uploadSkippedFilePage.value, uploadSkippedFilePageSize.value)
)

const filteredProjectFiles = computed(() =>
  filterFileRows(files.value, projectFileKeyword.value, projectFileType.value)
)

const paginatedProjectFiles = computed(() =>
  paginateRows(filteredProjectFiles.value, projectFilePage.value, projectFilePageSize.value)
)

watch([uploadSavedFileKeyword, uploadSavedFileType, uploadSavedFilePageSize], () => {
  uploadSavedFilePage.value = 1
})

watch([uploadSkippedFileKeyword, uploadSkippedFileType, uploadSkippedFilePageSize], () => {
  uploadSkippedFilePage.value = 1
})

watch([projectFileKeyword, projectFileType, projectFilePageSize], () => {
  projectFilePage.value = 1
})

function filterFileRows<T extends FileListRow>(rows: T[], keyword: string, group: FileTypeGroup) {
  const normalizedKeyword = keyword.trim().toLowerCase()

  return rows.filter((row) => {
    const pathMatched = !normalizedKeyword || row.filePath.toLowerCase().includes(normalizedKeyword)
    const typeMatched = !group || fileTypeGroup(row) === group
    return pathMatched && typeMatched
  })
}

function filterSkippedRows(rows: SkippedProjectFile[], keyword: string, group: FileTypeGroup) {
  const normalizedKeyword = keyword.trim().toLowerCase()

  return rows.filter((row) => {
    const pathMatched = !normalizedKeyword || row.filePath.toLowerCase().includes(normalizedKeyword)
    const typeMatched = !group || fileTypeGroup(row) === group
    return pathMatched && typeMatched
  })
}

function paginateRows<T>(rows: T[], page: number, pageSize: number) {
  const start = (Math.max(1, page) - 1) * pageSize
  return rows.slice(start, start + pageSize)
}

function filePageSummary(total: number, page: number, pageSize: number) {
  const pages = Math.max(1, Math.ceil(total / pageSize))
  return t('projects.filePageSummary', {
    page: Math.min(page, pages),
    pages,
    total
  })
}

function fileTypeGroup(row: { filePath: string; fileType?: string }): Exclude<FileTypeGroup, ''> {
  const type = row.fileType?.toUpperCase() || ''
  const path = row.filePath.toLowerCase()

  if (
    path.endsWith('.java') ||
    ['CONTROLLER', 'SERVICE', 'MAPPER', 'ENTITY', 'UTIL'].includes(type)
  ) {
    return 'CODE'
  }

  if (
    ['CONFIG', 'POM', 'PACKAGE', 'DOCKER', 'DOCKER_COMPOSE', 'SQL', 'GITIGNORE'].includes(type) ||
    path.endsWith('.xml') ||
    path.endsWith('.yml') ||
    path.endsWith('.yaml') ||
    path.endsWith('.properties') ||
    path.endsWith('.sql') ||
    path.endsWith('.json') ||
    path.endsWith('dockerfile')
  ) {
    return 'CONFIG'
  }

  if (type === 'README' || path.endsWith('.md')) {
    return 'DOC'
  }

  return 'OTHER'
}

function fileTypeGroupLabel(group: FileTypeGroup) {
  if (!group) {
    return t('projects.fileTypeAll')
  }

  return t(`projects.fileTypeGroups.${group}`)
}

function skipReasonLabel(reason?: string) {
  if (!reason) {
    return '-'
  }

  return skipReasonLabels.value[reason] || reason
}

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

.file-list-section {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #fbfdff;
}

.file-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.file-list-header h4 {
  margin: 0 0 6px;
  color: var(--pm-ink);
}

.file-list-header p {
  margin: 0;
  font-size: 13px;
}

.file-list-filters {
  display: grid;
  grid-template-columns: minmax(220px, 320px) 150px;
  gap: 10px;
  min-width: min(100%, 480px);
}

.file-list-pagination {
  display: flex;
  justify-content: flex-end;
}

.file-list-section :deep(.el-table__cell) {
  padding: 8px 0;
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

  .file-list-header,
  .zip-upload-actions {
    align-items: flex-start;
    max-width: none;
  }

  .file-list-header {
    flex-direction: column;
  }

  .file-list-filters {
    width: 100%;
    grid-template-columns: 1fr;
    min-width: 0;
  }

  .upload-tip {
    text-align: left;
  }
}
</style>
