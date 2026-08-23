<template>
  <section class="defense-overview" aria-labelledby="defense-overview-title">
    <header class="defense-overview__intro">
      <h2 id="defense-overview-title">{{ t('defense.overview.title') }}</h2>
      <p>{{ t('defense.overview.description') }}</p>
    </header>

    <div v-if="loading" class="defense-overview__loading" aria-live="polite">
      <el-skeleton :rows="6" animated />
    </div>

    <EmptyState
      v-else-if="error"
      :title="error"
      :description="errorDescription || t('defense.errors.contextRecovery')"
    >
      <el-button @click="emit('retry')">{{ t('defense.retry') }}</el-button>
      <el-button text @click="emit('open-project')">{{ t('defense.backToProject') }}</el-button>
    </EmptyState>

    <EmptyState
      v-else-if="!report"
      :title="t('defense.overview.noReport')"
      :description="t('defense.overview.noReportDescription')"
    >
      <el-button type="primary" @click="emit('open-project')">{{ t('defense.overview.openProject') }}</el-button>
    </EmptyState>

    <template v-else>
      <div class="defense-overview__body">
        <section class="defense-overview__record" aria-labelledby="defense-source-record-title">
          <h3 id="defense-source-record-title">{{ t('defense.overview.contextTitle') }}</h3>
          <dl>
            <div>
              <dt>{{ t('defense.overview.project') }}</dt>
              <dd>{{ project?.name || t('common.unnamedProject') }}</dd>
            </div>
            <div>
              <dt>{{ t('defense.overview.report') }}</dt>
              <dd>{{ t('defense.overview.latestReport', { id: report.reportId }) }}</dd>
            </div>
            <div>
              <dt>{{ t('defense.overview.reportDate') }}</dt>
              <dd>{{ formatDate(report.createTime) }}</dd>
            </div>
          </dl>
          <el-button type="primary" :loading="starting" @click="emit('start')">
            {{ starting ? t('defense.overview.starting') : t('defense.overview.start') }}
          </el-button>
        </section>

        <section class="defense-overview__method" aria-labelledby="defense-method-title">
          <h3 id="defense-method-title">{{ t('defense.overview.methodTitle') }}</h3>
          <ol>
            <li>{{ t('defense.overview.methodQuestions') }}</li>
            <li>{{ t('defense.overview.methodAnswers') }}</li>
            <li>{{ t('defense.overview.methodReview') }}</li>
          </ol>
        </section>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import EmptyState from '@/components/EmptyState.vue'
import type { Project, ReportListItem } from '@/types/api'

defineProps<{
  project?: Project
  report?: ReportListItem
  loading: boolean
  starting: boolean
  error?: string
  errorDescription?: string
}>()

const emit = defineEmits<{
  (event: 'start'): void
  (event: 'retry'): void
  (event: 'open-project'): void
}>()

const { t } = useI18n()

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped>
.defense-overview__intro {
  max-width: 820px;
}

.defense-overview__intro h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: clamp(30px, 4vw, 48px);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 1.08;
}

.defense-overview__intro p {
  max-width: 68ch;
  margin: 16px 0 0;
  color: var(--pm-graphite);
  font-size: 15px;
  line-height: 1.75;
}

.defense-overview__loading,
.defense-overview :deep(.empty-state),
.defense-overview__body {
  margin-top: 36px;
}

.defense-overview__loading {
  min-height: 320px;
  padding: 28px 0;
  border-top: 1px solid var(--pm-stone-strong);
}

.defense-overview__body {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.defense-overview__record,
.defense-overview__method {
  min-width: 0;
  padding: 28px;
}

.defense-overview__record {
  border-right: 1px solid var(--pm-stone);
}

.defense-overview__body h3 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 17px;
}

.defense-overview__record dl {
  margin: 20px 0 24px;
  border-top: 1px solid var(--pm-stone);
}

.defense-overview__record dl div {
  display: flex;
  min-width: 0;
  align-items: baseline;
  justify-content: space-between;
  gap: 20px;
  padding: 13px 0;
  border-bottom: 1px solid var(--pm-stone);
}

.defense-overview__record dt {
  color: var(--pm-muted);
  font-size: 12px;
}

.defense-overview__record dd {
  min-width: 0;
  margin: 0;
  color: var(--pm-ink);
  font-family: var(--pm-font-mono);
  font-size: 12px;
  overflow-wrap: anywhere;
  text-align: right;
}

.defense-overview__method ol {
  margin: 20px 0 0;
  padding: 0;
  counter-reset: defense-method;
  list-style: none;
}

.defense-overview__method li {
  position: relative;
  min-height: 52px;
  padding: 13px 0 13px 44px;
  border-top: 1px solid var(--pm-stone);
  color: var(--pm-graphite);
  font-size: 13px;
  line-height: 1.65;
  counter-increment: defense-method;
}

.defense-overview__method li::before {
  position: absolute;
  top: 12px;
  left: 0;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid var(--pm-stone-strong);
  color: var(--pm-primary-dark);
  content: counter(defense-method);
  font-family: var(--pm-font-mono);
  font-size: 10px;
}

@media (max-width: 900px) {
  .defense-overview__body {
    grid-template-columns: 1fr;
  }

  .defense-overview__record {
    border-right: 0;
    border-bottom: 1px solid var(--pm-stone);
  }
}

@media (max-width: 520px) {
  .defense-overview__body {
    background: transparent;
  }

  .defense-overview__record,
  .defense-overview__method {
    padding: 24px 0;
  }

  .defense-overview__record :deep(.el-button) {
    width: 100%;
  }
}
</style>
