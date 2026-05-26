<template>
  <div class="page-stack">
    <section class="panel interview-controls print-hidden">
      <div class="panel-title">
        <div>
          <h2>模拟面试</h2>
          <p class="muted">选择项目和模式，让系统围绕项目证据连续追问。</p>
        </div>
      </div>
      <div class="panel-body">
        <el-form :model="form" label-width="110px">
          <el-form-item label="项目" required>
            <el-select
              v-model="form.projectId"
              filterable
              placeholder="请选择项目"
              :loading="projectLoading"
              class="wide-control"
            >
              <el-option
                v-for="project in projects"
                :key="project.id"
                :label="`${project.name}（#${project.id}）`"
                :value="project.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模式">
            <el-select v-model="form.mode" class="wide-control">
              <el-option label="HR 真实度" value="HR_REALITY" />
              <el-option label="技术深挖" value="TECH_DEEP_DIVE" />
              <el-option label="压力面试" value="PRESSURE" />
              <el-option label="华为后端" value="HUAWEI_BACKEND" />
              <el-option label="AI 项目" value="AI_PROJECT" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="starting" @click="handleStart">开始面试</el-button>
            <el-button :disabled="!session || session.status === 'FINISHED'" :loading="finishing" @click="handleFinish">
              结束面试
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section v-if="session" class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ session.projectName || `项目 ${session.projectId}` }}</h3>
          <p class="muted">模式：{{ modeLabel(session.mode) }} · 状态：{{ session.status }}</p>
        </div>
        <div class="interview-title-actions">
          <el-tag v-if="session.totalScore !== undefined" type="success" effect="light">
            总分 {{ session.totalScore }}
          </el-tag>
          <el-button :icon="Printer" class="print-hidden" @click="handlePrint">打印 / 保存复盘</el-button>
        </div>
      </div>
      <div class="panel-body page-stack">
        <div class="message-list">
          <article
            v-for="message in messages"
            :key="message.id"
            class="chat-message"
            :class="messageClass(message.role)"
          >
            <div class="chat-role">{{ roleLabel(message.role) }}</div>
            <div class="message-content">{{ message.content }}</div>
            <p v-if="message.feedback" class="muted">反馈：{{ message.feedback }}</p>
            <el-tag v-if="message.score !== undefined" size="small" effect="light">本轮评分 {{ message.score }}</el-tag>
          </article>
        </div>

        <el-input
          v-model="answer"
          type="textarea"
          :rows="4"
          :disabled="session.status === 'FINISHED'"
          placeholder="输入你的回答"
          class="answer-editor print-hidden"
        />
        <div class="toolbar answer-toolbar print-hidden">
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="session.status === 'FINISHED'"
            @click="handleSubmitAnswer"
          >
            提交回答
          </el-button>
          <span class="muted">提交后系统会继续追问或给出本轮反馈。</span>
        </div>

        <section v-if="session.summary || session.totalScore !== undefined" class="interview-summary">
          <div v-if="session.totalScore !== undefined" class="metric-card">
            <span>面试总分</span>
            <strong>{{ session.totalScore }}</strong>
          </div>
          <div class="summary-text">
            <h4>总结</h4>
            <MarkdownBlock :content="session.summary" />
          </div>
        </section>
      </div>
    </section>

    <section v-else class="panel">
      <div class="panel-body">
        <EmptyState title="还没有面试会话" description="选择项目与模式后，点击开始面试进入对话。" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'

import { finishInterview, startInterview, submitAnswer } from '@/api/interview'
import { listProjects } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import type { InterviewSession, Project } from '@/types/api'

const starting = ref(false)
const submitting = ref(false)
const finishing = ref(false)
const projectLoading = ref(false)
const session = ref<InterviewSession>()
const projects = ref<Project[]>([])
const answer = ref('')
const form = reactive<{
  projectId?: number
  mode: string
}>({
  projectId: undefined,
  mode: 'TECH_DEEP_DIVE'
})

const messages = computed(() => session.value?.messages || [])

