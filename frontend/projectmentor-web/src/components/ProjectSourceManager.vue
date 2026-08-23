<template>
  <section class="source-manager" aria-labelledby="project-sources-title">
    <header class="source-manager-header">
      <div>
        <h2 id="project-sources-title">{{ t('projects.v5.sources.title') }}</h2>
        <p>{{ t('projects.v5.sources.description') }}</p>
      </div>
      <div class="source-actions">
        <el-upload accept=".zip" :show-file-list="false" :before-upload="beforeZipUpload">
          <el-button :icon="Upload" :loading="uploading">{{ t('projects.v5.sources.upload') }}</el-button>
        </el-upload>
        <el-button type="primary" :loading="savingReadme" :disabled="!readmeDirty" @click="handleSaveReadme">
          {{ t('projects.saveReadme') }}
        </el-button>
      </div>
    </header>

    <div class="source-management-grid">
      <section class="readme-source" aria-labelledby="readme-source-title">
        <div class="source-section-heading">
          <div>
            <h3 id="readme-source-title">{{ t('projects.v5.sources.readme') }}</h3>
            <p>{{ t('projects.v5.sources.readmeHelp') }}</p>
          </div>
          <StatusLabel
            :status="readmeDirty ? 'PENDING' : 'SUCCESS'"
            :label="readmeDirty ? t('projects.v5.sources.unsaved') : t('projects.v5.sources.saved')"
          />
        </div>
        <label class="field-label" for="project-readme">{{ t('projects.v5.sources.readmeLabel') }}</label>
        <el-input
          id="project-readme"
          v-model="readmeContent"
          type="textarea"
          :rows="16"
          :placeholder="t('projects.readmePlaceholder')"
          @input="readmeDirty = true"
        />
      </section>

      <section class="upload-source" aria-labelledby="upload-source-title">
        <div class="source-section-heading">
          <div>
            <h3 id="upload-source-title">{{ t('projects.v5.sources.upload') }}</h3>
            <p>{{ t('projects.v5.sources.uploadHelp') }}</p>
          </div>
        </div>

        <div class="upload-ledger">
          <div>
            <span>{{ t('projects.savedFiles') }}</span>
            <strong>{{ uploadResult?.savedFileCount ?? '—' }}</strong>
          </div>
          <div>
            <span>{{ t('projects.skippedFiles') }}</span>
            <strong>{{ uploadResult?.skippedFileCount ?? '—' }}</strong>
          </div>
        </div>

        <template v-if="uploadResult">
          <dl v-if="skipReasonEntries.length" class="skip-reason-ledger">
            <div v-for="item in skipReasonEntries" :key="item.reason">
              <dt>{{ item.label }}</dt>
              <dd>{{ item.count }}</dd>
            </div>
          </dl>

          <section v-if="uploadResult.warnings?.length" class="upload-warnings" role="status">
            <h4>{{ t('projects.v5.sources.warnings') }}</h4>
            <ul>
              <li v-for="warning in uploadResult.warnings" :key="warning">{{ warning }}</li>
            </ul>
          </section>

          <details v-if="uploadResult.skippedFiles?.length" class="skipped-details">
            <summary>{{ t('projects.v5.sources.skippedDetails') }}</summary>
            <ul>
              <li v-for="file in uploadResult.skippedFiles" :key="`${file.filePath}-${file.reason}`">
                <code>{{ file.filePath }}</code>
                <span>{{ skipReasonLabel(file.reason) }}</span>
              </li>
            </ul>
          </details>
        </template>
      </section>
    </div>

    <section class="file-inventory" aria-labelledby="file-inventory-title">
      <div class="inventory-header">
        <div>
          <h3 id="file-inventory-title">{{ t('projects.v5.sources.inventory') }}</h3>
          <p>{{ t('projects.v5.sources.inventoryDescription') }}</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="emit('refresh')">{{ t('common.refresh') }}</el-button>
      </div>

      <div class="inventory-controls">
        <el-input
          v-model="fileKeyword"
          clearable
          :aria-label="t('projects.fileSearchPlaceholder')"
          :placeholder="t('projects.fileSearchPlaceholder')"
        />
        <el-select v-model="fileType" :aria-label="t('projects.fileTypeFilter')">
          <el-option :label="t('projects.fileTypeAll')" value="" />
          <el-option
            v-for="option in fileTypeGroupOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>

      <div v-if="error" class="inventory-error" role="alert">
        <span>{{ t('projects.v5.errors.files') }}</span>
        <el-button text @click="emit('refresh')">{{ t('projects.v5.retry') }}</el-button>
      </div>

      <div v-else-if="loading" class="inventory-loading" aria-live="polite">
        <el-skeleton :rows="7" animated />
      </div>

      <template v-else-if="filteredFiles.length">
        <div class="inventory-list" role="table" :aria-label="t('projects.v5.sources.inventory')">
          <div class="inventory-row inventory-row-head" role="row">
            <span role="columnheader">{{ t('projects.filePath') }}</span>
            <span role="columnheader">{{ t('projects.v5.sources.fileType') }}</span>
            <span role="columnheader">{{ t('projects.v5.sources.fileSize') }}</span>
            <span role="columnheader">{{ t('projects.v5.sources.modified') }}</span>
          </div>
          <div v-for="file in paginatedFiles" :key="file.id || file.filePath" class="inventory-row" role="row">
            <span class="inventory-path" role="cell">
              <strong>{{ fileName(file.filePath) }}</strong>
              <code>{{ file.filePath }}</code>
            </span>
            <span class="inventory-role" role="cell">{{ fileTypeGroupLabel(fileTypeGroup(file)) }}</span>
            <span class="inventory-number" role="cell">{{ file.contentLength ?? '—' }}</span>
            <span class="inventory-time" role="cell">{{ formatDate(file.updateTime) }}</span>
          </div>
        </div>
        <div class="inventory-pagination">
          <span>{{ filePageSummary }}</span>
          <el-pagination
            v-model:current-page="filePage"
            v-model:page-size="filePageSize"
            :page-sizes="FILE_PAGE_SIZES"
            :total="filteredFiles.length"
            layout="sizes, prev, pager, next"
            size="small"
          />
        </div>
      </template>

      <EmptyState
        v-else
        variant="compact"
        :title="props.files.length ? t('projects.v5.sources.noMatches') : t('projects.v5.evidence.noSources')"
        :description="props.files.length ? t('projects.v5.sources.noMatchesDescription') : t('projects.v5.evidence.noSourcesDescription')"
      />
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type UploadProps } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'

