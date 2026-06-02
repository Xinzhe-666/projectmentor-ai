<template>
  <section class="panel qa-panel pm-glass-card">
    <div class="panel-title">
      <div>
        <h3>{{ t('qa.title') }}</h3>
        <p class="muted">{{ t('qa.desc') }}</p>
      </div>
      <el-button :icon="Refresh" :loading="historyLoading" @click="loadHistory">{{ t('qa.refreshHistory') }}</el-button>
    </div>

    <div class="panel-body qa-body">
      <el-alert
        v-if="!hasProjectFiles"
        :title="t('qa.noFiles')"
        type="info"
        show-icon
        :closable="false"
      />

      <div class="qa-input-shell">
        <div class="qa-prompt-head">
          <div>
            <p class="eyebrow">{{ t('qa.askEyebrow') }}</p>
            <h4>{{ t('qa.askTitle') }}</h4>
            <p>{{ t('qa.askSubtitle') }}</p>
          </div>
        </div>

        <div class="quick-question-block">
          <div class="qa-record-subtitle">{{ t('qa.quickTitle') }}</div>
          <div class="quick-question-list">
            <el-button
              v-for="item in quickQuestions"
              :key="item"
              size="small"
              plain
              @click="fillQuestion(item)"
            >
              {{ item }}
            </el-button>
          </div>
        </div>

        <div class="qa-input-row">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :placeholder="t('qa.placeholder')"
          />
          <el-button type="primary" :icon="QuestionFilled" :loading="loading" :disabled="!hasProjectFiles" @click="handleAsk">
            {{ t('qa.ask') }}
          </el-button>
        </div>
      </div>

      <div v-if="currentAnswer" class="qa-current">
        <div class="qa-section-heading">{{ t('qa.current') }}</div>
        <article class="qa-record qa-record-current">
          <header class="qa-record-header">
            <div class="qa-record-main">
                <h4>{{ currentAnswer.question }}</h4>
                <div class="qa-record-meta">
                  <el-tag :type="currentAnswer.aiUsed ? 'success' : 'warning'" effect="light">
                    {{ currentAnswer.aiUsed ? t('qa.aiUsed') : t('qa.ruleOnly') }}
                  </el-tag>
                  <el-tag :type="evidenceTagType(currentAnswer)" effect="light">
                    {{ effectiveEvidenceLevelText(currentAnswer) }}
                  </el-tag>
                  <span>{{ t('qa.confidence', { score: formatConfidence(currentAnswer) }) }}</span>
                  <span>{{ formatDate(currentAnswer.createTime) }}</span>
                  <span>{{ t('qa.evidenceCount', { count: evidenceCount(currentAnswer) }) }}</span>
                </div>
              </div>
            <div class="qa-record-actions">
              <el-button class="qa-action-button" :icon="CopyDocument" @click="copyQaRecord(currentAnswer)">{{ t('qa.copyAnswer') }}</el-button>
              <el-button class="qa-action-button" :icon="CopyDocument" type="primary" plain @click="copyInterviewRecord(currentAnswer)">{{ t('qa.copyInterview') }}</el-button>
            </div>
          </header>

          <el-alert
            v-if="!currentAnswer.aiUsed"
            :title="t('qa.aiUnavailable')"
            type="warning"
            show-icon
            :closable="false"
          />

          <ConfidenceCard :record="currentAnswer" />

          <el-alert
            v-if="isWeakEvidence(currentAnswer)"
            :title="t('qa.weakEvidence')"
            type="warning"
            show-icon
            :closable="false"
          />

          <MarkdownBlock :content="currentAnswer.answer" />

          <div class="qa-insight-grid">
            <section class="qa-insight-block">
              <div class="qa-record-subtitle">{{ t('qa.interviewAnswer') }}</div>
              <p>{{ displayInterviewAnswer(currentAnswer) }}</p>
            </section>
            <section class="qa-insight-block">
              <div class="qa-record-subtitle">{{ t('qa.resumeRisk') }}</div>
              <p>{{ displayResumeRisk(currentAnswer) }}</p>
            </section>
          </div>

          <QaEvidenceList :record="currentAnswer" />

          <QaFollowUps :record="currentAnswer" />
        </article>
      </div>

      <div class="qa-history">
        <div class="qa-history-title">
          <div>
            <div class="qa-section-heading">{{ t('qa.recent') }}</div>
            <p class="muted">{{ t('qa.recentDesc') }}</p>
          </div>
        </div>

        <div v-loading="historyLoading" class="qa-history-list">
          <EmptyState
            v-if="!historyLoading && visibleHistory.length === 0"
            :title="t('qa.emptyTitle')"
          />

          <article v-for="record in visibleHistory" :key="record.id" class="qa-record">
            <header class="qa-record-header">
              <div class="qa-record-main">
                <h4>{{ record.question }}</h4>
                <div class="qa-record-meta">
                  <el-tag :type="record.aiUsed ? 'success' : 'warning'" effect="light">
                    {{ record.aiUsed ? t('qa.aiUsed') : t('qa.ruleOnly') }}
                  </el-tag>
                  <el-tag :type="evidenceTagType(record)" effect="light">
                    {{ effectiveEvidenceLevelText(record) }}
                  </el-tag>
                  <span>{{ t('qa.confidence', { score: formatConfidence(record) }) }}</span>
                  <span>{{ formatDate(record.createTime) }}</span>
                  <span>{{ t('qa.evidenceCount', { count: evidenceCount(record) }) }}</span>
                </div>
              </div>
              <div class="qa-record-actions">
                <el-button class="qa-action-button" :icon="CopyDocument" @click="copyQaRecord(record)">{{ t('qa.copyAnswer') }}</el-button>
                <el-button class="qa-action-button" :icon="CopyDocument" type="primary" plain @click="copyInterviewRecord(record)">{{ t('qa.copyInterview') }}</el-button>
                <el-button class="qa-action-button" :icon="DeleteIcon" type="danger" plain @click="handleDelete(record)">{{ t('qa.deleteRecord') }}</el-button>
              </div>
            </header>

            <details class="qa-record-details">
              <summary>{{ t('qa.detailsSummary') }}</summary>

              <div class="qa-record-detail-body">
                <el-alert
                  v-if="!record.aiUsed"
                  :title="t('qa.aiUnavailable')"
                  type="warning"
                  show-icon
                  :closable="false"
                />

                <ConfidenceCard :record="record" />

                <el-alert
                  v-if="isWeakEvidence(record)"
                  :title="t('qa.weakEvidence')"
                  type="warning"
                  show-icon
                  :closable="false"
                />

                <MarkdownBlock :content="record.answer" />

                <div class="qa-insight-grid">
                  <section class="qa-insight-block">
                    <div class="qa-record-subtitle">{{ t('qa.interviewAnswer') }}</div>
                    <p>{{ displayInterviewAnswer(record) }}</p>
                  </section>
                  <section class="qa-insight-block">
                    <div class="qa-record-subtitle">{{ t('qa.resumeRisk') }}</div>
                    <p>{{ displayResumeRisk(record) }}</p>
                  </section>
                </div>

                <QaEvidenceList :record="record" />

                <QaFollowUps :record="record" />
              </div>
            </details>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { CopyDocument, Delete as DeleteIcon, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElTag } from 'element-plus'

