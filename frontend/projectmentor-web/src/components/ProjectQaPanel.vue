<template>
  <section class="evidence-qa-view" :aria-label="t('projects.v5.qa.title')">
    <EmptyState
      v-if="!hasProjectFiles"
      variant="compact"
      :title="t('qa.v6.noEvidenceTitle')"
      :description="t('qa.v6.noEvidenceDescription')"
    >
      <el-button type="primary" @click="emit('open-sources')">{{ t('qa.v6.manageSources') }}</el-button>
    </EmptyState>

    <template v-else>
      <QuestionComposer
        v-model="question"
        :project-name="projectName || t('common.unnamedProject')"
        :suggestions="questionExamples"
        :loading="asking"
        :disabled="projectUnavailable"
        :credit-cost="AI_CREDIT_COSTS.PROJECT_QA"
        @submit="handleAsk"
        @select="fillQuestion"
      />

      <p class="qa-limitation" role="note">{{ t('qa.v6.limitation') }}</p>

      <section v-if="projectUnavailable" class="qa-state qa-state-error" role="alert">
        <div>
          <h3>{{ t('qa.v6.projectUnavailableTitle') }}</h3>
          <p>{{ t('qa.v6.projectUnavailableDescription') }}</p>
        </div>
      </section>

      <template v-else>
        <section v-if="askError" class="qa-state qa-state-error" role="alert">
          <div>
            <h3>{{ t('qa.v6.questionFailedTitle') }}</h3>
            <p>{{ t('qa.v6.questionFailedDescription') }}</p>
          </div>
          <el-button @click="handleAsk">{{ t('qa.v6.retry') }}</el-button>
        </section>

        <div v-if="currentAnswer" ref="resultRegion" class="qa-result-region" tabindex="-1" aria-live="polite">
          <AnswerPanel
            :record="currentAnswer"
            :project-files="projectFiles"
            :claims="claims"
            @follow-up="fillQuestion"
          />
        </div>

        <EmptyState
          v-else-if="!historyLoading && !historyError"
          variant="compact"
          :title="t('qa.v6.firstQuestionTitle')"
          :description="t('qa.v6.firstQuestionDescription')"
        />

        <section class="recent-questions" aria-labelledby="recent-questions-heading">
          <header>
            <div>
              <h3 id="recent-questions-heading">{{ t('qa.v6.recent') }}</h3>
              <p>{{ t('qa.v6.recentDescription') }}</p>
            </div>
            <el-button :loading="historyLoading" @click="loadHistory">{{ t('qa.v6.refresh') }}</el-button>
          </header>

          <div v-if="historyLoading" class="history-status" role="status" aria-live="polite">
            {{ t('qa.v6.loadingRetrieval') }}
          </div>

          <section v-else-if="historyError" class="qa-state qa-state-error" role="alert">
            <div>
              <h4>{{ t('qa.v6.historyFailedTitle') }}</h4>
              <p>{{ t('qa.v6.historyFailedDescription') }}</p>
            </div>
            <el-button @click="loadHistory">{{ t('qa.v6.retry') }}</el-button>
          </section>

          <div v-else-if="historyRecords.length" class="recent-question-list">
            <article
              v-for="record in historyRecords"
              :key="record.id"
              class="recent-question-row"
              :class="{ 'is-current': currentAnswer?.id === record.id }"
              :aria-current="currentAnswer?.id === record.id ? 'true' : undefined"
            >
              <div class="recent-question-copy">
                <strong>{{ record.question }}</strong>
                <div>
                  <StatusLabel :status="historyEvidenceLevel(record)" :label="historyEvidenceLabel(record)" />
                  <span>{{ formatDate(record.createTime) }}</span>
                  <span>{{ t('qa.v6.sourceCount', { count: record.evidences?.length || 0 }) }}</span>
                </div>
              </div>
              <div class="recent-question-actions">
                <button type="button" @click="reviewRecord(record)">{{ t('qa.v6.review') }}</button>
                <button type="button" class="delete-question" @click="handleDelete(record)">{{ t('qa.v6.delete') }}</button>
              </div>
            </article>
          </div>
        </section>
      </template>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'

