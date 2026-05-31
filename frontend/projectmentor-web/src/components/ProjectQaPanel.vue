<template>
  <section class="panel qa-panel">
    <div class="panel-title">
      <div>
        <h3>项目问答</h3>
        <p class="muted">基于当前项目已上传的 README 和代码文件进行轻量检索增强问答。回答会尽量附带文件证据；如果证据不足，系统会明确提示。</p>
      </div>
      <el-button :icon="Refresh" :loading="historyLoading" @click="loadHistory">刷新历史</el-button>
    </div>

    <div class="panel-body qa-body">
      <el-alert
        v-if="!hasProjectFiles"
        title="请先保存 README 或上传项目 ZIP，再使用项目问答。"
        type="info"
        show-icon
        :closable="false"
      />

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

      <div class="qa-input-row">
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="例如：JWT 在哪里实现？"
        />
        <el-button type="primary" :icon="QuestionFilled" :loading="loading" :disabled="!hasProjectFiles" @click="handleAsk">
          提问
        </el-button>
      </div>

      <div v-if="currentAnswer" class="qa-current">
        <div class="qa-section-heading">本次回答</div>
        <article class="qa-record qa-record-current">
          <header class="qa-record-header">
            <div class="qa-record-main">
              <h4>{{ currentAnswer.question }}</h4>
              <div class="qa-record-meta">
                <el-tag :type="currentAnswer.aiUsed ? 'success' : 'warning'" effect="light">
                  {{ currentAnswer.aiUsed ? 'AI 已参与回答' : '规则检索结果' }}
                </el-tag>
                <el-tag :type="evidenceTagType(currentAnswer)" effect="light">
                  {{ effectiveEvidenceLevelText(currentAnswer) }}
                </el-tag>
                <span>可信度 {{ formatConfidence(currentAnswer) }}</span>
                <span>{{ formatDate(currentAnswer.createTime) }}</span>
                <span>证据 {{ evidenceCount(currentAnswer) }} 条</span>
              </div>
            </div>
            <div class="qa-record-actions">
              <el-button :icon="CopyDocument" @click="copyQaRecord(currentAnswer)">复制回答</el-button>
              <el-button :icon="CopyDocument" type="primary" plain @click="copyInterviewRecord(currentAnswer)">复制面试版回答</el-button>
            </div>
          </header>

          <el-alert
            v-if="!currentAnswer.aiUsed"
            title="AI 当前不可用或未启用，本回答仅基于关键词检索证据，不代表完整结论。"
            type="warning"
            show-icon
            :closable="false"
          />

          <ConfidenceCard :record="currentAnswer" />

          <el-alert
            v-if="isWeakEvidence(currentAnswer)"
            title="这个回答证据较弱，建议补充 README、上传更多代码文件，或把问题问得更具体。"
            type="warning"
            show-icon
            :closable="false"
          />

          <MarkdownBlock :content="currentAnswer.answer" />

          <div class="qa-insight-grid">
            <section class="qa-insight-block">
              <div class="qa-record-subtitle">面试讲法</div>
              <p>{{ displayInterviewAnswer(currentAnswer) }}</p>
            </section>
            <section class="qa-insight-block">
              <div class="qa-record-subtitle">简历风险提示</div>
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
            <div class="qa-section-heading">最近问答</div>
            <p class="muted">默认展示最近 20 条，仅当前登录用户可见。</p>
          </div>
        </div>

        <div v-loading="historyLoading" class="qa-history-list">
          <EmptyState
            v-if="!historyLoading && visibleHistory.length === 0"
            title="暂无问答记录，试着问一个和项目实现相关的问题。"
          />

          <article v-for="record in visibleHistory" :key="record.id" class="qa-record">
            <header class="qa-record-header">
              <div class="qa-record-main">
                <h4>{{ record.question }}</h4>
                <div class="qa-record-meta">
                  <el-tag :type="record.aiUsed ? 'success' : 'warning'" effect="light">
                    {{ record.aiUsed ? 'AI 已参与回答' : '规则检索结果' }}
                  </el-tag>
                  <el-tag :type="evidenceTagType(record)" effect="light">
                    {{ effectiveEvidenceLevelText(record) }}
                  </el-tag>
                  <span>可信度 {{ formatConfidence(record) }}</span>
                  <span>{{ formatDate(record.createTime) }}</span>
                  <span>证据 {{ evidenceCount(record) }} 条</span>
                </div>
              </div>
              <div class="qa-record-actions">
                <el-button :icon="CopyDocument" @click="copyQaRecord(record)">复制回答</el-button>
                <el-button :icon="CopyDocument" type="primary" plain @click="copyInterviewRecord(record)">复制面试版回答</el-button>
                <el-button :icon="DeleteIcon" type="danger" plain @click="handleDelete(record)">删除</el-button>
              </div>
            </header>

            <details class="qa-record-details">
              <summary>查看完整回答、面试讲法、简历风险和证据</summary>

              <div class="qa-record-detail-body">
                <el-alert
                  v-if="!record.aiUsed"
                  title="AI 当前不可用或未启用，本回答仅基于关键词检索证据，不代表完整结论。"
                  type="warning"
                  show-icon
                  :closable="false"
                />

                <ConfidenceCard :record="record" />

                <el-alert
                  v-if="isWeakEvidence(record)"
                  title="这个回答证据较弱，建议补充 README、上传更多代码文件，或把问题问得更具体。"
                  type="warning"
                  show-icon
                  :closable="false"
                />

                <MarkdownBlock :content="record.answer" />

                <div class="qa-insight-grid">
                  <section class="qa-insight-block">
                    <div class="qa-record-subtitle">面试讲法</div>
                    <p>{{ displayInterviewAnswer(record) }}</p>
                  </section>
                  <section class="qa-insight-block">
                    <div class="qa-record-subtitle">简历风险提示</div>
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