import { askProjectQuestion, deleteProjectQaRecord, getProjectQaHistory } from '@/api/projectQa'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import type { ProjectQaHistoryRecord, ProjectQaResponse } from '@/types/api'

type QaDisplayRecord = ProjectQaResponse & {
  id?: number
  createTime?: string
}

const props = defineProps<{
  projectId: number
  hasProjectFiles: boolean
}>()

const { t } = useI18n()

const quickQuestions = computed(() => [
  t('qa.quickQuestions.auth'),
  t('qa.quickQuestions.jwt'),
  t('qa.quickQuestions.redis'),
  t('qa.quickQuestions.zip'),
  t('qa.quickQuestions.report'),
  t('qa.quickQuestions.fallback'),
  t('qa.quickQuestions.resume'),
  t('qa.quickQuestions.interview')
])

const question = ref('')
const loading = ref(false)
const historyLoading = ref(false)
const currentAnswer = ref<QaDisplayRecord>()
const historyRecords = ref<ProjectQaHistoryRecord[]>([])
const expandedSnippets = ref(new Set<string>())

const visibleHistory = computed(() => {
  const currentId = currentAnswer.value?.id
  if (!currentId) {
    return historyRecords.value
  }

  return historyRecords.value.filter((record) => record.id !== currentId)
})

