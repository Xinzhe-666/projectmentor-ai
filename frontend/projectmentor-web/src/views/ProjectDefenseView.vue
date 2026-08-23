<template>
  <div class="project-defense-page">
    <nav class="project-defense-page__back" :aria-label="t('defense.navigation')">
      <el-button text :icon="ArrowLeft" @click="openProject">{{ t('defense.backToProject') }}</el-button>
    </nav>

    <DefenseOverview
      v-if="!session"
      :project="project"
      :report="report"
      :loading="loadingContext || restoringSession"
      :starting="startingSession"
      :error="overviewError"
      :error-description="overviewErrorDescription"
      @start="startSession"
      @retry="retryOverview"
      @open-project="openProject"
    />

    <section
      v-else-if="session.status === 'INSUFFICIENT_DATA'"
      class="project-defense-page__insufficient"
      aria-labelledby="defense-insufficient-title"
    >
      <StatusLabel :status="session.status" :label="t('defense.status.insufficientData')" />
      <EmptyState
        :title="t('defense.insufficient.title')"
        :description="t('defense.insufficient.description')"
      >
        <el-button type="primary" :loading="startingSession" @click="restartSession">{{ t('defense.overview.retryLatest') }}</el-button>
        <el-button @click="openProject">{{ t('defense.overview.openProject') }}</el-button>
      </EmptyState>
    </section>

    <DefenseReview
      v-else-if="review"
      :review="review"
      @start-again="restartSession"
      @open-project="openProject"
    />

    <DefenseSession
      v-else
      :session="session"
      :questions="questions"
      :loading="loadingQuestions"
      :error="sessionError"
      :blocking-error="sessionErrorBlocking"
      :submitting-question-id="submittingQuestionId"
      @submit="submitAnswer"
      @retry="refreshSession"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowLeft } from '@element-plus/icons-vue'

import { listMyReports } from '@/api/analysis'
import {
  createDefenseSession,
  getDefenseQuestions,
  getDefenseSessionReview,
  submitDefenseAnswer
} from '@/api/defense'
import { getProjectDetail } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import DefenseOverview from '@/components/defense/DefenseOverview.vue'
import DefenseReview from '@/components/defense/DefenseReview.vue'
import DefenseSession from '@/components/defense/DefenseSession.vue'
import type {
  DefenseQuestionResponse,
  DefenseSessionResponse,
  DefenseSessionReviewResponse,
  Project,
  ReportListItem
} from '@/types/api'

const ACTIVE_SESSION_STORAGE_PREFIX = 'pmai-defense-active-session:v1'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const project = ref<Project>()
const report = ref<ReportListItem>()
const session = ref<DefenseSessionResponse>()
const questions = ref<DefenseQuestionResponse[]>([])
const review = ref<DefenseSessionReviewResponse>()
const loadingContext = ref(false)
const restoringSession = ref(false)
const startingSession = ref(false)
const loadingQuestions = ref(false)
const submittingQuestionId = ref<number>()
const contextError = ref<string>()
const sessionError = ref<string>()
const sessionErrorDescription = ref<string>()
const sessionErrorBlocking = ref(false)
let loadVersion = 0

const projectId = computed(() => Number(route.params.id))
const overviewError = computed(() => contextError.value || (!session.value ? sessionError.value : undefined))
const overviewErrorDescription = computed(() =>
  contextError.value ? t('defense.errors.contextRecovery') : sessionErrorDescription.value
)

watch(
  () => route.params.id,
  () => loadPage(),
  { immediate: true }
)

async function loadPage() {
  const version = ++loadVersion
  resetPageState()

  if (!Number.isInteger(projectId.value) || projectId.value <= 0) {
    contextError.value = t('defense.errors.invalidProject')
    return
  }

  loadingContext.value = true
  try {
    const [projectResult, reportPage] = await Promise.all([
      getProjectDetail(projectId.value),
      listMyReports({ projectId: projectId.value, page: 1, size: 1 })
    ])

    if (version !== loadVersion) {
      return
    }

    project.value = projectResult
    report.value = reportPage.records[0]
  } catch {
    if (version === loadVersion) {
      contextError.value = t('defense.errors.context')
    }
    return
  } finally {
    if (version === loadVersion) {
      loadingContext.value = false
    }
  }

  const savedSessionId = readStoredSessionId()
  if (savedSessionId && version === loadVersion) {
    await restoreSession(savedSessionId, version)
  }
}

function resetPageState() {
  project.value = undefined
  report.value = undefined
  session.value = undefined
  questions.value = []
  review.value = undefined
  contextError.value = undefined
  sessionError.value = undefined
  sessionErrorDescription.value = undefined
  sessionErrorBlocking.value = false
  submittingQuestionId.value = undefined
}