import { getMyCredits } from '@/api/credit'
import { askProjectQuestion, deleteProjectQaRecord, getProjectQaHistory } from '@/api/projectQa'
import AnswerPanel from '@/components/AnswerPanel.vue'
import EmptyState from '@/components/EmptyState.vue'
import QuestionComposer from '@/components/QuestionComposer.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import { AI_CREDIT_COSTS } from '@/constants/creditCosts'
import { useUserStore } from '@/stores/user'
import type { ClaimEvidenceItem, ProjectFile, ProjectQaHistoryRecord, ProjectQaResponse } from '@/types/api'

type QaDisplayRecord = ProjectQaResponse & {
  id?: number
  createTime?: string
}

const props = defineProps<{
  projectId: number
  projectName: string
  hasProjectFiles: boolean
  projectFiles: ProjectFile[]
  claims: ClaimEvidenceItem[]
}>()

const emit = defineEmits<{
  'open-sources': []
}>()

const { locale, t } = useI18n()
const userStore = useUserStore()

const question = ref('')
const asking = ref(false)
const historyLoading = ref(false)
const historyError = ref(false)
const askError = ref(false)
const projectUnavailable = ref(false)
const currentAnswer = ref<QaDisplayRecord>()
const historyRecords = ref<ProjectQaHistoryRecord[]>([])
const resultRegion = ref<HTMLElement>()

const questionExamples = computed(() => [
  t('qa.v6.examples.implementation'),
  t('qa.v6.examples.boundary'),
  t('qa.v6.examples.evidence')
])

async function loadHistory() {
  historyLoading.value = true
  historyError.value = false
  try {
    const records = await getProjectQaHistory(props.projectId)
    historyRecords.value = records
    if (!currentAnswer.value && records.length) currentAnswer.value = records[0]
    projectUnavailable.value = false
  } catch (error) {
    if (isProjectUnavailableError(error)) projectUnavailable.value = true
    else historyError.value = true
  } finally {
    historyLoading.value = false
  }
}

