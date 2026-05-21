<template>
  <div class="page-stack">
    <section class="panel" v-loading="loading">
      <div class="panel-title">
        <div>
          <h2>{{ project?.name || '项目详情' }}</h2>
          <p class="muted">{{ project?.description || '暂无项目描述' }}</p>
        </div>
        <el-tag :type="statusTagType(project?.status)" effect="light">{{ project?.status || 'PENDING' }}</el-tag>
      </div>
      <div class="panel-body detail-grid">
        <div>
          <span class="muted">项目名称</span>
          <p>{{ project?.name || '-' }}</p>
        </div>
        <div>
          <span class="muted">GitHub</span>
          <p class="link-text">{{ project?.githubUrl || '-' }}</p>
        </div>
        <div>
          <span class="muted">项目描述</span>
          <p>{{ project?.description || '-' }}</p>
        </div>
        <div>
          <span class="muted">技术栈</span>
          <p>{{ project?.techStack || '-' }}</p>
        </div>
        <div>
          <span class="muted">项目类型</span>
          <p>{{ project?.projectType || '-' }}</p>
        </div>
        <div>
          <span class="muted">创建时间</span>
          <p>{{ project?.createTime || '-' }}</p>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>README 保存</h3>
          <p class="muted">粘贴 README.md 内容，作为后续扫描和报告生成的核心证据。</p>
        </div>
        <el-button type="primary" :loading="savingReadme" @click="handleSaveReadme">保存 README</el-button>
      </div>
      <div class="panel-body readme-editor">
        <el-input
          v-model="readmeContent"
          type="textarea"
          :rows="10"
          placeholder="粘贴 README.md 内容"
        />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>ZIP 上传</h3>
          <p class="muted">支持直接上传项目 ZIP，系统会自动过滤 .git、target、node_modules、dist 等无关目录。建议 ZIP 不超过 50MB。</p>
        </div>
        <el-upload accept=".zip" :show-file-list="false" :before-upload="beforeZipUpload">
          <el-button :icon="Upload" :loading="uploading">上传 ZIP</el-button>
        </el-upload>
      </div>
      <div class="panel-body">
        <div v-if="uploadResult" class="upload-result">
          <div class="score-grid">
            <div class="metric-card">
              <span>已保存文件</span>
              <strong>{{ uploadResult.savedFileCount }}</strong>
            </div>
            <div class="metric-card">
              <span>已跳过文件</span>
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
            <el-table-column prop="filePath" label="文件路径" min-width="260" show-overflow-tooltip />
            <el-table-column prop="fileType" label="类型" width="150" />
            <el-table-column prop="contentLength" label="长度" width="120" />
          </el-table>
        </div>
        <EmptyState v-else title="还没有上传结果" description="上传 ZIP 后，这里会展示保存数量、跳过原因、警告和文件列表。" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>项目文件</h3>
          <p class="muted">当前项目已保存的 README 和解析文件。</p>
        </div>
        <el-button :icon="Refresh" :loading="fileLoading" @click="loadFiles">刷新</el-button>
      </div>
      <div class="panel-body">
        <el-table v-if="files.length || fileLoading" v-loading="fileLoading" :data="files" stripe>
          <el-table-column prop="filePath" label="文件路径" min-width="280" show-overflow-tooltip />
          <el-table-column prop="fileType" label="类型" width="150" />
          <el-table-column prop="contentLength" label="长度" width="120" />
          <el-table-column prop="updateTime" label="更新时间" min-width="180" />
        </el-table>
        <EmptyState v-else title="暂无项目文件" description="保存 README 或上传 ZIP 后，文件会出现在这里。" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>规则扫描</h3>
          <p class="muted">先用规则检查 README 和代码证据，快速发现高风险描述。</p>
        </div>
        <el-button :icon="Search" :loading="scanning" @click="handleScan">规则扫描</el-button>
      </div>
      <div class="panel-body">
        <div v-if="scanResult" class="scan-result">
          <div class="score-grid">
            <div class="metric-card">
              <span>风险总数</span>
              <strong>{{ scanResult.totalRiskCount }}</strong>
            </div>
            <div class="metric-card">
              <span>高风险</span>
              <strong>{{ scanResult.highRiskCount }}</strong>
            </div>
            <div class="metric-card">
              <span>中风险</span>
              <strong>{{ scanResult.mediumRiskCount }}</strong>
            </div>
            <div class="metric-card">
              <span>低风险</span>
              <strong>{{ scanResult.lowRiskCount }}</strong>
            </div>
          </div>

          <div class="subsection">
            <h4>风险点</h4>
            <RiskList :risks="scanResult.risks" />
          </div>

          <div class="subsection">
            <h4>证据链</h4>
            <EvidenceList :evidences="scanResult.evidences" />
          </div>

          <div class="subsection">
            <h4>建议</h4>
            <el-tag v-for="suggestion in scanResult.suggestions" :key="suggestion" class="suggestion-tag" effect="light">
              {{ suggestion }}
            </el-tag>
          </div>
        </div>
        <EmptyState v-else title="尚未扫描" description="点击规则扫描后，这里会展示风险统计、风险点、证据链和建议。" />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>异步生成报告</h3>
          <p class="muted">生成报告会消耗额度，任务运行中会每 1.5 秒刷新一次进度。</p>
        </div>
        <el-button type="primary" :icon="Cpu" :loading="analyzing" @click="handleStartAnalyze">开始生成报告</el-button>
      </div>
      <div class="panel-body async-panel">
        <TaskProgress v-if="task" :task="task" />
        <EmptyState v-else title="还没有分析任务" description="点击开始生成报告后，会展示任务状态和报告入口。" />
        <div v-if="task?.status === 'SUCCESS' && task.reportId" class="toolbar">
          <el-button type="primary" @click="router.push(`/reports/${task.reportId}`)">查看报告</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cpu, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, type UploadProps } from 'element-plus'