import { saveReadme, uploadZip } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type { ProjectFile, UploadZipResult } from '@/types/api'

const props = withDefaults(defineProps<{
  projectId: number
  files?: ProjectFile[]
  loading?: boolean
  error?: boolean
}>(), {
  files: () => [],
  loading: false,
  error: false
})

const emit = defineEmits<{
  (event: 'refresh'): void
}>()

const { t } = useI18n()
const readmeContent = ref('')
const readmeDirty = ref(false)
const initializedReadmeId = ref<number>()
const savingReadme = ref(false)
const uploading = ref(false)
const uploadResult = ref<UploadZipResult>()
const fileKeyword = ref('')
const fileType = ref<FileTypeGroup>('')
const filePage = ref(1)
const filePageSize = ref(20)

const MAX_ZIP_SIZE_BYTES = 800 * 1024 * 1024
const FILE_PAGE_SIZES = [20, 50, 100]
type FileTypeGroup = '' | 'CODE' | 'CONFIG' | 'DOC' | 'OTHER'

const readmeFile = computed(() => props.files.find((file) => /(^|[\\/])readme/i.test(file.filePath)))

watch(readmeFile, (file) => {
  if (!readmeDirty.value && file && initializedReadmeId.value !== file.id) {
    readmeContent.value = file.content || ''
    initializedReadmeId.value = file.id
  }
}, { immediate: true })

watch([fileKeyword, fileType, filePageSize], () => {
  filePage.value = 1
})

const fileTypeGroupOptions = computed(() => [
  { label: t('projects.fileTypeGroups.CODE'), value: 'CODE' as const },
  { label: t('projects.fileTypeGroups.CONFIG'), value: 'CONFIG' as const },
  { label: t('projects.fileTypeGroups.DOC'), value: 'DOC' as const },
  { label: t('projects.fileTypeGroups.OTHER'), value: 'OTHER' as const }
])

const skipReasonLabels = computed<Record<string, string>>(() => ({
  ignored_directory: t('projects.skipReason.ignored_directory'),
  unsupported_type: t('projects.skipReason.unsupported_type'),
  file_too_large: t('projects.skipReason.file_too_large'),
  unsafe_path: t('projects.skipReason.unsafe_path'),
  max_file_count_exceeded: t('projects.skipReason.max_file_count_exceeded'),
  max_total_size_exceeded: t('projects.skipReason.max_total_size_exceeded'),
  empty_file: t('projects.skipReason.empty_file'),
  binary_file: t('projects.skipReason.binary_file')
}))

