<template>
  <section class="question-composer" :aria-busy="loading">
    <header class="composer-header">
      <div>
        <h3>{{ t('qa.v6.composerTitle') }}</h3>
        <p>{{ t('qa.v6.composerDescription') }}</p>
      </div>
      <dl class="project-context">
        <dt>{{ t('qa.v6.projectLabel') }}</dt>
        <dd>{{ projectName }}</dd>
      </dl>
    </header>

    <form class="question-form" @submit.prevent="emit('submit')">
      <label for="project-evidence-question">{{ t('qa.v6.questionLabel') }}</label>
      <div class="question-control">
        <el-input
          id="project-evidence-question"
          :model-value="modelValue"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 3 }"
          maxlength="1000"
          :placeholder="t('qa.v6.placeholder')"
          :disabled="disabled || loading"
          aria-describedby="question-method-note"
          @update:model-value="updateValue"
          @keydown="handleKeydown"
        />
        <el-button type="primary" native-type="submit" :loading="loading" :disabled="disabled || !modelValue.trim()">
          {{ t('qa.v6.ask') }}
        </el-button>
      </div>
      <div class="question-form-meta">
        <span>{{ t('qa.v6.askShortcut') }}</span>
        <span id="question-method-note">{{ t('qa.v6.costNote', { count: creditCost }) }}</span>
      </div>
    </form>

    <div class="question-examples" :aria-label="t('qa.v6.examplesLabel')">
      <span>{{ t('qa.v6.examplesLabel') }}</span>
      <div>
        <button v-for="example in suggestions" :key="example" type="button" :disabled="loading" @click="emit('select', example)">
          {{ example }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="question-loading" role="status" aria-live="polite">
      <span><i aria-hidden="true" />{{ t('qa.v6.loadingRetrieval') }}</span>
      <span><i aria-hidden="true" />{{ t('qa.v6.loadingAnalysis') }}</span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  modelValue: string
  projectName: string
  suggestions: string[]
  loading: boolean
  disabled: boolean
  creditCost: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  submit: []
  select: [value: string]
}>()

const { t } = useI18n()

function updateValue(value: string) {
  emit('update:modelValue', value)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
    event.preventDefault()
    emit('submit')
  }
}
</script>

<style scoped>
.question-composer {
  border-top: 1px solid var(--pm-ink);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.composer-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(190px, 0.32fr);
  gap: 36px;
  padding: 26px 0 22px;
}

.composer-header h3 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.015em;
}

.composer-header p {
  max-width: 68ch;
  margin: 8px 0 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.65;
}

.project-context {
  display: grid;
  align-content: start;
  gap: 7px;
  margin: 0;
  padding-left: 20px;
  border-left: 1px solid var(--pm-stone);
}

.project-context dt,
.question-form > label,
.question-examples > span,
.question-form-meta {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  line-height: 1.45;
  text-transform: uppercase;
}

.project-context dd {
  min-width: 0;
  margin: 0;
  color: var(--pm-ink);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.question-form {
  display: grid;
  gap: 9px;
  padding: 22px 0;
  border-top: 1px solid var(--pm-stone);
}

.question-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: stretch;
}

.question-control :deep(.el-textarea__inner) {
  min-height: 48px !important;
  padding: 12px 14px;
  border-radius: var(--pm-radius-sm);
  background: var(--pm-surface);
  color: var(--pm-ink);
  font-family: var(--pm-font-sans);
  font-size: 15px;
  line-height: 1.55;
  box-shadow: 0 0 0 1px var(--pm-stone-strong) inset;
  resize: none;
}

.question-control :deep(.el-textarea__inner::placeholder) {
  color: var(--pm-muted);
  opacity: 1;
}

.question-control :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--pm-primary) inset;
}

.question-control :deep(.el-button) {
  min-width: 92px;
  min-height: 48px;
}

.question-form-meta {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  text-transform: none;
}

.question-form-meta span:last-child {
  max-width: 72ch;
  text-align: right;
}

.question-examples {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
  padding: 18px 0 20px;
  border-top: 1px solid var(--pm-stone);
}

.question-examples > div {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.question-examples button {
  min-height: 44px;
  padding: 7px 0;
  border: 0;
  border-bottom: 1px solid var(--pm-stone-strong);
  background: transparent;
  color: var(--pm-primary-dark);
  cursor: pointer;
  font: 500 13px/1.5 var(--pm-font-sans);
  text-align: left;
  transition: border-color var(--pm-motion-fast) ease, color var(--pm-motion-fast) ease;
}

.question-examples button:hover {
  border-bottom-color: var(--pm-primary);
  color: var(--pm-ink);
}

.question-examples button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.question-loading {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 26px;
  padding: 13px 0;
  border-top: 1px solid var(--pm-stone);
  color: var(--pm-graphite);
  font-size: 13px;
}

.question-loading span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.question-loading i {
  width: 7px;
  height: 7px;
  background: var(--pm-primary);
}

@media (max-width: 720px) {
  .composer-header,
  .question-examples {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .project-context {
    padding: 14px 0 0;
    border-top: 1px solid var(--pm-stone);
    border-left: 0;
  }

  .question-form-meta {
    display: grid;
  }

  .question-form-meta span:last-child {
    text-align: left;
  }
}

@media (max-width: 520px) {
  .question-control {
    grid-template-columns: 1fr;
  }

  .question-control :deep(.el-button) {
    width: 100%;
  }

  .question-examples > div {
    display: grid;
  }

  .question-examples button {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .question-examples button {
    transition: none;
  }
}
</style>
