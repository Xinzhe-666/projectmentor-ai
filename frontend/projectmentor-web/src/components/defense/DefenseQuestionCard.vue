<template>
  <article class="defense-question" :aria-labelledby="questionHeadingId">
    <header class="defense-question__header">
      <div class="defense-question__meta">
        <span>{{ t('defense.session.questionLabel', { current: index + 1, total }) }}</span>
        <span>{{ t('defense.session.category', { category: formatCategory(question.category) }) }}</span>
      </div>
      <h2 :id="questionHeadingId">{{ question.question }}</h2>
    </header>

    <div v-if="question.answer" class="defense-question__submitted">
      <div class="defense-question__section-heading">
        <h3>{{ t('defense.session.submittedAnswer') }}</h3>
        <StatusLabel
          :status="question.answer.evaluationStatus"
          :label="alignmentLabel(question.answer.evaluationStatus)"
        />
      </div>
      <p class="defense-question__answer-text">{{ question.answer.answerText }}</p>
      <div class="defense-question__rule-review">
        <h3>{{ t('defense.session.ruleReview') }}</h3>
        <p>{{ question.answer.reviewResult.summary }}</p>
      </div>
    </div>

    <form v-else class="defense-question__form" @submit.prevent="handleSubmit">
      <label :for="answerId">{{ t('defense.session.answerLabel') }}</label>
      <el-input
        :id="answerId"
        v-model="draftModel"
        type="textarea"
        :rows="8"
        resize="vertical"
        :maxlength="10000"
        :placeholder="t('defense.session.answerPlaceholder')"
        :disabled="submitting"
      />
      <div class="defense-question__form-help">
        <p>{{ t('defense.session.answerHint') }}</p>
        <span aria-live="polite">{{ t('defense.session.characters', { count: draftModel.length }) }}</span>
      </div>
      <div class="defense-question__actions">
        <el-button
          native-type="submit"
          type="primary"
          :loading="submitting"
          :disabled="!draftModel.trim() || submitting"
        >
          {{ submitting ? t('defense.session.submitting') : t('defense.session.submit') }}
        </el-button>
      </div>
    </form>
  </article>
</template>

<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'

import StatusLabel from '@/components/StatusLabel.vue'
import type { DefenseEvidenceAlignment, DefenseQuestionResponse } from '@/types/api'

const props = defineProps<{
  question: DefenseQuestionResponse
  index: number
  total: number
  submitting: boolean
  draft: string
}>()

const emit = defineEmits<{
  (event: 'submit', payload: { questionId: number; answerText: string }): void
  (event: 'update:draft', value: string): void
}>()

const { t } = useI18n()
const answerId = `defense-answer-${useId()}`
const questionHeadingId = computed(() => `defense-question-${props.question.id}`)
const draftModel = computed({
  get: () => props.draft,
  set: (value: string) => emit('update:draft', value)
})

function handleSubmit() {
  const answerText = draftModel.value.trim()
  if (!answerText || props.submitting) {
    return
  }

  emit('submit', { questionId: props.question.id, answerText })
}

function formatCategory(value: string) {
  return value.replace(/_/g, ' ')
}

function alignmentLabel(value: DefenseEvidenceAlignment) {
  const keyMap: Record<DefenseEvidenceAlignment, string> = {
    SUPPORTED: 'defense.status.supported',
    PARTIAL: 'defense.status.partial',
    INSUFFICIENT: 'defense.status.insufficient'
  }
  return t(keyMap[value])
}
</script>

<style scoped>
.defense-question {
  min-width: 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.defense-question__header,
.defense-question__form,
.defense-question__submitted {
  padding: 24px;
}

.defense-question__header {
  border-bottom: 1px solid var(--pm-stone);
}

.defense-question__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px 18px;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  line-height: 1.5;
  text-transform: uppercase;
}

.defense-question h2 {
  max-width: 32ch;
  margin: 16px 0 0;
  color: var(--pm-ink);
  font-size: clamp(22px, 2.4vw, 30px);
  font-weight: 600;
  letter-spacing: -0.025em;
  line-height: 1.28;
}

.defense-question__form label,
.defense-question__section-heading h3,
.defense-question__rule-review h3 {
  display: block;
  margin: 0;
  color: var(--pm-ink);
  font-size: 13px;
  font-weight: 600;
}

.defense-question__form :deep(.el-textarea) {
  margin-top: 12px;
}

.defense-question__form :deep(.el-textarea__inner) {
  min-height: 184px !important;
  padding: 14px 16px;
  color: var(--pm-ink);
  font-family: var(--pm-font-sans);
  font-size: 14px;
  line-height: 1.7;
}

.defense-question__form-help {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-top: 10px;
}

.defense-question__form-help p {
  max-width: 68ch;
  margin: 0;
  color: var(--pm-muted);
  font-size: 12px;
  line-height: 1.6;
}

.defense-question__form-help span {
  flex: 0 0 auto;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.defense-question__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.defense-question__section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.defense-question__answer-text,
.defense-question__rule-review p {
  margin: 14px 0 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.defense-question__rule-review {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--pm-stone);
}

@media (max-width: 520px) {
  .defense-question {
    background: transparent;
  }

  .defense-question__header,
  .defense-question__form,
  .defense-question__submitted {
    padding: 20px 0;
  }

  .defense-question__form-help,
  .defense-question__section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .defense-question__actions,
  .defense-question__actions :deep(.el-button) {
    width: 100%;
  }
}
</style>
