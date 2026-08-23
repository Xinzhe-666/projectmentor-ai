<template>
  <section class="defense-session" aria-labelledby="defense-session-title">
    <header class="defense-session__header">
      <div>
        <div class="defense-session__title-line">
          <h2 id="defense-session-title">{{ t('defense.session.title') }}</h2>
          <StatusLabel :status="session.status" :label="sessionStatusLabel" />
        </div>
        <p>{{ t('defense.session.description') }}</p>
      </div>
      <div class="defense-session__identity">
        <span>{{ t('defense.session.sessionId', { id: session.id }) }}</span>
        <strong>{{ t('defense.session.progress', { answered: answeredCount, total: questions.length }) }}</strong>
      </div>
    </header>

    <div class="defense-session__progress" role="progressbar" :aria-valuenow="answeredCount" :aria-valuemin="0" :aria-valuemax="questions.length">
      <span :style="{ transform: `scaleX(${progressScale})` }" />
    </div>

    <div v-if="error && !blockingError" class="defense-session__error" role="alert">
      <span>{{ error }}</span>
      <el-button text @click="emit('retry')">{{ t('defense.retry') }}</el-button>
    </div>

    <div v-if="loading" class="defense-session__loading" aria-live="polite">
      <el-skeleton :rows="8" animated />
    </div>

    <EmptyState
      v-else-if="blockingError && error"
      :title="error"
      :description="t('defense.errors.contextRecovery')"
    >
      <el-button @click="emit('retry')">{{ t('defense.retry') }}</el-button>
    </EmptyState>

    <EmptyState
      v-else-if="!questions.length"
      :title="t('defense.session.empty')"
      :description="t('defense.session.emptyDescription')"
    />

    <div v-else-if="selectedQuestion" class="defense-session__workspace">
      <nav class="defense-session__rail" :aria-label="t('defense.session.questionMap')">
        <h3>{{ t('defense.session.questionMap') }}</h3>
        <ol>
          <li v-for="(question, index) in questions" :key="question.id">
            <button
              type="button"
              :class="{
                'is-selected': question.id === selectedQuestionId,
                'is-answered': Boolean(question.answer)
              }"
              :aria-current="question.id === selectedQuestionId ? 'step' : undefined"
              @click="selectedQuestionId = question.id"
            >
              <span>{{ index + 1 }}</span>
              <span>{{ questionState(question) }}</span>
            </button>
          </li>
        </ol>
      </nav>

      <DefenseQuestionCard
        :question="selectedQuestion"
        :index="selectedQuestionIndex"
        :total="questions.length"
        :submitting="submittingQuestionId === selectedQuestion.id"
        :draft="drafts[selectedQuestion.id] || ''"
        @update:draft="updateDraft(selectedQuestion.id, $event)"
        @submit="emit('submit', $event)"
      />

      <DefenseEvidencePanel
        :claims="selectedQuestion.relatedClaims"
        :evidence="selectedQuestion.relatedEvidence"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import DefenseEvidencePanel from '@/components/defense/DefenseEvidencePanel.vue'
import DefenseQuestionCard from '@/components/defense/DefenseQuestionCard.vue'
import type { DefenseQuestionResponse, DefenseSessionResponse } from '@/types/api'

const props = defineProps<{
  session: DefenseSessionResponse
  questions: DefenseQuestionResponse[]
  loading: boolean
  error?: string
  blockingError?: boolean
  submittingQuestionId?: number
}>()

const emit = defineEmits<{
  (event: 'submit', payload: { questionId: number; answerText: string }): void
  (event: 'retry'): void
}>()

const { t } = useI18n()
const selectedQuestionId = ref<number>()
const drafts = ref<Record<number, string>>({})

const answeredCount = computed(() => props.questions.filter((question) => question.answer).length)
const progressScale = computed(() => {
  if (!props.questions.length) {
    return 0
  }
  return answeredCount.value / props.questions.length
})
const selectedQuestion = computed(() =>
  props.questions.find((question) => question.id === selectedQuestionId.value) || props.questions[0]
)
const selectedQuestionIndex = computed(() =>
  Math.max(0, props.questions.findIndex((question) => question.id === selectedQuestion.value?.id))
)
const sessionStatusLabel = computed(() => {
  const keyMap: Record<string, string> = {
    CREATING: 'defense.status.creating',
    ACTIVE: 'defense.status.active',
    INSUFFICIENT_DATA: 'defense.status.insufficientData',
    COMPLETED: 'defense.status.completed'
  }
  return keyMap[props.session.status] ? t(keyMap[props.session.status]) : props.session.status
})

