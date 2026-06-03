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
              {{ t('interview.finishWithReview') }}
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
          <el-tag type="primary" effect="light">
            {{ t('interview.progress', { current: currentProgress, total: MAX_INTERVIEW_QUESTIONS }) }}
          </el-tag>
          <el-tag v-if="session.totalScore !== undefined" type="success" effect="light">
            {{ t('interview.totalScore', { score: session.totalScore }) }}
          </el-tag>
          <el-button :icon="Printer" class="print-hidden" @click="handlePrint">{{ t('common.printPdf') }}</el-button>
        </div>
      </div>
      <div class="panel-body interview-workspace">
        <aside class="question-list-panel print-hidden">
          <div class="question-list-head">
            <h4>{{ t('interview.questionList') }}</h4>
            <span>{{ t('interview.questionCount', { count: questionItems.length, total: MAX_INTERVIEW_QUESTIONS }) }}</span>
          </div>
          <button
            v-for="item in questionItems"
            :key="item.message.id"
            class="question-list-item"
            :class="`is-${item.status.toLowerCase()}`"
            type="button"
          >
            <span class="question-index">{{ item.index }}</span>
            <span class="question-list-copy">
              <strong>{{ questionStatusLabel(item.status) }}</strong>
              <span>{{ item.message.content }}</span>
            </span>
          </button>
        </aside>

        <div class="interview-main">
          <div class="message-list">
            <article
              v-for="message in messages"
              :key="message.id"
              class="chat-message"
              :class="messageClass(message.role)"
            >
              <div class="chat-role">{{ roleLabel(message.role) }}</div>
              <div class="message-content">{{ message.content }}</div>
              <div v-if="message.role === 'INTERVIEWER'" class="question-evidence">
                <el-tag size="small" effect="light">{{ questionCategoryLabel(message.questionCategory) }}</el-tag>
                <el-tag size="small" :type="evidenceStrengthTagType(message.evidenceStrength)" effect="light">
                  {{ t('interview.evidenceStrength', { strength: evidenceStrengthLabel(message.evidenceStrength) }) }}
                </el-tag>
                <span v-if="message.sourceFile">{{ t('interview.sourceFile', { file: message.sourceFile }) }}</span>
                <span v-if="message.reason">{{ message.reason }}</span>
              </div>
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
            <el-button
              :loading="skipping"
              :disabled="session.status === 'FINISHED' || !currentQuestion"
              @click="handleSkipQuestion"
            >
              {{ t('interview.skipQuestion') }}
            </el-button>
            <el-button
              :loading="finishing"
              :disabled="session.status === 'FINISHED'"
              @click="handleFinish"
            >
              {{ t('interview.finishWithReview') }}
            </el-button>
            <span class="muted">{{ t('interview.submitHint') }}</span>
          </div>

          <section v-if="session.summary || session.totalScore !== undefined" class="interview-review">
            <div class="review-metrics">
              <div class="metric-card">
                <span>{{ t('interview.interviewScore') }}</span>
                <strong>{{ session.totalScore ?? 0 }}</strong>
              </div>
              <div class="metric-card">
                <span>{{ t('interview.answeredCount') }}</span>
                <strong>{{ answeredCount }}</strong>
              </div>
              <div class="metric-card">
                <span>{{ t('interview.skippedCount') }}</span>
                <strong>{{ skippedCount }}</strong>
              </div>
            </div>
            <div class="summary-text">
              <h4>{{ t('interview.reviewTitle') }}</h4>
              <MarkdownBlock :content="session.summary" />
            </div>
          </section>
        </div>
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
const skipping = ref(false)
const projectLoading = ref(false)
const session = ref<InterviewSession>()
const projects = ref<Project[]>([])
const answer = ref('')
const MAX_INTERVIEW_QUESTIONS = 8
const SKIP_ANSWER_PREFIX = '[PM_INTERVIEW_SKIP]'

type QuestionStatus = 'CURRENT' | 'ANSWERED' | 'UNANSWERED' | 'SKIPPED'

const form = reactive<{
  projectId?: number
  mode: string
}>({
  projectId: undefined,
  mode: 'TECH_DEEP_DIVE'
})

const messages = computed(() => session.value?.messages || [])

const interviewerMessages = computed(() => messages.value.filter((message) => message.role === 'INTERVIEWER'))

const userMessages = computed(() => messages.value.filter((message) => message.role === 'USER'))