async function handleAsk() {
  const trimmedQuestion = question.value.trim()
  if (!trimmedQuestion || asking.value) return

  try {
    await ElMessageBox.confirm(
      t('credits.confirmAiUse', { count: AI_CREDIT_COSTS.PROJECT_QA }),
      t('qa.v6.confirmTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }

  asking.value = true
  askError.value = false
  try {
    const response = await askProjectQuestion(props.projectId, trimmedQuestion)
    currentAnswer.value = { ...response }
    question.value = ''
    await loadHistory()

    const savedRecord = historyRecords.value.find((record) => (
      record.question === response.question && record.answer === response.answer
    ))
    if (savedRecord) currentAnswer.value = savedRecord

    await focusResult()
  } catch (error) {
    if (isProjectUnavailableError(error)) projectUnavailable.value = true
    else askError.value = true
  } finally {
    await syncCredits()
    asking.value = false
  }
}

function fillQuestion(value: string) {
  question.value = value
  nextTick(() => document.getElementById('project-evidence-question')?.focus())
}

async function reviewRecord(record: ProjectQaHistoryRecord) {
  currentAnswer.value = record
  await focusResult()
}

async function focusResult() {
  await nextTick()
  resultRegion.value?.focus({ preventScroll: false })
}

async function handleDelete(record: ProjectQaHistoryRecord) {
  try {
    await ElMessageBox.confirm(t('qa.v6.deleteConfirm'), t('qa.v6.deleteTitle'), {
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteProjectQaRecord(props.projectId, record.id)
    historyRecords.value = historyRecords.value.filter((item) => item.id !== record.id)
    if (currentAnswer.value?.id === record.id) currentAnswer.value = historyRecords.value[0]
    ElMessage.success(t('qa.v6.deleted'))
  } catch {
    ElMessage.error(t('qa.v6.deleteFailed'))
  }
}

function historyEvidenceLevel(record: ProjectQaHistoryRecord) {
  return record.evidenceLevel || (record.evidences?.length ? 'AVAILABLE' : 'NO_EVIDENCE')
}

function historyEvidenceLabel(record: ProjectQaHistoryRecord) {
  const level = historyEvidenceLevel(record)
  const key = level === 'NO_EVIDENCE' ? 'NONE' : level
  return t(`qa.v6.evidenceLevels.${key}`)
}

function formatDate(value?: string) {
  if (!value) return '—'
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return value.replace('T', ' ').slice(0, 19)
  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit'
  }).format(parsed)
}

function isProjectUnavailableError(error: unknown) {
  const responseStatus = (error as { response?: { status?: number } })?.response?.status
  const message = error instanceof Error ? error.message.toLowerCase() : ''
  return responseStatus === 404
    || message.includes('项目不存在')
    || message.includes('无权访问')
    || message.includes('project not found')
}

async function syncCredits() {
  try {
    const credits = await getMyCredits()
    userStore.updateCredits(credits.remainingCredits)
  } catch {
    // The shell refreshes the balance during the next normal credit request.
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.evidence-qa-view {
  display: grid;
  min-width: 0;
  gap: 34px;
}

.qa-limitation {
  margin: -17px 0 0;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--pm-stone);
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.6;
}

.qa-result-region {
  min-width: 0;
  scroll-margin-top: 144px;
}

.qa-result-region:focus {
  outline-offset: 8px;
}

.qa-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 20px 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
}

.qa-state h3,
.qa-state h4 {
  margin: 0;
  color: var(--pm-risk);
  font-size: 15px;
  font-weight: 600;
}

.qa-state p {
  max-width: 68ch;
  margin: 6px 0 0;
  color: var(--pm-graphite);
  font-size: 13px;
  line-height: 1.65;
}

.qa-state :deep(.el-button) {
  min-height: 44px;
}

.recent-questions {
  padding-top: 2px;
  border-top: 1px solid var(--pm-stone-strong);
}

.recent-questions > header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 0 16px;
}

.recent-questions h3 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.recent-questions header p {
  max-width: 68ch;
  margin: 7px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.recent-questions header :deep(.el-button) {
  min-height: 44px;
}

.history-status {
  min-height: 56px;
  padding: 18px 0;
  border-top: 1px solid var(--pm-stone);
  color: var(--pm-muted);
  font-size: 13px;
}

.recent-question-list {
  border-top: 1px solid var(--pm-ink);
}

.recent-question-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  min-height: 76px;
  padding: 12px 0;
  border-bottom: 1px solid var(--pm-stone);
}

.recent-question-row.is-current {
  background: var(--pm-primary-soft);
}

.recent-question-copy {
  min-width: 0;
  padding-left: 10px;
}

.recent-question-copy strong {
  display: block;
  color: var(--pm-ink);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.recent-question-copy > div {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 18px;
  margin-top: 7px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.recent-question-actions {
  display: flex;
  gap: 8px;
}

.recent-question-actions button {
  min-width: 58px;
  min-height: 44px;
  padding: 0 10px;
  border: 1px solid var(--pm-stone-strong);
  border-radius: var(--pm-radius-sm);
  background: transparent;
  color: var(--pm-primary-dark);
  cursor: pointer;
  font: 600 12px/1 var(--pm-font-sans);
}

.recent-question-actions button:hover {
  border-color: var(--pm-ink);
  color: var(--pm-ink);
}

.recent-question-actions .delete-question {
  color: var(--pm-risk);
}

@media (max-width: 720px) {
  .qa-state,
  .recent-questions > header {
    align-items: flex-start;
    flex-direction: column;
  }

  .recent-question-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .recent-question-actions {
    width: 100%;
  }

  .recent-question-actions button {
    flex: 1;
  }
}

@media (max-width: 390px) {
  .evidence-qa-view {
    gap: 28px;
  }

  .recent-question-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
