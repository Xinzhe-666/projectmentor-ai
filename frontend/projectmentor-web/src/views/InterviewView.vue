<template>
  <div class="page-stack">
    <section class="panel interview-controls print-hidden">
      <div class="panel-title">
        <div>
          <h2>{{ t('interview.title') }}</h2>
          <p class="muted">{{ t('interview.desc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <el-form :model="form" label-width="110px">
          <el-form-item :label="t('interview.project')" required>
            <el-select
              v-model="form.projectId"
              filterable
              :placeholder="t('interview.projectPlaceholder')"
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
          <el-form-item :label="t('interview.mode')">
            <el-select v-model="form.mode" class="wide-control">
              <el-option :label="t('interview.modes.HR_REALITY')" value="HR_REALITY" />
              <el-option :label="t('interview.modes.TECH_DEEP_DIVE')" value="TECH_DEEP_DIVE" />
              <el-option :label="t('interview.modes.PRESSURE')" value="PRESSURE" />
              <el-option :label="t('interview.modes.JAVA_BACKEND')" value="JAVA_BACKEND" />
              <el-option :label="t('interview.modes.AI_PROJECT')" value="AI_PROJECT" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="starting" @click="handleStart">{{ t('interview.start') }}</el-button>
            <el-button :disabled="!session || session.status === 'FINISHED'" :loading="finishing" @click="handleFinish">
              {{ t('interview.finish') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section v-if="session" class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ session.projectName || `${t('common.projectId')} ${session.projectId}` }}</h3>
          <p class="muted">{{ t('interview.modeStatus', { mode: modeLabel(session.mode), status: session.status }) }}</p>
        </div>
        <div class="interview-title-actions">
          <el-tag v-if="session.totalScore !== undefined" type="success" effect="light">
            {{ t('interview.totalScore', { score: session.totalScore }) }}
          </el-tag>
          <el-button :icon="Printer" class="print-hidden" @click="handlePrint">{{ t('common.printPdf') }}</el-button>
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
            <p v-if="message.feedback" class="muted">{{ t('interview.feedback') }}：{{ message.feedback }}</p>
            <el-tag v-if="message.score !== undefined" size="small" effect="light">{{ t('interview.roundScore', { score: message.score }) }}</el-tag>
          </article>
        </div>

        <el-input
          v-model="answer"
          type="textarea"
          :rows="4"
          :disabled="session.status === 'FINISHED'"
          :placeholder="t('interview.answerPlaceholder')"
          class="answer-editor print-hidden"
        />
        <div class="toolbar answer-toolbar print-hidden">
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="session.status === 'FINISHED'"
            @click="handleSubmitAnswer"
          >
            {{ t('interview.submitAnswer') }}
          </el-button>
          <span class="muted">{{ t('interview.submitHint') }}</span>
        </div>

        <section v-if="session.summary || session.totalScore !== undefined" class="interview-summary">
          <div v-if="session.totalScore !== undefined" class="metric-card">
            <span>{{ t('interview.interviewScore') }}</span>
            <strong>{{ session.totalScore }}</strong>
          </div>
          <div class="summary-text">
            <h4>{{ t('interview.summary') }}</h4>
            <MarkdownBlock :content="session.summary" />
          </div>
        </section>
      </div>
    </section>

    <section v-else class="panel">
      <div class="panel-body">
        <EmptyState :title="t('interview.emptyTitle')" :description="t('interview.emptyDesc')" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'

import { finishInterview, startInterview, submitAnswer } from '@/api/interview'
import { listProjects } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import type { InterviewSession, Project } from '@/types/api'

const starting = ref(false)
const { t } = useI18n()
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

function modeLabel(mode: string) {
  const key = `interview.modes.${mode}`
  const label = t(key)
  return label === key ? mode : label
}

function roleLabel(role: string) {
  const key = `interview.roles.${role}`
  const label = t(key)
  return label === key ? role : label
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
  ElMessage.info(t('report.printTip'))
  window.setTimeout(() => window.print(), 100)
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
    ElMessage.warning(t('interview.selectProject'))
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
    ElMessage.warning(t('interview.startFirst'))
    return
  }

  if (!answer.value.trim()) {
    ElMessage.warning(t('interview.answerRequired'))
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
    ElMessage.success(t('interview.finished'))
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
