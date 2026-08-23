<template>
  <section class="defense-review" aria-labelledby="defense-review-title">
    <header class="defense-review__header">
      <div>
        <div class="defense-review__title-line">
          <h2 id="defense-review-title">{{ t('defense.review.title') }}</h2>
          <StatusLabel status="COMPLETED" :label="t('defense.review.complete')" />
        </div>
        <p>{{ t('defense.review.description') }}</p>
      </div>
      <span class="defense-review__session">{{ t('defense.session.sessionId', { id: review.session.id }) }}</span>
    </header>

    <dl class="defense-review__summary">
      <div>
        <dt>{{ t('defense.review.answered') }}</dt>
        <dd>{{ review.answeredCount }}</dd>
      </div>
      <div>
        <dt>{{ t('defense.review.supported') }}</dt>
        <dd>{{ review.supportedCount }}</dd>
      </div>
      <div>
        <dt>{{ t('defense.review.partial') }}</dt>
        <dd>{{ review.partialCount }}</dd>
      </div>
      <div>
        <dt>{{ t('defense.review.insufficient') }}</dt>
        <dd>{{ review.insufficientCount }}</dd>
      </div>
    </dl>

    <div class="defense-review__questions">
      <article v-for="(question, index) in review.questions" :key="question.id">
        <header class="defense-review__question-header">
          <div>
            <span>{{ t('defense.review.question', { number: index + 1 }) }}</span>
            <h3>{{ question.question }}</h3>
          </div>
          <StatusLabel
            v-if="question.answer"
            :status="question.answer.evaluationStatus"
            :label="alignmentLabel(question.answer.evaluationStatus)"
          />
        </header>

        <div v-if="question.answer" class="defense-review__question-body">
          <section>
            <h4>{{ t('defense.review.answer') }}</h4>
            <p>{{ question.answer.answerText }}</p>
            <div class="defense-review__finding">
              <h4>{{ t('defense.review.finding') }}</h4>
              <p>{{ question.answer.reviewResult.summary }}</p>
            </div>
          </section>
          <DefenseEvidencePanel
            :claims="question.answer.reviewResult.relatedClaims"
            :evidence="question.answer.reviewResult.matchedEvidence"
            mode="review"
          />
        </div>
      </article>
    </div>

    <footer class="defense-review__actions">
      <el-button @click="emit('open-project')">{{ t('defense.backToProject') }}</el-button>
      <el-button type="primary" @click="emit('start-again')">{{ t('defense.review.startAgain') }}</el-button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import StatusLabel from '@/components/StatusLabel.vue'
import DefenseEvidencePanel from '@/components/defense/DefenseEvidencePanel.vue'
import type { DefenseEvidenceAlignment, DefenseSessionReviewResponse } from '@/types/api'

defineProps<{
  review: DefenseSessionReviewResponse
}>()

const emit = defineEmits<{
  (event: 'start-again'): void
  (event: 'open-project'): void
}>()

const { t } = useI18n()

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
.defense-review__header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
}

.defense-review__title-line {
  display: flex;
  align-items: center;
  gap: 14px;
}

.defense-review__header h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: clamp(28px, 3.6vw, 42px);
  letter-spacing: -0.035em;
}

.defense-review__header p {
  max-width: 68ch;
  margin: 10px 0 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.7;
}

.defense-review__session {
  flex: 0 0 auto;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

.defense-review__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 28px 0 0;
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  border-left: 1px solid var(--pm-stone);
  background: var(--pm-surface);
}

.defense-review__summary div {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-right: 1px solid var(--pm-stone);
}

.defense-review__summary dt {
  color: var(--pm-muted);
  font-size: 11px;
}

.defense-review__summary dd {
  margin: 0;
  color: var(--pm-ink);
  font-family: var(--pm-font-mono);
  font-size: 26px;
  font-variant-numeric: tabular-nums;
}

.defense-review__questions {
  margin-top: 36px;
  border-top: 1px solid var(--pm-stone-strong);
}

.defense-review__questions > article {
  padding: 28px 0;
  border-bottom: 1px solid var(--pm-stone-strong);
}

.defense-review__question-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.defense-review__question-header span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.defense-review__question-header h3 {
  max-width: 62ch;
  margin: 8px 0 0;
  color: var(--pm-ink);
  font-size: 18px;
  line-height: 1.45;
}

.defense-review__question-body {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 28px;
  margin-top: 22px;
}

.defense-review__question-body > section {
  min-width: 0;
  padding-top: 18px;
  border-top: 1px solid var(--pm-stone);
}

.defense-review__question-body h4 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 12px;
}

.defense-review__question-body p {
  margin: 10px 0 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
}

.defense-review__finding {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid var(--pm-stone);
}

.defense-review__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .defense-review__question-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .defense-review__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .defense-review__summary div:nth-child(-n + 2) {
    border-bottom: 1px solid var(--pm-stone);
  }
}

@media (max-width: 520px) {
  .defense-review__header,
  .defense-review__title-line,
  .defense-review__question-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .defense-review__summary div {
    padding: 14px;
  }

  .defense-review__actions {
    flex-direction: column-reverse;
  }

  .defense-review__actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