const modeMap: Record<string, string> = {
  HR_REALITY: 'HR 真实度',
  TECH_DEEP_DIVE: '技术深挖',
  PRESSURE: '压力面试',
  HUAWEI_BACKEND: '华为后端',
  AI_PROJECT: 'AI 项目'
}

function modeLabel(mode: string) {
  return modeMap[mode] || mode
}

function roleLabel(role: string) {
  const roleMap: Record<string, string> = {
    INTERVIEWER: '面试官',
    USER: '你',
    SYSTEM: '系统'
  }

  return roleMap[role] || role
}

function messageClass(role: string) {
  if (role === 'USER') {
    return 'user'
  }

  if (role === 'SYSTEM') {
    return 'system'
  }

  return 'interviewer'
}

function handlePrint() {
  window.print()
}

async function loadProjects() {
  projectLoading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    projectLoading.value = false
  }
}

async function handleStart() {
  if (!form.projectId) {
    ElMessage.warning('请选择项目')
    return
  }

  starting.value = true
  try {
    session.value = await startInterview({
      projectId: form.projectId,
      mode: form.mode
    })
    answer.value = ''
  } finally {
    starting.value = false
  }
}

async function handleSubmitAnswer() {
  if (!session.value) {
    ElMessage.warning('请先开始面试')
    return
  }

  if (!answer.value.trim()) {
    ElMessage.warning('请输入回答')
    return
  }

  submitting.value = true
  try {
    session.value = await submitAnswer(session.value.id, answer.value)
    answer.value = ''
  } finally {
    submitting.value = false
  }
}

async function handleFinish() {
  if (!session.value) {
    return
  }

  finishing.value = true
  try {
    session.value = await finishInterview(session.value.id)
    ElMessage.success('面试已结束')
  } finally {
    finishing.value = false
  }
}

onMounted(loadProjects)
</script>

<style scoped>
.wide-control {
  width: min(420px, 100%);
}

.interview-title-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.message-content {
  line-height: 1.75;
  white-space: pre-wrap;
}

.chat-message p {
  margin: 10px 0;
  line-height: 1.7;
}

.interview-summary {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.summary-text {
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.summary-text h4 {
  margin: 0 0 12px;
}

@media (max-width: 620px) {
  .interview-summary {
    grid-template-columns: 1fr;
  }
}

@media print {
  :global(body) {
    background: #ffffff !important;
    color: #111827 !important;
  }

  :global(.app-header),
  :global(.app-sidebar),
  :global(.print-hidden),
  :global(.el-overlay),
  :global(.el-loading-mask),
  :global(.el-message),
  :global(.el-notification),
  :global(.el-popper) {
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

  :global(.page-stack) {
    gap: 14px !important;
  }

  :global(.panel),
  :global(.section-card),
  .summary-text,
  :global(.metric-card),
  :global(.chat-message) {
    background: #ffffff !important;
    color: #111827 !important;
    box-shadow: none !important;
  }

  :global(.panel) {
    border: 0 !important;
    border-radius: 0 !important;
  }

  :global(.panel-title) {
    padding: 0 0 12px !important;
    border-bottom: 1px solid #cbd5e1 !important;
  }

  :global(.panel-body) {
    padding: 16px 0 0 !important;
  }

  :global(.message-list) {
    max-height: none !important;
    overflow: visible !important;
    padding-right: 0 !important;
  }

  :global(.chat-message) {
    max-width: 100% !important;
    break-inside: avoid !important;
    page-break-inside: avoid !important;
    border-color: #cbd5e1 !important;
  }

  :global(.chat-message.user),
  :global(.chat-message.interviewer),
  :global(.chat-message.system) {
    align-self: stretch !important;
    background: #ffffff !important;
    text-align: left !important;
  }

  :global(.chat-role) {
    color: #1f2937 !important;
  }

  :global(.el-tag) {
    border-color: #cbd5e1 !important;
    background: #ffffff !important;
    color: #111827 !important;
  }

  :global(.muted) {
    color: #374151 !important;
  }

  .interview-summary,
  .summary-text,
  :global(.metric-card) {
    break-inside: avoid !important;
    page-break-inside: avoid !important;
  }

  .interview-summary {
    grid-template-columns: 150px minmax(0, 1fr);
  }
}
</style>
