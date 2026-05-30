<template>
  <section class="panel qa-panel">
    <div class="panel-title">
      <div>
        <h3>项目问答</h3>
        <p class="muted">基于当前项目已上传的 README 和代码文件进行轻量检索增强问答。回答会尽量附带文件证据；如果证据不足，系统会明确提示。</p>
      </div>
      <el-tag v-if="result" :type="result.aiUsed ? 'success' : 'warning'" effect="light">
        {{ result.aiUsed ? 'AI 已参与回答' : 'AI 不可用，当前为规则检索结果' }}
      </el-tag>
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
          @click="question = item"
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

      <div v-if="result" class="qa-result">
        <div class="qa-answer">
          <div class="qa-section-title">回答</div>
          <MarkdownBlock :content="result.answer" />
        </div>

        <div class="qa-evidence-list">
          <div class="qa-section-title">证据</div>
          <div v-if="result.evidences.length" class="qa-evidence-grid">
            <article v-for="evidence in result.evidences" :key="`${evidence.filePath}-${evidence.snippet}`" class="qa-evidence-item">
              <div class="qa-file-path">{{ evidence.filePath }}</div>
              <p class="qa-reason">{{ evidence.reason }}</p>
              <pre class="qa-snippet">{{ evidence.snippet }}</pre>
            </article>
          </div>
          <EmptyState
            v-else
            title="没有明显相关证据"
            description="建议先补充 README 或上传更完整的项目 ZIP，也可以换一个更具体的问题。"
          />
        </div>

        <div v-if="result.suggestedFollowUps.length" class="qa-follow-ups">
          <div class="qa-section-title">建议追问</div>
          <div class="follow-up-list">
            <el-button
              v-for="followUp in result.suggestedFollowUps"
              :key="followUp"
              size="small"
              @click="question = followUp"
            >
              {{ followUp }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { askProjectQa } from '@/api/projectQa'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import type { ProjectQaResponse } from '@/types/api'

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
const result = ref<ProjectQaResponse>()

async function handleAsk() {
  const trimmedQuestion = question.value.trim()
  if (!trimmedQuestion) {
    ElMessage.warning('请输入问题')
    return
  }

  loading.value = true
  try {
    result.value = await askProjectQa(props.projectId, trimmedQuestion)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.qa-body,
.qa-result,
.qa-answer,
.qa-evidence-list,
.qa-follow-ups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.quick-question-list,
.follow-up-list {
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

.qa-section-title {
  color: #344054;
  font-weight: 800;
}

.qa-answer {
  padding: 16px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #fbfdff;
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
  max-height: 240px;
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

@media (max-width: 860px) {
  .qa-evidence-grid,
  .qa-input-row {
    grid-template-columns: 1fr;
  }

  .qa-input-row .el-button {
    width: 100%;
  }
}
</style>