const ConfidenceCard = defineComponent({
  name: 'QaConfidenceCard',
  props: {
    record: {
      type: Object as PropType<QaDisplayRecord>,
      required: true
    }
  },
  setup(componentProps) {
    return () => h('section', { class: ['qa-confidence-card', `qa-confidence-${effectiveEvidenceLevel(componentProps.record).toLowerCase()}`] }, [
      h('div', { class: 'qa-confidence-main' }, [
        h('span', { class: 'qa-confidence-label' }, t('qa.evidenceTrust')),
        h(ElTag, { type: evidenceTagType(componentProps.record), effect: 'light' }, () => effectiveEvidenceLevelText(componentProps.record))
      ]),
      h('strong', { class: 'qa-confidence-score' }, formatConfidence(componentProps.record)),
      h('p', null, effectiveEvidenceSummary(componentProps.record))
    ])
  }
})

const QaEvidenceList = defineComponent({
  name: 'QaEvidenceList',
  props: {
    record: {
      type: Object as PropType<QaDisplayRecord>,
      required: true
    }
  },
  setup(componentProps) {
    return () => h('div', { class: 'qa-evidence-list' }, [
      h('div', { class: 'qa-record-subtitle' }, t('common.evidence')),
      evidenceCount(componentProps.record)
        ? h('div', { class: 'qa-evidence-grid' }, safeEvidences(componentProps.record).map((evidence, evidenceIndex) => {
          const key = evidenceKey(componentProps.record, evidenceIndex)
          return h('article', { key, class: 'qa-evidence-item' }, [
            h('div', { class: 'qa-evidence-header' }, [
              h('div', { class: 'qa-file-path' }, evidence.filePath || '-'),
              h('button', {
                class: 'qa-copy-path-button',
                type: 'button',
                onClick: () => copyFilePath(evidence.filePath || '')
              }, t('qa.filePath'))
            ]),
            h('p', { class: 'qa-reason' }, [
              h('span', { class: 'qa-reason-label' }, t('qa.reason')),
              h('span', null, evidence.reason || '-')
            ]),
            h('pre', { class: 'qa-snippet' }, visibleSnippet(evidence.snippet, key)),
            hasSnippetOverflow(evidence.snippet)
              ? h('button', {
                class: 'snippet-toggle qa-text-button',
                type: 'button',
                onClick: () => toggleSnippet(key)
              }, isSnippetExpanded(key) ? t('qa.collapse') : t('qa.expand'))
              : null
          ])
        }))
        : h(EmptyState, {
          title: t('qa.noEvidenceTitle'),
          description: t('qa.noEvidenceDesc')
        })
    ])
  }
})

const QaFollowUps = defineComponent({
  name: 'QaFollowUps',
  props: {
    record: {
      type: Object as PropType<QaDisplayRecord>,
      required: true
    }
  },
  setup(componentProps) {
    return () => safeFollowUps(componentProps.record).length
      ? h('div', { class: 'qa-follow-ups' }, [
        h('div', { class: 'qa-record-subtitle' }, t('qa.followUps')),
        h('div', { class: 'follow-up-list' }, safeFollowUps(componentProps.record).map((followUp) => h('button', {
          key: followUp,
          class: 'qa-follow-up-button',
          type: 'button',
          onClick: () => fillQuestion(followUp)
        }, followUp)))
      ])
      : null
  }
})

function fillQuestion(value: string) {
  question.value = value
}

async function loadHistory() {
  historyLoading.value = true
  try {
    historyRecords.value = await getProjectQaHistory(props.projectId)
  } finally {
    historyLoading.value = false
  }
}

