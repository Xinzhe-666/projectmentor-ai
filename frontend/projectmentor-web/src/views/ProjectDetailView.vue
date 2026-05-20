<template>
  <div class="page-stack">
    <section class="panel" v-loading="loading">
      <div class="panel-title">
        <div>
          <h2>{{ project?.name || '项目详情' }}</h2>
          <p class="muted">{{ project?.description || '暂无项目描述' }}</p>
        </div>
        <el-tag>{{ project?.status || 'PENDING' }}</el-tag>
      </div>
      <div class="panel-body detail-grid">
        <div>
          <span class="muted">GitHub</span>
          <p>{{ project?.githubUrl || '-' }}</p>
        </div>
        <div>
          <span class="muted">项目类型</span>
          <p>{{ project?.projectType || '-' }}</p>
        </div>
        <div>
          <span class="muted">技术栈</span>
          <p>{{ project?.techStack || '-' }}</p>
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
          <h3>README 与 ZIP 文件</h3>
          <p class="muted">粘贴 README 或上传项目 ZIP，系统会保存白名单文本文件。</p>
        </div>
        <el-upload :show-file-list="false" :before-upload="beforeZipUpload">
          <el-button :icon="Upload" :loading="uploading">上传 ZIP</el-button>
        </el-upload>
      </div>
      <div class="panel-body readme-editor">
        <el-input
          v-model="readmeContent"
          type="textarea"
          :rows="9"
          placeholder="粘贴 README.md 内容"
        />
        <div class="toolbar">
          <el-button type="primary" :loading="savingReadme" @click="handleSaveReadme">保存 README</el-button>
          <span class="muted">ZIP 上传后会自动刷新文件列表。</span>
        </div>
      </div>
    </section>

    <section v-if="uploadResult" class="panel">
      <div class="panel-title">
        <h3>最近一次 ZIP 解析</h3>
        <el-tag type="success">保存 {{ uploadResult.savedFileCount }} 个文件</el-tag>
      </div>
      <div class="panel-body">
        <p class="muted">跳过 {{ uploadResult.skippedFileCount }} 个文件</p>
        <el-alert
          v-for="warning in uploadResult.warnings.slice(0, 6)"
          :key="warning"
          :title="warning"
          type="warning"
          show-icon
          :closable="false"
        />
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>项目文件</h3>
          <p class="muted">当前项目已保存的 README 和解析文件。</p>
        </div>
        <el-button :icon="Refresh" @click="loadFiles">刷新</el-button>
      </div>
      <div class="panel-body">
        <el-table :data="files" stripe>
          <el-table-column prop="filePath" label="文件路径" min-width="260" show-overflow-tooltip />
          <el-table-column prop="fileType" label="类型" width="150" />
          <el-table-column prop="contentLength" label="长度" width="120" />
          <el-table-column prop="updateTime" label="更新时间" min-width="180" />
        </el-table>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>审计动作</h3>
          <p class="muted">先做规则扫描，再启动异步 AI 审计报告。</p>
        </div>
        <div class="toolbar">
          <el-button :icon="Search" :loading="scanning" @click="handleScan">规则扫描</el-button>
          <el-button type="primary" :icon="Cpu" :loading="analyzing" @click="handleStartAnalyze">
            异步生成报告
          </el-button>
        </div>
      </div>
      <div class="panel-body">
        <el-progress v-if="task" :percentage="task.progress || 0" :status="task.status === 'FAILED' ? 'exception' : undefined" />
        <p v-if="task" class="muted">任务状态：{{ task.status }} {{ task.message || '' }}</p>

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
              <span>文件数</span>
              <strong>{{ scanResult.fileCount }}</strong>
            </div>
          </div>

          <el-table :data="scanResult.risks" stripe class="risk-table">
            <el-table-column prop="riskLevel" label="等级" width="100" />
            <el-table-column prop="riskType" label="类型" width="170" />
            <el-table-column prop="sourceFile" label="来源" min-width="180" />
            <el-table-column prop="message" label="说明" min-width="260" show-overflow-tooltip />
          </el-table>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cpu, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, type UploadProps } from 'element-plus'

import { getProjectDetail, listProjectFiles, saveReadme, uploadZip } from '@/api/project'
import { getTask, scanProject, startAnalyze } from '@/api/analysis'
import type { AnalysisTask, Project, ProjectFile, RuleScanResult, UploadZipResult } from '@/types/api'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.id)

const loading = ref(false)
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

async function loadProject() {
  loading.value = true
  try {
    project.value = await getProjectDetail(projectId)
  } finally {
    loading.value = false
  }
}

async function loadFiles() {
  files.value = await listProjectFiles(projectId)
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
    const latestTask = await getTask(taskId)
    task.value = latestTask

    if (latestTask.status === 'SUCCESS') {
      clearPolling()
      if (latestTask.reportId) {
        router.push(`/reports/${latestTask.reportId}`)
      }
    }

    if (latestTask.status === 'FAILED') {
      clearPolling()
      ElMessage.error(latestTask.failReason || '分析任务失败')
    }
  }, 2500)
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.detail-grid p {
  margin: 8px 0 0;
}

.readme-editor {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.scan-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 18px;
}

.risk-table {
  margin-top: 4px;
}

@media (max-width: 860px) {
  .detail-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