const answeredCount = computed(() => userMessages.value.filter((message) => !message.skipped).length)

const skippedCount = computed(() => userMessages.value.filter((message) => message.skipped).length)

const currentProgress = computed(() => {
  if (!session.value) {
    return 0
  }

  if (session.value.status === 'FINISHED') {
    return Math.min(Math.max(interviewerMessages.value.length, answeredCount.value + skippedCount.value), MAX_INTERVIEW_QUESTIONS)
  }

  return Math.min(answeredCount.value + skippedCount.value + 1, MAX_INTERVIEW_QUESTIONS)
})

const questionItems = computed(() =>
  interviewerMessages.value.map((message, index) => {
    const answerMessage = userMessages.value[index]
    const isCurrent = session.value?.status !== 'FINISHED' && !answerMessage && index === userMessages.value.length
    let status: QuestionStatus = 'UNANSWERED'

    if (answerMessage?.skipped) {
      status = 'SKIPPED'
    } else if (answerMessage) {
      status = 'ANSWERED'
    } else if (isCurrent) {
      status = 'CURRENT'
    }

    return {
      index: message.questionIndex || index + 1,
      message,
      answerMessage,
      status
    }
  })
)

const currentQuestion = computed(() => questionItems.value.find((item) => item.status === 'CURRENT'))

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

function questionStatusLabel(status: QuestionStatus) {
  return t(`interview.questionStatuses.${status}`)
}

function questionCategoryLabel(category?: string) {
  if (!category) {
    return t('interview.questionCategories.UNKNOWN')
  }

  const key = `interview.questionCategories.${category}`
  const label = t(key)
  return label === key ? category : label
}

function evidenceStrengthLabel(strength?: string) {
  const normalized = strength || 'NONE'
  const key = `interview.evidenceStrengths.${normalized}`
  const label = t(key)
  return label === key ? normalized : label
}

function evidenceStrengthTagType(strength?: string) {
  const typeMap: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    STRONG: 'success',
    MEDIUM: 'warning',
    WEAK: 'info',
    NONE: 'danger'
  }

  return typeMap[strength || 'NONE'] || 'info'
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

async function handleSkipQuestion() {
  if (!session.value) {
    ElMessage.warning(t('interview.startFirst'))
    return
  }

  skipping.value = true
  try {
    session.value = await submitAnswer(session.value.id, `${SKIP_ANSWER_PREFIX} ${t('interview.skipSubmitted')}`)
    answer.value = ''
  } finally {
    skipping.value = false
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

.interview-workspace {
  display: grid;
  grid-template-columns: minmax(220px, 300px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.question-list-panel {
  position: sticky;
  top: 18px;
  display: grid;
  max-height: 640px;
  gap: 10px;
  overflow: auto;
  padding: 14px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.question-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.question-list-head h4 {
  margin: 0;
  color: var(--pm-ink);
}

.question-list-head span {
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 700;
}

.question-list-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  padding: 10px;
  border: 1px solid rgba(223, 230, 240, 0.96);
  border-radius: 8px;
  background: #ffffff;
  color: #344054;
  text-align: left;
}

.question-list-item.is-current {
  border-color: rgba(31, 111, 235, 0.38);
  background: #eef6ff;
}

.question-list-item.is-answered {
  border-color: rgba(20, 184, 166, 0.28);
  background: #f0fdfa;
}

.question-list-item.is-skipped {
  border-color: rgba(245, 158, 11, 0.32);
  background: #fffbeb;
}

.question-index {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #ffffff;
  color: var(--pm-primary);
  font-weight: 900;
}

.question-list-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.question-list-copy strong {
  color: var(--pm-ink);
  font-size: 12px;
}

.question-list-copy span {
  display: -webkit-box;
  overflow: hidden;
  color: #475467;
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.interview-main {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.message-content {
  line-height: 1.75;
  white-space: pre-wrap;
}

.question-evidence {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.chat-message p {
  margin: 10px 0;
  line-height: 1.7;
}

.interview-review {
  display: grid;
  gap: 16px;
  align-items: stretch;
}

.review-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
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

@media (max-width: 900px) {
  .interview-workspace,
  .review-metrics {
    grid-template-columns: 1fr;
  }

  .question-list-panel {
    position: static;
    max-height: 360px;
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

  .interview-review,
  .summary-text,
  :global(.metric-card) {
    break-inside: avoid !important;
    page-break-inside: avoid !important;
  }
}
</style>