watch(
  () => props.questions.map((question) => `${question.id}:${Boolean(question.answer)}`).join('|'),
  () => {
    const selected = props.questions.find((question) => question.id === selectedQuestionId.value)
    const nextUnanswered = props.questions.find((question) => !question.answer)

    if (!selectedQuestionId.value || !selected) {
      selectedQuestionId.value = nextUnanswered?.id || props.questions[0]?.id
      return
    }

    if (selected.answer && nextUnanswered && selected.id !== nextUnanswered.id) {
      const remainingDrafts = { ...drafts.value }
      delete remainingDrafts[selected.id]
      drafts.value = remainingDrafts
      selectedQuestionId.value = nextUnanswered.id
    }
  },
  { immediate: true }
)

function questionState(question: DefenseQuestionResponse) {
  if (question.answer) {
    return t('defense.session.answered')
  }
  return question.id === selectedQuestionId.value
    ? t('defense.session.current')
    : t('defense.session.upcoming')
}

function updateDraft(questionId: number, value: string) {
  drafts.value = {
    ...drafts.value,
    [questionId]: value
  }
}
</script>

<style scoped>
.defense-session__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
}

.defense-session__title-line {
  display: flex;
  align-items: center;
  gap: 14px;
}

.defense-session__header h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: var(--pm-type-section-title);
  letter-spacing: -0.025em;
}

.defense-session__header p {
  max-width: 68ch;
  margin: 8px 0 0;
  color: var(--pm-muted);
  font-size: 14px;
  line-height: 1.65;
}

.defense-session__identity {
  display: grid;
  flex: 0 0 auto;
  justify-items: end;
  gap: 5px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  line-height: 1.4;
}

.defense-session__identity strong {
  color: var(--pm-ink);
  font-size: 11px;
  font-weight: 500;
}

.defense-session__progress {
  height: 3px;
  margin-top: 20px;
  overflow: hidden;
  background: var(--pm-stone);
}

.defense-session__progress span {
  display: block;
  width: 100%;
  height: 100%;
  background: var(--pm-primary);
  transform-origin: left;
  transition: transform var(--pm-motion-base) var(--pm-ease-standard);
}

.defense-session__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 18px;
  padding: 12px 0;
  border-top: 1px solid var(--pm-risk-border);
  border-bottom: 1px solid var(--pm-risk-border);
  color: var(--pm-risk);
  font-size: 13px;
}

.defense-session__loading,
.defense-session :deep(.empty-state),
.defense-session__workspace {
  margin-top: 28px;
}

.defense-session__workspace {
  display: grid;
  min-width: 0;
  grid-template-columns: 160px minmax(340px, 1fr) minmax(280px, 0.72fr);
  align-items: start;
  gap: 24px;
}

.defense-session__rail {
  position: sticky;
  top: 104px;
  min-width: 0;
  border-top: 1px solid var(--pm-stone-strong);
}

.defense-session__rail h3 {
  margin: 0;
  padding: 14px 0;
  border-bottom: 1px solid var(--pm-stone);
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.07em;
  text-transform: uppercase;
}

.defense-session__rail ol {
  margin: 0;
  padding: 0;
  list-style: none;
}

.defense-session__rail button {
  display: grid;
  width: 100%;
  min-height: 48px;
  grid-template-columns: 24px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 9px 8px;
  border: 0;
  border-bottom: 1px solid var(--pm-stone);
  background: transparent;
  color: var(--pm-muted);
  cursor: pointer;
  font-family: var(--pm-font-sans);
  font-size: 11px;
  text-align: left;
}

.defense-session__rail button > span:first-child {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 1px solid var(--pm-stone-strong);
  color: var(--pm-graphite);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.defense-session__rail button:hover,
.defense-session__rail button.is-selected {
  background: var(--pm-primary-soft);
  color: var(--pm-primary-dark);
}

.defense-session__rail button.is-selected > span:first-child {
  border-color: var(--pm-primary);
  background: var(--pm-primary);
  color: var(--pm-surface);
}

.defense-session__rail button.is-answered:not(.is-selected) > span:first-child {
  border-color: var(--pm-supported);
  color: var(--pm-supported);
}

@media (max-width: 1180px) {
  .defense-session__workspace {
    grid-template-columns: 150px minmax(0, 1fr);
  }

  .defense-session__workspace > :last-child {
    grid-column: 2;
  }
}

@media (max-width: 900px) {
  .defense-session__workspace {
    grid-template-columns: 1fr;
  }

  .defense-session__workspace > :last-child {
    grid-column: 1;
  }

  .defense-session__rail {
    position: static;
    overflow-x: auto;
  }

  .defense-session__rail ol {
    display: flex;
  }

  .defense-session__rail li {
    flex: 0 0 128px;
  }
}

@media (max-width: 520px) {
  .defense-session__header,
  .defense-session__title-line {
    align-items: flex-start;
    flex-direction: column;
  }

  .defense-session__identity {
    justify-items: start;
  }

  .defense-session__error {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .defense-session__progress span {
    transition: none;
  }
}
</style>