async function handleAsk() {
  const trimmedQuestion = question.value.trim()
  if (!trimmedQuestion) {
    ElMessage.warning(t('qa.questionRequired'))
    return
  }

  loading.value = true
  try {
    const response = await askProjectQuestion(props.projectId, trimmedQuestion)
    currentAnswer.value = { ...response }
    question.value = ''
    await loadHistory()

    const latestRecord = historyRecords.value[0]
    if (latestRecord?.question === response.question && latestRecord?.answer === response.answer) {
      currentAnswer.value = latestRecord
    }
  } finally {
    loading.value = false
  }
}

async function handleDelete(record: ProjectQaHistoryRecord) {
  try {
    await ElMessageBox.confirm(t('qa.deleteConfirm'), t('qa.deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel')
    })
  } catch {
    return
  }

  await deleteProjectQaRecord(props.projectId, record.id)
  historyRecords.value = historyRecords.value.filter((item) => item.id !== record.id)
  if (currentAnswer.value?.id === record.id) {
    currentAnswer.value = undefined
  }
  ElMessage.success(t('qa.deleted'))
}

async function copyQaRecord(record: QaDisplayRecord) {
  const text = buildCopyText(record)
  const copied = await copyText(text)

  if (copied) {
    ElMessage.success(t('qa.answerCopied'))
    return
  }

  ElMessage.warning(t('qa.copyFailed'))
}

async function copyInterviewRecord(record: QaDisplayRecord) {
  const copied = await copyText(buildInterviewCopyText(record))

  if (copied) {
    ElMessage.success(t('qa.interviewCopied'))
    return
  }

  ElMessage.warning(t('qa.interviewCopyFailed'))
}

async function copyFilePath(filePath: string) {
  if (!filePath) {
    ElMessage.warning(t('qa.noPath'))
    return
  }

  const copied = await copyText(filePath)
  if (copied) {
    ElMessage.success(t('qa.pathCopied'))
    return
  }

  ElMessage.warning(t('qa.pathCopyFailed'))
}

function buildCopyText(record: QaDisplayRecord) {
  return [
    `${t('qa.copyTemplate.question')}${record.question || '-'}`,
    '',
    t('qa.copyTemplate.answer'),
    record.answer || '-',
    '',
    t('qa.copyTemplate.confidence'),
    `${effectiveEvidenceLevelText(record)}（${formatConfidence(record)}）`,
    effectiveEvidenceSummary(record),
    '',
    t('qa.copyTemplate.paths'),
    evidencePathsText(record),
    '',
    t('qa.copyTemplate.followUps'),
    followUpsText(record)
  ].join('\n')
}

function buildInterviewCopyText(record: QaDisplayRecord) {
  return [
    `${t('qa.copyTemplate.question')}${record.question || '-'}`,
    '',
    t('qa.copyTemplate.confidence'),
    `${effectiveEvidenceLevelText(record)}（${formatConfidence(record)}）`,
    effectiveEvidenceSummary(record),
    '',
    t('qa.copyTemplate.interviewAnswer'),
    displayInterviewAnswer(record),
    '',
    t('qa.copyTemplate.resumeRisk'),
    displayResumeRisk(record),
    '',
    t('qa.copyTemplate.keyPaths'),
    evidencePathsText(record),
    '',
    t('qa.copyTemplate.followUps'),
    followUpsText(record)
  ].join('\n')
}

function evidencePathsText(record: QaDisplayRecord) {
  const evidences = safeEvidences(record)
  return evidences.length
    ? evidences.map((evidence, index) => `${index + 1}. ${evidence.filePath || '-'}`).join('\n')
    : t('common.none')
}

function followUpsText(record: QaDisplayRecord) {
  const followUps = safeFollowUps(record)
  return followUps.length
    ? followUps.map((item, index) => `${index + 1}. ${item}`).join('\n')
    : t('common.none')
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // HTTP 环境或浏览器权限限制时，继续使用 textarea fallback。
  }

  return fallbackCopyText(text)
}