import { getTask, scanProject, startAnalyze } from '@/api/analysis'
import { getProjectDetail, listProjectFiles, saveReadme, uploadZip } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import EvidenceList from '@/components/EvidenceList.vue'
import RiskList from '@/components/RiskList.vue'
import TaskProgress from '@/components/TaskProgress.vue'
import type { AnalysisTask, Project, ProjectFile, RuleScanResult, UploadZipResult } from '@/types/api'

const route = useRoute()
const router = useRouter()
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

const MAX_ZIP_SIZE_BYTES = 50 * 1024 * 1024

const skipReasonLabels: Record<string, string> = {
  FILTERED_DIRECTORY: '过滤目录',
  NOT_WHITELIST: '非白名单',
  BINARY_FILE: '二进制文件',
  FILE_TOO_LARGE: '单文件过大',
  VALID_FILE_LIMIT: '文件数上限',
  TOTAL_TEXT_LIMIT: '文本总量上限',
  DANGEROUS_PATH: '危险路径',
  ENTRY_LIMIT: 'Entry 数上限'
}

const skipReasonEntries = computed(() => {
  const skippedByReason = uploadResult.value?.skippedByReason || {}

  return Object.entries(skippedByReason)
    .filter(([, count]) => count > 0)
    .map(([reason, count]) => ({
      reason,
      label: skipReasonLabels[reason] || reason,
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
    ElMessage.warning('请输入 README 内容')
    return
  }

  savingReadme.value = true
  try {
    await saveReadme(projectId, readmeContent.value)
    ElMessage.success('README 已保存')
    loadFiles()
  } finally {
    savingReadme.value = false
  }
}

const beforeZipUpload: UploadProps['beforeUpload'] = async (rawFile) => {
  if (!rawFile.name.toLowerCase().endsWith('.zip')) {
    ElMessage.warning('请上传 .zip 文件')
    return false
  }

  if (rawFile.size > MAX_ZIP_SIZE_BYTES) {
    ElMessage.error('ZIP 文件过大，当前最大支持 50MB。')
    return false
  }

  uploading.value = true
  try {
    uploadResult.value = await uploadZip(projectId, rawFile)
    ElMessage.success(`已保存 ${uploadResult.value.savedFileCount} 个文件`)
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
    ElMessage.success('规则扫描完成')
  } finally {
    scanning.value = false
  }
}

async function handleStartAnalyze() {
  analyzing.value = true
  try {
    task.value = await startAnalyze(projectId)
    ElMessage.success('分析任务已启动')
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
        ElMessage.success('报告生成完成')
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
}
</style>
