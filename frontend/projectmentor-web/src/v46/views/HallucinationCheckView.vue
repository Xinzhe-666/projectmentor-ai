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
          <el-form-item :label="t('hallucination.projectMode')">
            <el-radio-group v-model="form.projectMode">
              <el-radio-button label="TEXT_ONLY">{{ t('hallucination.textOnlyMode') }}</el-radio-button>
              <el-radio-button label="PROJECT">{{ t('hallucination.bindProjectMode') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.projectMode === 'PROJECT'" :label="t('hallucination.project')">
            <el-select
              v-model="form.projectId"
              clearable
              filterable
              :placeholder="t('hallucination.projectPlaceholder')"
              :loading="projectLoading"
              class="wide-control"
            >
              <el-option
                v-for="project in projects"
                :key="project.id"
                :label="project.name"
                :value="project.id"
              >
                <div class="project-option">
                  <strong>{{ project.name }}</strong>
                  <span>{{ project.techStack || t('common.notFilled') }} · {{ project.createTime || '-' }}</span>
                </div>
              </el-option>
            </el-select>
            <p v-if="selectedProject" class="selected-project-hint">
              {{ t('hallucination.selectedProject', { name: selectedProject.name }) }}
            </p>
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
            <div class="ai-action-row">
              <el-button type="primary" :loading="loading" @click="handleCheck">{{ t('hallucination.start') }}</el-button>
              <span>{{ t('hallucination.aiCost', { count: AI_CREDIT_COSTS.HALLUCINATION_CHECK }) }}</span>
            </div>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getMyCredits } from '@/v46/api/credit'
import { checkHallucination } from '@/v46/api/hallucination'
import { listProjects } from '@/v46/api/project'
import EmptyState from '@/v46/components/EmptyState.vue'
import MarkdownBlock from '@/v46/components/MarkdownBlock.vue'
import ScoreRing from '@/v46/components/ScoreRing.vue'
import { AI_CREDIT_COSTS } from '@/v46/constants/creditCosts'
import { useUserStore } from '@/stores/user'
import type { HallucinationCheckResult, Project } from '@/v46/types/api'

const loading = ref(false)
const projectLoading = ref(false)
const route = useRoute()
const { t } = useI18n()
const userStore = useUserStore()
const result = ref<HallucinationCheckResult>()
const projects = ref<Project[]>([])
const form = reactive({
  projectMode: 'TEXT_ONLY' as 'TEXT_ONLY' | 'PROJECT',
  projectId: undefined as number | undefined,
  aiAnswer: ''
})

const selectedProject = computed(() => projects.value.find((project) => project.id === form.projectId))

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

  if (form.projectMode === 'PROJECT' && !form.projectId) {
    ElMessage.warning(t('hallucination.selectProjectOrTextOnly'))
    return
  }

  try {
    await ElMessageBox.confirm(
      t('credits.confirmAiUse', { count: AI_CREDIT_COSTS.HALLUCINATION_CHECK }),
      t('report.claimAiConfirmTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )
  } catch {
    return
  }

  loading.value = true
  try {
    result.value = await checkHallucination({
      projectId: form.projectMode === 'PROJECT' ? form.projectId : undefined,
      aiAnswer: form.aiAnswer
    })
    if (result.value.creditsRefunded) {
      ElMessage.warning(t('credits.aiFailedRefunded'))
    }
  } finally {
    await syncCredits()
    loading.value = false
  }
}

async function syncCredits() {
  try {
    const credits = await getMyCredits()
    userStore.updateCredits(credits.remainingCredits)
  } catch {
    // Header balance will refresh on the next normal credit fetch.
  }
}

async function loadProjects() {
  projectLoading.value = true
  try {
    projects.value = await listProjects()
    applyProjectIdFromQuery()
  } finally {
    projectLoading.value = false
  }
}

function applyProjectIdFromQuery() {
  const rawProjectId = Array.isArray(route.query.projectId) ? route.query.projectId[0] : route.query.projectId
  const projectId = Number(rawProjectId)

  if (!Number.isFinite(projectId) || projectId <= 0) {
    return
  }

  const matchedProject = projects.value.find((project) => project.id === projectId)
  if (!matchedProject) {
    ElMessage.warning(t('hallucination.projectNotFound'))
    return
  }

  form.projectMode = 'PROJECT'
  form.projectId = matchedProject.id
}

onMounted(loadProjects)
</script>

<style scoped>
.wide-control {
  width: min(520px, 100%);
}

.ai-action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.ai-action-row span {
  color: var(--pm-muted);
  font-size: 12px;
}

.project-option {
  display: grid;
  gap: 2px;
  line-height: 1.35;
}

.project-option span {
  color: var(--pm-muted);
  font-size: 12px;
}

.selected-project-hint {
  margin: 8px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
}

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
