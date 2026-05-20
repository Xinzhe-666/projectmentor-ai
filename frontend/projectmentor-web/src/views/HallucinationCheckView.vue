<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>AI 幻觉检测</h2>
          <p class="muted">输入 AI 对项目的描述，检查夸大、缺证据和简历风险。</p>
        </div>
      </div>
      <div class="panel-body">
        <el-form :model="form" label-width="110px">
          <el-form-item label="项目 ID">
            <el-input v-model="form.projectId" placeholder="可选，传入后会结合项目文件证据" />
          </el-form-item>
          <el-form-item label="AI 回答" required>
            <el-input
              v-model="form.aiAnswer"
              type="textarea"
              :rows="9"
              placeholder="粘贴 AI 生成的项目描述、简历项目经历或面试包装文案"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleCheck">开始检测</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section v-if="result" class="panel">
      <div class="panel-title">
        <div>
          <h3>检测结果</h3>
          <p class="muted">风险等级：{{ result.riskLevel }}</p>
        </div>
        <el-tag :class="riskLevelClass(result.riskLevel)" effect="light">{{ result.riskLevel }}</el-tag>
      </div>
      <div class="panel-body page-stack">
        <div class="ring-grid">
          <ScoreRing :score="result.credibilityScore" title="可信度" />
          <ScoreRing :score="result.objectivityScore" title="客观度" />
        </div>

        <div class="flag-grid">
          <div class="metric-card">
            <span>过度鼓励风险</span>
            <strong>{{ result.overEncouragementRisk ? '有' : '无' }}</strong>
          </div>
          <div class="metric-card">
            <span>缺少证据风险</span>
            <strong>{{ result.missingEvidenceRisk ? '有' : '无' }}</strong>
          </div>
          <div class="metric-card">
            <span>简历风险</span>
            <strong>{{ result.resumeRisk ? '有' : '无' }}</strong>
          </div>
          <div class="metric-card">
            <span>问题数</span>
            <strong>{{ result.issueCount }}</strong>
          </div>
        </div>

        <div>
          <h3 class="sub-title">风险问题</h3>
          <div v-if="result.issues.length" class="issue-list">
            <article v-for="(issue, index) in result.issues" :key="`${issue.issueType}-${index}`" class="issue-card">
              <div class="issue-head">
                <el-tag :class="riskLevelClass(issue.riskLevel)" effect="light">{{ issue.riskLevel }}</el-tag>
                <strong>{{ issue.issueType }}</strong>
              </div>
              <p v-if="issue.matchedText" class="matched-text">{{ issue.matchedText }}</p>
              <p>{{ issue.message }}</p>
              <p v-if="issue.evidence" class="muted">证据：{{ issue.evidence }}</p>
              <p v-if="issue.suggestion" class="muted">建议：{{ issue.suggestion }}</p>
            </article>
          </div>
          <EmptyState v-else title="暂无结构化风险" description="当前回答没有返回具体风险问题。" />
        </div>

        <div>
          <h3 class="sub-title">不安全简历表述</h3>
          <template v-if="result.unsafeResumeStatements.length">
            <el-tag
              v-for="statement in result.unsafeResumeStatements"
              :key="statement"
              class="tag-item"
              type="warning"
              effect="light"
            >
              {{ statement }}
            </el-tag>
          </template>
          <EmptyState v-else title="暂无不安全表述" description="当前结果没有标记需要规避的简历表述。" />
        </div>

        <div>
          <h3 class="sub-title">更稳妥改写</h3>
          <MarkdownBlock :content="result.saferRewrite" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { checkHallucination } from '@/api/hallucination'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import type { HallucinationCheckResult } from '@/types/api'

const loading = ref(false)
const result = ref<HallucinationCheckResult>()
const form = reactive({
  projectId: '',
  aiAnswer: ''
})

function riskLevelClass(level?: string) {
  const normalized = (level || 'INFO').toUpperCase()
  return {
    'risk-high': normalized === 'HIGH',
    'risk-medium': normalized === 'MEDIUM',
    'risk-low': normalized === 'LOW',
    'risk-info': normalized === 'INFO'
  }
}

async function handleCheck() {
  if (!form.aiAnswer.trim()) {
    ElMessage.warning('请输入 AI 回答内容')
    return
  }

  loading.value = true
  try {
    result.value = await checkHallucination({
      projectId: form.projectId ? Number(form.projectId) : undefined,
      aiAnswer: form.aiAnswer
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ring-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.flag-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.sub-title {
  margin: 0 0 12px;
}

.issue-list {
  display: grid;
  gap: 12px;
}

.issue-card {
  padding: 16px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.issue-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.issue-card p {
  margin: 10px 0 0;
  line-height: 1.75;
}

.matched-text {
  color: var(--pm-primary);
  font-weight: 600;
}

.tag-item {
  margin: 0 8px 8px 0;
  max-width: 100%;
  white-space: normal;
  height: auto;
  padding: 6px 10px;
  line-height: 1.5;
}

@media (max-width: 920px) {
  .flag-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .ring-grid,
  .flag-grid {
    grid-template-columns: 1fr;
  }
}
</style>
