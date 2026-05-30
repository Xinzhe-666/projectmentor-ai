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
                <span>{{ formatDate(currentAnswer.createTime) }}</span>
                <span>证据 {{ currentAnswer.evidences.length }} 条</span>
              </div>
            </div>
            <el-button :icon="CopyDocument" @click="copyQaRecord(currentAnswer)">复制回答</el-button>
          </header>

          <el-alert
            v-if="!currentAnswer.aiUsed"
            title="AI 当前不可用或未启用，本回答仅基于关键词检索证据，不代表完整结论。"
            type="warning"
            show-icon
            :closable="false"
          />

          <MarkdownBlock :content="currentAnswer.answer" />

          <div class="qa-record-subtitle">证据</div>
          <div v-if="currentAnswer.evidences.length" class="qa-evidence-grid">
            <article
              v-for="(evidence, evidenceIndex) in currentAnswer.evidences"
              :key="evidenceKey(currentAnswer, evidenceIndex)"
              class="qa-evidence-item"
            >
              <div class="qa-file-path">{{ evidence.filePath || '-' }}</div>
              <p class="qa-reason">{{ evidence.reason || '-' }}</p>
              <pre class="qa-snippet">{{ visibleSnippet(evidence.snippet, evidenceKey(currentAnswer, evidenceIndex)) }}</pre>
              <el-button
                v-if="hasSnippetOverflow(evidence.snippet)"
                text
                type="primary"
                class="snippet-toggle"
                @click="toggleSnippet(evidenceKey(currentAnswer, evidenceIndex))"
              >
                {{ isSnippetExpanded(evidenceKey(currentAnswer, evidenceIndex)) ? '收起' : '展开' }}
              </el-button>
            </article>
          </div>
          <EmptyState
            v-else
            title="当前上传文件中没有找到明显相关证据。"
            description="建议先保存 README 或上传项目 ZIP，或者把问题问得更具体。"
          />

          <div v-if="currentAnswer.suggestedFollowUps.length" class="qa-follow-ups">
            <div class="qa-record-subtitle">建议追问</div>
            <div class="follow-up-list">
              <el-button
                v-for="followUp in currentAnswer.suggestedFollowUps"
                :key="followUp"
                size="small"
                @click="fillQuestion(followUp)"
              >
                {{ followUp }}
              </el-button>
            </div>
          </div>
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
                  <span>{{ formatDate(record.createTime) }}</span>
                  <span>证据 {{ record.evidences.length }} 条</span>
                </div>
              </div>
              <div class="qa-record-actions">
                <el-button :icon="CopyDocument" @click="copyQaRecord(record)">复制回答</el-button>
                <el-button :icon="DeleteIcon" type="danger" plain @click="handleDelete(record)">删除</el-button>
              </div>
            </header>

            <el-alert
              v-if="!record.aiUsed"
              title="AI 当前不可用或未启用，本回答仅基于关键词检索证据，不代表完整结论。"
              type="warning"
              show-icon
              :closable="false"
            />

            <MarkdownBlock :content="record.answer" />

            <div class="qa-record-subtitle">证据</div>
            <div v-if="record.evidences.length" class="qa-evidence-grid">
              <article
                v-for="(evidence, evidenceIndex) in record.evidences"
                :key="evidenceKey(record, evidenceIndex)"
                class="qa-evidence-item"
              >
                <div class="qa-file-path">{{ evidence.filePath || '-' }}</div>
                <p class="qa-reason">{{ evidence.reason || '-' }}</p>
                <pre class="qa-snippet">{{ visibleSnippet(evidence.snippet, evidenceKey(record, evidenceIndex)) }}</pre>
                <el-button
                  v-if="hasSnippetOverflow(evidence.snippet)"
                  text
                  type="primary"
                  class="snippet-toggle"
                  @click="toggleSnippet(evidenceKey(record, evidenceIndex))"
                >
                  {{ isSnippetExpanded(evidenceKey(record, evidenceIndex)) ? '收起' : '展开' }}
                </el-button>
              </article>
            </div>
            <EmptyState
              v-else
              title="当前上传文件中没有找到明显相关证据。"
              description="建议先保存 README 或上传项目 ZIP，或者把问题问得更具体。"
            />

            <div v-if="record.suggestedFollowUps.length" class="qa-follow-ups">
              <div class="qa-record-subtitle">建议追问</div>
              <div class="follow-up-list">
                <el-button
                  v-for="followUp in record.suggestedFollowUps"
                  :key="followUp"
                  size="small"
                  @click="fillQuestion(followUp)"
                >
                  {{ followUp }}
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CopyDocument, Delete as DeleteIcon, QuestionFilled, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

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
  '这个项目的登录鉴权在哪里实现？',
  '这个项目用了 Redis 吗？',
  '这个项目的 ZIP 上传安全限制在哪里？',
  '这个项目适合写进简历吗？',
  '面试官可能追问哪些点？'
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

function buildCopyText(record: QaDisplayRecord) {
  const evidencePaths = record.evidences.length
    ? record.evidences.map((evidence, index) => `${index + 1}. ${evidence.filePath || '-'}`).join('\n')
    : '无'
  const followUps = record.suggestedFollowUps.length
    ? record.suggestedFollowUps.map((item, index) => `${index + 1}. ${item}`).join('\n')
    : '无'

  return [
    `问题：${record.question || '-'}`,
    '',
    '回答：',
    record.answer || '-',
    '',
    '证据文件路径：',
    evidencePaths,
    '',
    '建议追问：',
    followUps
  ].join('\n')
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

function evidenceKey(record: QaDisplayRecord, evidenceIndex: number) {
  return `${record.id ?? 'current'}-${evidenceIndex}-${record.evidences[evidenceIndex]?.filePath || 'evidence'}`
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

<style scoped>
.qa-body,
.qa-current,
.qa-history,
.qa-history-list,
.qa-record,
.qa-follow-ups {
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
.follow-up-list .el-button {
  margin-left: 0;
  max-width: 100%;
  white-space: normal;
  height: auto;
  min-height: 32px;
  line-height: 1.4;
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

.qa-record-subtitle {
  color: #344054;
  font-weight: 800;
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

.qa-file-path {
  margin-bottom: 8px;
  color: var(--pm-primary);
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.qa-reason {
  margin: 0 0 10px;
  color: var(--pm-muted);
  line-height: 1.6;
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
  padding-left: 0;
}

@media (max-width: 860px) {
  .qa-evidence-grid,
  .qa-input-row {
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
}
</style>