function fallbackCopyText(text: string) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  textarea.style.left = '-9999px'
  textarea.style.opacity = '0'

  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  let copied = false
  try {
    copied = document.execCommand('copy')
  } catch {
    copied = false
  } finally {
    document.body.removeChild(textarea)
  }

  return copied
}

function formatDate(value?: string) {
  if (!value) {
    return t('common.justNow')
  }

  return String(value).replace('T', ' ').slice(0, 19)
}

function safeEvidences(record: QaDisplayRecord) {
  return record.evidences || []
}

function safeFollowUps(record: QaDisplayRecord) {
  return record.suggestedFollowUps || []
}

function evidenceCount(record: QaDisplayRecord) {
  return safeEvidences(record).length
}

function effectiveEvidenceLevel(record: QaDisplayRecord) {
  if (record.evidenceLevel) {
    return record.evidenceLevel
  }

  const count = evidenceCount(record)
  if (count <= 0) {
    return 'NONE'
  }
  if (count === 1) {
    return 'WEAK'
  }
  if (count <= 2) {
    return 'MEDIUM'
  }
  return 'STRONG'
}

function effectiveEvidenceLevelText(record: QaDisplayRecord) {
  if (record.evidenceLevelText) {
    return record.evidenceLevelText
  }

  const levelMap: Record<string, string> = {
    STRONG: t('qa.evidenceLevels.STRONG'),
    MEDIUM: t('qa.evidenceLevels.MEDIUM'),
    WEAK: t('qa.evidenceLevels.WEAK'),
    NONE: t('qa.evidenceLevels.NONE')
  }
  return levelMap[effectiveEvidenceLevel(record)] || t('qa.evidenceLevels.NONE')
}

function formatConfidence(record: QaDisplayRecord) {
  return typeof record.confidenceScore === 'number' ? `${record.confidenceScore}/100` : '-'
}

function effectiveEvidenceSummary(record: QaDisplayRecord) {
  if (record.evidenceSummary) {
    return record.evidenceSummary
  }

  const count = evidenceCount(record)
  if (count === 0) {
    return t('qa.noRelatedEvidence')
  }
  return t('qa.fallbackEvidenceSummary', { count })
}

function displayInterviewAnswer(record: QaDisplayRecord) {
  return record.interviewAnswer || t('qa.noInterviewAnswer')
}

function displayResumeRisk(record: QaDisplayRecord) {
  return record.resumeRisk || t('qa.noResumeRisk')
}

function evidenceTagType(record: QaDisplayRecord): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  const level = effectiveEvidenceLevel(record)
  if (level === 'STRONG') {
    return 'success'
  }
  if (level === 'MEDIUM') {
    return 'primary'
  }
  if (level === 'WEAK') {
    return 'warning'
  }
  return 'danger'
}

function isWeakEvidence(record: QaDisplayRecord) {
  const level = effectiveEvidenceLevel(record)
  return level === 'WEAK' || level === 'NONE'
}

function evidenceKey(record: QaDisplayRecord, evidenceIndex: number) {
  return `${record.id ?? 'current'}-${evidenceIndex}-${safeEvidences(record)[evidenceIndex]?.filePath || 'evidence'}`
}

function splitSnippet(snippet?: string) {
  return (snippet || '').split(/\r?\n/)
}

function hasSnippetOverflow(snippet?: string) {
  return splitSnippet(snippet).length > 5 || (snippet || '').length > 420
}

function isSnippetExpanded(key: string) {
  return expandedSnippets.value.has(key)
}

function toggleSnippet(key: string) {
  const nextExpanded = new Set(expandedSnippets.value)
  if (nextExpanded.has(key)) {
    nextExpanded.delete(key)
  } else {
    nextExpanded.add(key)
  }
  expandedSnippets.value = nextExpanded
}

function visibleSnippet(snippet: string | undefined, key: string) {
  const normalized = snippet || ''
  if (!hasSnippetOverflow(normalized) || isSnippetExpanded(key)) {
    return normalized || '-'
  }

  const preview = splitSnippet(normalized).slice(0, 5).join('\n')
  const clipped = preview.length > 420 ? `${preview.slice(0, 420)}...` : preview
  return clipped.endsWith('...') ? clipped : `${clipped}\n...`
}