const skipReasonEntries = computed(() => Object.entries(uploadResult.value?.skippedByReason || {})
  .filter(([, count]) => count > 0)
  .map(([reason, count]) => ({ reason, label: skipReasonLabel(reason), count })))

const filteredFiles = computed(() => {
  const keyword = fileKeyword.value.trim().toLowerCase()
  return props.files.filter((file) => {
    const keywordMatched = !keyword || file.filePath.toLowerCase().includes(keyword)
    const typeMatched = !fileType.value || fileTypeGroup(file) === fileType.value
    return keywordMatched && typeMatched
  })
})

const paginatedFiles = computed(() => {
  const start = (filePage.value - 1) * filePageSize.value
  return filteredFiles.value.slice(start, start + filePageSize.value)
})

const filePageSummary = computed(() => {
  const pages = Math.max(1, Math.ceil(filteredFiles.value.length / filePageSize.value))
  return t('projects.filePageSummary', {
    page: Math.min(filePage.value, pages),
    pages,
    total: filteredFiles.value.length
  })
})

async function handleSaveReadme() {
  if (!readmeContent.value.trim()) {
    ElMessage.warning(t('projects.readmeRequired'))
    return
  }

  savingReadme.value = true
  try {
    const savedFile = await saveReadme(props.projectId, readmeContent.value)
    initializedReadmeId.value = savedFile.id
    readmeDirty.value = false
    ElMessage.success(t('projects.readmeSaved'))
    emit('refresh')
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
    uploadResult.value = await uploadZip(props.projectId, rawFile)
    ElMessage.success(t('projects.savedFileMessage', { count: uploadResult.value.savedFileCount }))
    emit('refresh')
  } finally {
    uploading.value = false
  }
  return false
}

function skipReasonLabel(reason?: string) {
  return reason ? (skipReasonLabels.value[reason] || reason) : '—'
}

function fileTypeGroup(file: Pick<ProjectFile, 'filePath' | 'fileType'>): Exclude<FileTypeGroup, ''> {
  const type = file.fileType?.toUpperCase() || ''
  const path = file.filePath.toLowerCase()

  if (type === 'README' || path.endsWith('.md')) {
    return 'DOC'
  }
  if (
    ['CONFIG', 'POM', 'PACKAGE', 'DOCKER', 'DOCKER_COMPOSE', 'SQL', 'GITIGNORE'].includes(type)
    || ['.xml', '.yml', '.yaml', '.properties', '.sql', '.json'].some((extension) => path.endsWith(extension))
    || path.endsWith('dockerfile')
  ) {
    return 'CONFIG'
  }
  if (
    ['CODE', 'CONTROLLER', 'SERVICE', 'MAPPER', 'ENTITY', 'UTIL'].includes(type)
    || ['.java', '.kt', '.js', '.jsx', '.ts', '.tsx', '.vue', '.css', '.scss', '.html', '.py', '.go', '.rs', '.c', '.cpp', '.cs', '.sh'].some((extension) => path.endsWith(extension))
  ) {
    return 'CODE'
  }
  return 'OTHER'
}

function fileTypeGroupLabel(group: FileTypeGroup) {
  return group ? t(`projects.fileTypeGroups.${group}`) : t('projects.fileTypeAll')
}

function fileName(path: string) {
  return path.split(/[\\/]/).filter(Boolean).pop() || path
}

