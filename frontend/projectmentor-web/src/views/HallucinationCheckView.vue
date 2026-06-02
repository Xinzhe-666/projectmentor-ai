<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('hallucination.title') }}</h2>
          <p class="muted">{{ t('hallucination.desc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <el-form :model="form" label-width="110px">
          <el-form-item :label="t('hallucination.projectId')">
            <el-input v-model="form.projectId" :placeholder="t('hallucination.projectPlaceholder')" />
          </el-form-item>
          <el-form-item :label="t('hallucination.aiAnswer')" required>
            <el-input
              v-model="form.aiAnswer"
              type="textarea"
              :rows="9"
              :placeholder="t('hallucination.answerPlaceholder')"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleCheck">{{ t('hallucination.start') }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>

    <section v-if="result" class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('hallucination.result') }}</h3>
          <p class="muted">{{ t('hallucination.riskLevel', { level: result.riskLevel }) }}</p>
        </div>
        <el-tag :class="riskLevelClass(result.riskLevel)" effect="light">{{ result.riskLevel }}</el-tag>
      </div>
      <div class="panel-body page-stack">
        <div class="ring-grid">
          <ScoreRing :score="result.credibilityScore" :title="t('hallucination.credibility')" />
          <ScoreRing :score="result.objectivityScore" :title="t('hallucination.objectivity')" />
        </div>

        <div class="flag-grid">
          <div class="metric-card">
            <span>{{ t('hallucination.overEncouragement') }}</span>
            <strong>{{ result.overEncouragementRisk ? t('common.yes') : t('common.none') }}</strong>
          </div>
          <div class="metric-card">
            <span>{{ t('hallucination.missingEvidence') }}</span>
            <strong>{{ result.missingEvidenceRisk ? t('common.yes') : t('common.none') }}</strong>
          </div>
          <div class="metric-card">
            <span>{{ t('hallucination.resumeRisk') }}</span>
            <strong>{{ result.resumeRisk ? t('common.yes') : t('common.none') }}</strong>
          </div>
          <div class="metric-card">
            <span>{{ t('hallucination.issueCount') }}</span>
            <strong>{{ result.issueCount }}</strong>
          </div>
        </div>

        <div>
          <h3 class="sub-title">{{ t('hallucination.riskIssues') }}</h3>
          <div v-if="result.issues.length" class="issue-list">
            <article v-for="(issue, index) in result.issues" :key="`${issue.issueType}-${index}`" class="issue-card">
              <div class="issue-head">
                <el-tag :class="riskLevelClass(issue.riskLevel)" effect="light">{{ issue.riskLevel }}</el-tag>
                <strong>{{ issue.issueType }}</strong>
              </div>
              <p v-if="issue.matchedText" class="matched-text">{{ issue.matchedText }}</p>
              <p>{{ issue.message }}</p>
              <p v-if="issue.evidence" class="muted">{{ t('hallucination.evidence') }}：{{ issue.evidence }}</p>
              <p v-if="issue.suggestion" class="muted">{{ t('hallucination.suggestion') }}：{{ issue.suggestion }}</p>
            </article>
          </div>
          <EmptyState v-else :title="t('hallucination.noIssuesTitle')" :description="t('hallucination.noIssuesDesc')" />
        </div>

        <div>
          <h3 class="sub-title">{{ t('hallucination.unsafeTitle') }}</h3>
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
          <EmptyState v-else :title="t('hallucination.noUnsafeTitle')" :description="t('hallucination.noUnsafeDesc')" />
        </div>

        <div>
          <h3 class="sub-title">{{ t('hallucination.rewriteTitle') }}</h3>
          <MarkdownBlock :content="result.saferRewrite" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { checkHallucination } from '@/api/hallucination'
import EmptyState from '@/components/EmptyState.vue'
import MarkdownBlock from '@/components/MarkdownBlock.vue'
import ScoreRing from '@/components/ScoreRing.vue'
import type { HallucinationCheckResult } from '@/types/api'

const loading = ref(false)
const { t } = useI18n()
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
    ElMessage.warning(t('hallucination.answerRequired'))
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