onMounted(loadHistory)
</script>

<style>
.qa-body,
.qa-current,
.qa-history,
.qa-history-list,
.qa-record,
.qa-record-detail-body,
.qa-follow-ups,
.qa-evidence-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.qa-panel {
  overflow: hidden;
}

.qa-panel > .panel-title {
  background:
    radial-gradient(circle at 16% 0%, rgba(20, 184, 166, 0.14), transparent 34%),
    radial-gradient(circle at 92% 0%, rgba(31, 111, 235, 0.14), transparent 36%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.76)),
    rgba(255, 255, 255, 0.92);
}

.qa-input-shell {
  display: grid;
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(31, 111, 235, 0.14);
  border-radius: 8px;
  background:
    radial-gradient(circle at 4% 0%, rgba(20, 184, 166, 0.12), transparent 34%),
    radial-gradient(circle at 88% 0%, rgba(31, 111, 235, 0.12), transparent 36%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(248, 251, 255, 0.78));
  box-shadow: 0 18px 46px rgba(31, 111, 235, 0.09);
}

.qa-prompt-head h4 {
  margin: 8px 0 8px;
  color: #111827;
  font-size: 22px;
  line-height: 1.25;
}

.qa-prompt-head p:last-child {
  max-width: 780px;
  margin: 0;
  color: var(--pm-muted);
  line-height: 1.75;
}

.quick-question-block {
  display: grid;
  gap: 10px;
}

.quick-question-list,
.follow-up-list,
.qa-record-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-question-list .el-button,
.follow-up-list .el-button,
.qa-follow-up-button {
  margin-left: 0;
  max-width: 100%;
  white-space: normal;
  height: auto;
  min-height: 32px;
  line-height: 1.4;
}

.quick-question-list .el-button {
  border-color: rgba(31, 111, 235, 0.16);
  background: rgba(255, 255, 255, 0.82);
  color: #245089;
  font-weight: 700;
  box-shadow: 0 8px 18px rgba(28, 43, 68, 0.04);
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.quick-question-list .el-button:hover {
  border-color: rgba(31, 111, 235, 0.34);
  box-shadow: 0 10px 24px rgba(31, 111, 235, 0.1);
  transform: translateY(-2px);
}

.qa-follow-up-button,
.qa-text-button {
  border: 1px solid rgba(64, 158, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--pm-primary);
  cursor: pointer;
  font: inherit;
  padding: 7px 12px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.qa-follow-up-button:hover,
.qa-copy-path-button:hover {
  border-color: rgba(31, 111, 235, 0.34);
  box-shadow: 0 10px 22px rgba(31, 111, 235, 0.1);
  transform: translateY(-1px);
}

.qa-text-button {
  border: 0;
  padding-left: 0;
}

.qa-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 12px;
}

.qa-input-row :deep(.el-textarea__inner) {
  border-color: rgba(31, 111, 235, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
  line-height: 1.7;
}

.qa-section-heading {
  color: #344054;
  font-size: 16px;
  font-weight: 800;
}

.qa-history-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.qa-history-title p {
  margin: 6px 0 0;
}

.qa-record {
  min-width: 0;
  padding: 18px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.86)),
    #ffffff;
  box-shadow: 0 16px 38px rgba(28, 43, 68, 0.07);
}

.qa-record-current {
  border-color: rgba(64, 158, 255, 0.35);
  background:
    linear-gradient(145deg, rgba(248, 251, 255, 0.98), rgba(240, 251, 249, 0.72)),
    #fbfdff;
}

.qa-record-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.qa-record-main {
  min-width: 0;
}

.qa-record-main h4 {
  margin: 0 0 10px;
  color: #101828;
  font-size: 17px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.qa-record-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: var(--pm-muted);
  font-size: 13px;
}

.qa-record-actions .el-button {
  margin-left: 0;
}

.qa-action-button {
  border-radius: 8px;
  font-weight: 700;
}