const quickQuestions = [
  '登录鉴权在哪里实现？',
  'JWT / token 是在哪里生成和校验的？',
  '这个项目用了 Redis 吗？用在哪里？',
  'ZIP 上传有哪些安全限制？',
  '审计报告是如何生成的？',
  'AI 不可用时系统如何 fallback？',
  '这个功能适合写进简历吗？',
  '面试官可能围绕这个项目追问哪些点？'
]

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
        h('span', { class: 'qa-confidence-label' }, '证据可信度'),
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
      h('div', { class: 'qa-record-subtitle' }, '证据'),
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
              }, '复制文件路径')
            ]),
            h('p', { class: 'qa-reason' }, [
              h('span', { class: 'qa-reason-label' }, '命中原因'),
              h('span', null, evidence.reason || '-')
            ]),
            h('pre', { class: 'qa-snippet' }, visibleSnippet(evidence.snippet, key)),
            hasSnippetOverflow(evidence.snippet)
              ? h('button', {
                class: 'snippet-toggle qa-text-button',
                type: 'button',
                onClick: () => toggleSnippet(key)
              }, isSnippetExpanded(key) ? '收起' : '展开')
              : null
          ])
        }))
        : h(EmptyState, {
          title: '当前上传文件中没有找到明显相关证据。',
          description: '建议先保存 README 或上传项目 ZIP，或者把问题问得更具体。'
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
        h('div', { class: 'qa-record-subtitle' }, '建议追问'),
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
    ElMessage.warning('请输入问题')
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
    await ElMessageBox.confirm('确认删除这条问答记录吗？删除后历史中不再显示。', '删除问答记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  await deleteProjectQaRecord(props.projectId, record.id)
  historyRecords.value = historyRecords.value.filter((item) => item.id !== record.id)
  if (currentAnswer.value?.id === record.id) {
    currentAnswer.value = undefined
  }
  ElMessage.success('问答记录已删除')
}

async function copyQaRecord(record: QaDisplayRecord) {
  const text = buildCopyText(record)
  const copied = await copyText(text)

  if (copied) {
    ElMessage.success('回答已复制')
    return
  }

  ElMessage.warning('复制失败，请手动复制回答内容')
}

async function copyInterviewRecord(record: QaDisplayRecord) {
  const copied = await copyText(buildInterviewCopyText(record))

  if (copied) {
    ElMessage.success('面试版回答已复制')
    return
  }

  ElMessage.warning('复制失败，请手动复制面试版回答')
}

async function copyFilePath(filePath: string) {
  if (!filePath) {
    ElMessage.warning('没有可复制的文件路径')
    return
  }

  const copied = await copyText(filePath)
  if (copied) {
    ElMessage.success('文件路径已复制')
    return
  }

  ElMessage.warning('复制失败，请手动复制文件路径')
}

function buildCopyText(record: QaDisplayRecord) {
  return [
    `问题：${record.question || '-'}`,
    '',
    '回答：',
    record.answer || '-',
    '',
    '证据可信度：',
    `${effectiveEvidenceLevelText(record)}（${formatConfidence(record)}）`,
    effectiveEvidenceSummary(record),
    '',
    '证据文件路径：',
    evidencePathsText(record),
    '',
    '建议追问：',
    followUpsText(record)
  ].join('\n')
}

function buildInterviewCopyText(record: QaDisplayRecord) {
  return [
    `问题：${record.question || '-'}`,
    '',
    '证据可信度：',
    `${effectiveEvidenceLevelText(record)}（${formatConfidence(record)}）`,
    effectiveEvidenceSummary(record),
    '',
    '面试讲法：',
    displayInterviewAnswer(record),
    '',
    '简历风险提示：',
    displayResumeRisk(record),
    '',
    '关键证据文件路径：',
    evidencePathsText(record),
    '',
    '建议追问：',
    followUpsText(record)
  ].join('\n')
}

function evidencePathsText(record: QaDisplayRecord) {
  const evidences = safeEvidences(record)
  return evidences.length
    ? evidences.map((evidence, index) => `${index + 1}. ${evidence.filePath || '-'}`).join('\n')
    : '无'
}

function followUpsText(record: QaDisplayRecord) {
  const followUps = safeFollowUps(record)
  return followUps.length
    ? followUps.map((item, index) => `${index + 1}. ${item}`).join('\n')
    : '无'
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
    return '刚刚'
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
    STRONG: '强证据',
    MEDIUM: '中等证据',
    WEAK: '弱证据',
    NONE: '证据不足'
  }
  return levelMap[effectiveEvidenceLevel(record)] || '证据不足'
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
    return '当前上传文件中没有找到明显相关证据。'
  }
  return `当前根据 ${count} 条证据做了简单兜底评估，建议结合文件路径继续复盘。`
}