function formatDate(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped>
.source-manager {
  min-width: 0;
}

.source-manager-header,
.source-section-heading,
.inventory-header,
.inventory-pagination {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.source-manager-header {
  align-items: flex-end;
  margin-bottom: 20px;
}

.source-actions :deep(.el-button),
.inventory-header :deep(.el-button) {
  min-height: 44px;
}

.inventory-controls :deep(.el-input__wrapper),
.inventory-controls :deep(.el-select__wrapper) {
  min-height: 44px;
}

.source-manager h2,
.source-manager h3,
.source-manager h4 {
  margin: 0;
  color: var(--pm-ink);
}

.source-manager h2 {
  font-size: 24px;
  letter-spacing: -0.02em;
}

.source-manager h3 {
  font-size: 16px;
  letter-spacing: -0.012em;
}

.source-manager-header p,
.source-section-heading p,
.inventory-header p {
  max-width: 68ch;
  margin: 6px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.source-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.source-management-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.readme-source,
.upload-source {
  min-width: 0;
  padding: 24px;
}

.readme-source {
  border-right: 1px solid var(--pm-stone);
}

.field-label {
  display: block;
  margin: 20px 0 8px;
  color: var(--pm-graphite);
  font-size: 13px;
  font-weight: 600;
}

.readme-source :deep(.el-textarea__inner) {
  min-height: 360px;
  resize: vertical;
  font-family: var(--pm-font-mono);
  font-size: 12px;
  line-height: 1.7;
}

.upload-ledger,
.skip-reason-ledger {
  display: grid;
  margin: 20px 0 0;
  border-top: 1px solid var(--pm-stone);
}

.upload-ledger div,
.skip-reason-ledger div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 0;
  border-bottom: 1px solid var(--pm-stone);
}

.upload-ledger span,
.skip-reason-ledger dt {
  color: var(--pm-muted);
  font-size: 12px;
}

.upload-ledger strong,
.skip-reason-ledger dd {
  margin: 0;
  color: var(--pm-ink);
  font-family: var(--pm-font-mono);
  font-size: 16px;
  font-variant-numeric: tabular-nums;
}

.upload-warnings {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--pm-stone);
}

.upload-warnings h4 {
  color: var(--pm-risk);
  font-size: 13px;
}

.upload-warnings ul,
.skipped-details ul {
  margin: 9px 0 0;
  padding-left: 18px;
  color: var(--pm-graphite);
  font-size: 12px;
  line-height: 1.6;
}

.skipped-details {
  margin-top: 18px;
  border-top: 1px solid var(--pm-stone);
  padding-top: 14px;
}

.skipped-details summary {
  min-height: 36px;
  color: var(--pm-primary-dark);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.skipped-details li {
  margin-bottom: 7px;
}

.skipped-details code {
  display: block;
  color: var(--pm-ink);
  font-family: var(--pm-font-mono);
  overflow-wrap: anywhere;
}

.skipped-details span {
  color: var(--pm-muted);
}

.file-inventory {
  margin-top: 40px;
}

.inventory-header {
  align-items: flex-end;
}

.inventory-controls {
  display: grid;
  max-width: 620px;
  grid-template-columns: minmax(260px, 1fr) 180px;
  gap: 10px;
  margin-top: 18px;
}

.inventory-list {
  margin-top: 14px;
  border-top: 1px solid var(--pm-stone-strong);
}

.inventory-row {
  display: grid;
  min-height: 62px;
  grid-template-columns: minmax(280px, 1fr) 120px 100px 170px;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--pm-stone);
  background: var(--pm-surface);
  color: var(--pm-graphite);
  font-size: 12px;
}

.inventory-row-head {
  min-height: 42px;
  background: var(--pm-paper);
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.inventory-path {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.inventory-path strong {
  overflow: hidden;
  color: var(--pm-ink);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inventory-path code,
.inventory-time,
.inventory-role,
.inventory-number {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.inventory-path code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inventory-pagination {
  align-items: center;
  margin-top: 14px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.inventory-error,
.inventory-loading {
  margin-top: 14px;
  padding: 24px 0;
  border-top: 1px solid var(--pm-stone);
  border-bottom: 1px solid var(--pm-stone);
}

.inventory-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--pm-risk);
}

@media (max-width: 980px) {
  .source-management-grid {
    grid-template-columns: 1fr;
  }

  .readme-source {
    border-right: 0;
    border-bottom: 1px solid var(--pm-stone);
  }

  .inventory-list {
    overflow-x: auto;
  }

  .inventory-row {
    min-width: 820px;
  }
}

@media (max-width: 700px) {
  .source-manager-header,
  .inventory-header,
  .inventory-pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .source-actions,
  .source-actions :deep(.el-upload),
  .source-actions :deep(.el-button) {
    width: 100%;
  }

  .inventory-controls {
    max-width: none;
    grid-template-columns: 1fr;
  }

  .readme-source,
  .upload-source {
    padding: 20px 0;
  }

  .source-management-grid {
    background: transparent;
  }

  .readme-source :deep(.el-textarea__inner) {
    min-height: 300px;
  }

  .inventory-pagination :deep(.el-pagination) {
    max-width: 100%;
    overflow-x: auto;
  }
}
</style>