.qa-record-details {
  border-top: 1px solid rgba(223, 230, 240, 0.85);
  padding-top: 12px;
}

.qa-record-details summary {
  color: var(--pm-primary);
  cursor: pointer;
  font-weight: 700;
  line-height: 1.5;
}

.qa-record-detail-body {
  margin-top: 14px;
}

.qa-record-subtitle {
  color: #344054;
  font-weight: 800;
}

.qa-confidence-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 14px;
  padding: 16px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background:
    radial-gradient(circle at 96% 0%, rgba(31, 111, 235, 0.1), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.84)),
    #fbfdff;
  box-shadow:
    0 12px 30px rgba(28, 43, 68, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.qa-confidence-main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.qa-confidence-label {
  color: #344054;
  font-weight: 800;
}

.qa-confidence-score {
  color: #101828;
  font-size: 18px;
  text-align: right;
}

.qa-confidence-card p {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--pm-muted);
  line-height: 1.7;
}

.qa-confidence-strong {
  border-color: rgba(103, 194, 58, 0.36);
  background:
    radial-gradient(circle at 96% 0%, rgba(16, 185, 129, 0.12), transparent 34%),
    rgba(255, 255, 255, 0.9);
}

.qa-confidence-medium {
  border-color: rgba(64, 158, 255, 0.34);
}

.qa-confidence-weak {
  border-color: rgba(230, 162, 60, 0.36);
  background:
    radial-gradient(circle at 96% 0%, rgba(245, 158, 11, 0.12), transparent 34%),
    rgba(255, 255, 255, 0.9);
}

.qa-confidence-none {
  border-color: rgba(245, 108, 108, 0.34);
  background:
    radial-gradient(circle at 96% 0%, rgba(239, 68, 68, 0.1), transparent 34%),
    rgba(255, 255, 255, 0.9);
}

.qa-insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.qa-insight-block {
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.9), rgba(248, 251, 255, 0.7)),
    rgba(255, 255, 255, 0.78);
}

.qa-insight-block p {
  margin: 8px 0 0;
  color: #344054;
  line-height: 1.75;
}

.qa-evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.qa-evidence-item {
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.76)),
    rgba(255, 255, 255, 0.86);
  box-shadow: 0 12px 28px rgba(28, 43, 68, 0.05);
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.qa-evidence-item:hover {
  border-color: rgba(31, 111, 235, 0.24);
  box-shadow: 0 14px 30px rgba(31, 111, 235, 0.09);
  transform: translateY(-2px);
}

.qa-evidence-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.qa-file-path {
  color: var(--pm-primary);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.qa-copy-path-button {
  border: 1px solid rgba(64, 158, 255, 0.25);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--pm-primary);
  cursor: pointer;
  flex-shrink: 0;
  font: inherit;
  font-size: 12px;
  font-weight: 800;
  padding: 5px 8px;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.qa-reason {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin: 0 0 12px;
  color: #344054;
  border-left: 3px solid rgba(64, 158, 255, 0.35);
  padding-left: 10px;
  line-height: 1.6;
}

.qa-reason-label {
  color: #101828;
  font-size: 13px;
  font-weight: 800;
}

.qa-snippet {
  max-height: 220px;
  margin: 0;
  overflow: auto;
  padding: 12px;
  border-radius: 8px;
  background: #111827;
  color: #e5edf7;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.snippet-toggle {
  margin-top: 8px;
}

@media (max-width: 860px) {
  .qa-evidence-grid,
  .qa-input-row,
  .qa-insight-grid,
  .qa-confidence-card {
    grid-template-columns: 1fr;
  }

  .qa-input-row .el-button,
  .qa-record-header,
  .qa-record-actions {
    width: 100%;
  }

  .qa-record-header {
    flex-direction: column;
  }

  .qa-record-actions .el-button {
    flex: 1;
  }

  .qa-confidence-score {
    text-align: left;
  }

  .qa-evidence-header {
    grid-template-columns: 1fr;
  }

  .qa-copy-path-button {
    width: fit-content;
  }
}
</style>