function displayInterviewAnswer(record: QaDisplayRecord) {
  return record.interviewAnswer || '暂无面试讲法。'
}

function displayResumeRisk(record: QaDisplayRecord) {
  return record.resumeRisk || '暂无简历风险提示。'
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

.qa-follow-up-button,
.qa-text-button {
  border: 1px solid rgba(64, 158, 255, 0.28);
  border-radius: 6px;
  background: #ffffff;
  color: var(--pm-primary);
  cursor: pointer;
  font: inherit;
  padding: 7px 12px;
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
  padding: 16px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: #ffffff;
}

.qa-record-current {
  border-color: rgba(64, 158, 255, 0.35);
  background: #fbfdff;
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
  font-size: 16px;
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
  padding: 14px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  background: #fbfdff;
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
}

.qa-confidence-medium {
  border-color: rgba(64, 158, 255, 0.34);
}

.qa-confidence-weak {
  border-color: rgba(230, 162, 60, 0.36);
}

.qa-confidence-none {
  border-color: rgba(245, 108, 108, 0.34);
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
  background: #ffffff;
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
  background: #ffffff;
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
  border-radius: 6px;
  background: #ffffff;
  color: var(--pm-primary);
  cursor: pointer;
  flex-shrink: 0;
  font: inherit;
  font-size: 12px;
  padding: 5px 8px;
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
  background: #0f172a;
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