async function restoreSession(sessionId: number, version = loadVersion) {
  restoringSession.value = true
  sessionError.value = undefined
  try {
    const restored = await getDefenseSessionReview(sessionId, true)
    if (version !== loadVersion) {
      return
    }

    if (restored.session.projectId !== projectId.value) {
      clearStoredSessionId()
      return
    }

    applySessionReview(restored)
  } catch (error) {
    if (version !== loadVersion) {
      return
    }

    if (getResponseStatus(error) === 404) {
      clearStoredSessionId()
      return
    }

    sessionError.value = t('defense.errors.review')
    sessionErrorDescription.value = t('defense.errors.restoreRecovery')
  } finally {
    if (version === loadVersion) {
      restoringSession.value = false
    }
  }
}

async function startSession() {
  if (!report.value || startingSession.value) {
    return
  }

  startingSession.value = true
  sessionError.value = undefined
  sessionErrorDescription.value = undefined
  sessionErrorBlocking.value = false
  review.value = undefined
  questions.value = []
  try {
    const created = await createDefenseSession(projectId.value, {
      reportId: report.value.reportId,
      mode: 'EVIDENCE_DEFENSE'
    })
    session.value = created
    writeStoredSessionId(created.id)

    if (created.status !== 'INSUFFICIENT_DATA') {
      await loadQuestions(created.id)
    }
  } catch {
    session.value = undefined
    sessionError.value = t('defense.errors.session')
    sessionErrorDescription.value = t('defense.errors.sessionRecovery')
  } finally {
    startingSession.value = false
  }
}

async function loadQuestions(sessionId: number) {
  loadingQuestions.value = true
  sessionError.value = undefined
  sessionErrorBlocking.value = false
  try {
    questions.value = await getDefenseQuestions(sessionId)
  } catch {
    sessionError.value = t('defense.errors.questions')
    sessionErrorBlocking.value = true
  } finally {
    loadingQuestions.value = false
  }
}

async function submitAnswer(payload: { questionId: number; answerText: string }) {
  if (!session.value || submittingQuestionId.value) {
    return
  }

  submittingQuestionId.value = payload.questionId
  sessionError.value = undefined
  sessionErrorBlocking.value = false
  try {
    const answer = await submitDefenseAnswer(payload.questionId, payload.answerText)
    questions.value = questions.value.map((question) =>
      question.id === payload.questionId ? { ...question, answer } : question
    )
  } catch {
    sessionError.value = t('defense.errors.submit')
    submittingQuestionId.value = undefined
    return
  }

  try {
    await refreshSession()
  } finally {
    submittingQuestionId.value = undefined
  }
}

async function refreshSession() {
  if (!session.value) {
    await retryOverview()
    return
  }

  sessionError.value = undefined
  sessionErrorBlocking.value = false
  try {
    const refreshed = await getDefenseSessionReview(session.value.id, true)
    applySessionReview(refreshed)
  } catch {
    sessionError.value = t('defense.errors.review')
    sessionErrorBlocking.value = questions.value.length === 0
  }
}

function applySessionReview(value: DefenseSessionReviewResponse) {
  session.value = value.session
  questions.value = value.questions
  review.value = value.session.status === 'COMPLETED' ? value : undefined
  writeStoredSessionId(value.session.id)
}

async function retryOverview() {
  const savedSessionId = readStoredSessionId()
  if (savedSessionId && project.value) {
    await restoreSession(savedSessionId)
    return
  }
  await loadPage()
}

async function restartSession() {
  session.value = undefined
  questions.value = []
  review.value = undefined
  sessionError.value = undefined
  sessionErrorDescription.value = undefined
  sessionErrorBlocking.value = false
  clearStoredSessionId()
  await startSession()
}

function openProject() {
  router.push(`/projects/${projectId.value}`)
}

function storageKey() {
  return `${ACTIVE_SESSION_STORAGE_PREFIX}:${projectId.value}`
}

function readStoredSessionId() {
  try {
    const value = Number(localStorage.getItem(storageKey()))
    return Number.isInteger(value) && value > 0 ? value : undefined
  } catch {
    return undefined
  }
}

function writeStoredSessionId(sessionId: number) {
  try {
    localStorage.setItem(storageKey(), String(sessionId))
  } catch {
    // The live session remains usable if browser storage is unavailable.
  }
}

function clearStoredSessionId() {
  try {
    localStorage.removeItem(storageKey())
  } catch {
    // Storage cleanup failure does not block loading a fresh session.
  }
}

function getResponseStatus(error: unknown) {
  return (error as { response?: { status?: number } })?.response?.status
}
</script>

<style scoped>
.project-defense-page {
  min-width: 0;
}

.project-defense-page__back {
  margin: -10px 0 22px;
}

.project-defense-page__back :deep(.el-button) {
  min-height: 40px;
  margin-left: -14px;
  color: var(--pm-primary-dark);
}

.project-defense-page__insufficient > .status-label {
  margin-bottom: 14px;
}

.project-defense-page__insufficient :deep(.empty-state) {
  min-height: 300px;
  align-content: center;
  justify-items: center;
}

@media (max-width: 520px) {
  .project-defense-page__back {
    margin-top: -6px;
    margin-bottom: 16px;
  }
}
</style>
