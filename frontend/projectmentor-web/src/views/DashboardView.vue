<template>
  <div class="dashboard-workspace" :aria-busy="loading">
    <section v-if="loadError" class="workspace-error" role="alert">
      <div>
        <StatusLabel status="FAILED" :label="t('dashboard.v5.loadErrorStatus')" />
        <strong>{{ t('dashboard.v5.loadErrorTitle') }}</strong>
        <p>{{ t(summary ? 'dashboard.v5.refreshErrorDescription' : 'dashboard.v5.loadErrorDescription') }}</p>
      </div>
      <el-button :loading="loading" @click="loadDashboard">{{ t('dashboard.v5.retry') }}</el-button>
    </section>

    <section class="workspace-summary" :aria-labelledby="summaryHeadingId">
      <div class="workspace-section-heading">
        <div>
          <h2 :id="summaryHeadingId">{{ t('dashboard.v5.summaryTitle') }}</h2>
          <p>{{ t('dashboard.v5.summaryDescription') }}</p>
        </div>
        <div class="ai-service-state">
          <span>{{ t('dashboard.v5.aiService') }}</span>
          <StatusLabel :status="aiServiceState.status" :label="aiServiceState.label" />
        </div>
      </div>

      <div class="metric-ledger">
        <RouterLink v-for="metric in metrics" :key="metric.path" class="metric-ledger-item" :to="metric.path">
          <span>{{ metric.label }}</span>
          <span v-if="loading && !summary" class="metric-skeleton" aria-hidden="true" />
          <strong v-else>{{ metric.value }}</strong>
        </RouterLink>
      </div>

      <nav class="workspace-actions" :aria-label="t('dashboard.v5.quickActions')">
        <span>{{ t('dashboard.v5.quickActions') }}</span>
        <RouterLink to="/hallucination">
          <el-icon><Warning /></el-icon>
          {{ t('dashboard.v5.checkClaims') }}
        </RouterLink>
        <RouterLink to="/interview">
          <el-icon><ChatDotRound /></el-icon>
          {{ t('dashboard.v5.startInterview') }}
        </RouterLink>
        <RouterLink to="/credits">
          <el-icon><Coin /></el-icon>
          {{ t('dashboard.v5.reviewCredits') }}
        </RouterLink>
      </nav>
    </section>

    <section class="workspace-boundary" role="note">
      <el-icon><Lock /></el-icon>
      <div>
        <strong>{{ t('dashboard.v5.privacyTitle') }}</strong>
        <p>{{ t('dashboard.v5.privacyDescription') }}</p>
      </div>
    </section>

    <section class="recent-ledger" :aria-labelledby="recentHeadingId">
      <div class="workspace-section-heading recent-ledger-heading">
        <div>
          <h2 :id="recentHeadingId">{{ t('dashboard.v5.recentTitle') }}</h2>
          <p>{{ t('dashboard.v5.recentDescription') }}</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="evidence-tabs">
        <el-tab-pane name="projects">
          <template #label>
            <span class="tab-label">
              {{ t('dashboard.v5.tabs.projects') }}
              <span>{{ summary?.projectCount ?? '—' }}</span>
            </span>
          </template>

          <div v-if="loading && !summary" class="record-skeleton" aria-hidden="true">
            <i v-for="index in 4" :key="index" />
          </div>
          <p v-else-if="loadError && !summary" class="records-unavailable">{{ t('dashboard.v5.recordsUnavailable') }}</p>
          <template v-else-if="recentProjects.length">
            <el-table class="desktop-record-table" :data="recentProjects">
              <el-table-column prop="name" :label="t('common.projectName')" min-width="190" show-overflow-tooltip />
              <el-table-column prop="techStack" :label="t('common.techStack')" min-width="190" show-overflow-tooltip>
                <template #default="{ row }">{{ row.techStack || '—' }}</template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="144">
                <template #default="{ row }"><StatusLabel :status="row.status" /></template>
              </el-table-column>
              <el-table-column :label="t('common.createTime')" min-width="150">
                <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
              </el-table-column>
              <el-table-column :label="t('common.operation')" width="86" align="right">
                <template #default="{ row }">
                  <RouterLink class="table-action" :to="`/projects/${row.id}`">{{ t('common.view') }}</RouterLink>
                </template>
              </el-table-column>
            </el-table>

            <ul class="mobile-record-list">
              <li v-for="project in recentProjects" :key="project.id">
                <RouterLink :to="`/projects/${project.id}`">
                  <span class="mobile-record-main">
                    <strong>{{ project.name }}</strong>
                    <small>{{ project.techStack || '—' }}</small>
                  </span>
                  <span class="mobile-record-meta">
                    <StatusLabel :status="project.status" />
                    <time>{{ formatDate(project.createTime) }}</time>
                  </span>
                </RouterLink>
              </li>
            </ul>
          </template>
          <EmptyState
            v-else
            variant="compact"
            :title="t('dashboard.v5.emptyProjectsTitle')"
            :description="t('dashboard.v5.emptyProjectsDescription')"
          >
            <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">
              {{ t('shellV5.createProject') }}
            </el-button>
          </EmptyState>
        </el-tab-pane>

        <el-tab-pane name="reports">
          <template #label>
            <span class="tab-label">
              {{ t('dashboard.v5.tabs.reports') }}
              <span>{{ summary?.reportCount ?? '—' }}</span>
            </span>
          </template>

          <div v-if="loading && !summary" class="record-skeleton" aria-hidden="true">
            <i v-for="index in 4" :key="index" />
          </div>
          <p v-else-if="loadError && !summary" class="records-unavailable">{{ t('dashboard.v5.recordsUnavailable') }}</p>
          <template v-else-if="recentReports.length">
            <el-table class="desktop-record-table" :data="recentReports">
              <el-table-column prop="projectName" :label="t('common.projectName')" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.projectName || t('common.unnamedProject') }}</template>
              </el-table-column>
              <el-table-column :label="t('common.score')" width="120">
                <template #default="{ row }"><span class="numeric-data">{{ formatScore(row.healthScore) }}</span></template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="144">
                <template #default="{ row }"><StatusLabel :status="row.status || 'FINISHED'" /></template>
              </el-table-column>
              <el-table-column :label="t('common.createTime')" min-width="150">
                <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
              </el-table-column>
              <el-table-column :label="t('common.operation')" width="86" align="right">
                <template #default="{ row }">
                  <RouterLink class="table-action" :to="`/reports/${row.reportId}`">{{ t('common.view') }}</RouterLink>
                </template>
              </el-table-column>
            </el-table>

            <ul class="mobile-record-list">
              <li v-for="report in recentReports" :key="report.reportId">
                <RouterLink :to="`/reports/${report.reportId}`">
                  <span class="mobile-record-main">
                    <strong>{{ report.projectName || t('common.unnamedProject') }}</strong>
                    <small>{{ t('common.score') }} · {{ formatScore(report.healthScore) }}</small>
                  </span>
                  <span class="mobile-record-meta">
                    <StatusLabel :status="report.status || 'FINISHED'" />
                    <time>{{ formatDate(report.createTime) }}</time>
                  </span>
                </RouterLink>
              </li>
            </ul>
          </template>
          <EmptyState
            v-else
            variant="compact"
            :title="t('dashboard.v5.emptyReportsTitle')"
            :description="t('dashboard.v5.emptyReportsDescription')"
          >
            <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">
              {{ t('dashboard.v5.prepareEvidence') }}
            </el-button>
          </EmptyState>
        </el-tab-pane>

        <el-tab-pane name="interviews">
          <template #label>
            <span class="tab-label">
              {{ t('dashboard.v5.tabs.interviews') }}
              <span>{{ summary?.interviewSessionCount ?? '—' }}</span>
            </span>
          </template>

          <div v-if="loading && !summary" class="record-skeleton" aria-hidden="true">
            <i v-for="index in 4" :key="index" />
          </div>
          <p v-else-if="loadError && !summary" class="records-unavailable">{{ t('dashboard.v5.recordsUnavailable') }}</p>
          <template v-else-if="recentInterviews.length">
            <el-table class="desktop-record-table" :data="recentInterviews">
              <el-table-column prop="projectName" :label="t('common.projectName')" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">{{ row.projectName || t('common.unnamedProject') }}</template>
              </el-table-column>
              <el-table-column :label="t('dashboard.v5.interviewProgress')" width="128">
                <template #default="{ row }">
                  <span class="numeric-data">{{ row.answeredCount }} / {{ row.questionCount }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="t('common.score')" width="100">
                <template #default="{ row }"><span class="numeric-data">{{ formatScore(row.totalScore) }}</span></template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="144">
                <template #default="{ row }"><StatusLabel :status="row.status || 'RUNNING'" /></template>
              </el-table-column>
              <el-table-column :label="t('common.createTime')" min-width="150">
                <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
              </el-table-column>
              <el-table-column :label="t('common.operation')" width="86" align="right">
                <template #default="{ row }">
                  <RouterLink class="table-action" :to="`/interview?sessionId=${row.sessionId}`">{{ t('common.view') }}</RouterLink>
                </template>
              </el-table-column>
            </el-table>

            <ul class="mobile-record-list">
              <li v-for="interview in recentInterviews" :key="interview.sessionId">
                <RouterLink :to="`/interview?sessionId=${interview.sessionId}`">
                  <span class="mobile-record-main">
                    <strong>{{ interview.projectName || t('common.unnamedProject') }}</strong>
                    <small>{{ t('dashboard.v5.interviewProgress') }} · {{ interview.answeredCount }} / {{ interview.questionCount }}</small>
                  </span>
                  <span class="mobile-record-meta">
                    <StatusLabel :status="interview.status || 'RUNNING'" />
                    <time>{{ formatDate(interview.createTime) }}</time>
                  </span>
                </RouterLink>
              </li>
            </ul>
          </template>
          <EmptyState
            v-else
            variant="compact"
            :title="t('dashboard.v5.emptyInterviewsTitle')"
            :description="t('dashboard.v5.emptyInterviewsDescription')"
          >
            <el-button type="primary" @click="router.push('/interview')">{{ t('dashboard.v5.startInterview') }}</el-button>
          </EmptyState>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChatDotRound, Coin, Lock, Plus, Warning } from '@element-plus/icons-vue'

import { getAiStatus } from '@/api/ai'
import { getDashboardSummary } from '@/api/dashboard'
import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import { useUserStore } from '@/stores/user'
import type { AiStatus, DashboardSummary, InterviewSessionListItem, Project, ReportListItem } from '@/types/api'

const router = useRouter()
const { t, locale } = useI18n()
const userStore = useUserStore()

const summaryHeadingId = 'dashboard-workspace-summary'
const recentHeadingId = 'dashboard-recent-work'
const loading = ref(false)
const loadError = ref(false)
const summary = ref<DashboardSummary>()
const aiStatus = ref<AiStatus | null>(null)
const aiStatusResolved = ref(false)
const activeTab = ref('projects')

const metrics = computed(() => [
  { label: t('dashboard.v5.metrics.projects'), value: summary.value ? summary.value.projectCount : '—', path: '/projects' },
  { label: t('dashboard.v5.metrics.reports'), value: summary.value ? summary.value.reportCount : '—', path: '/reports' },
  { label: t('dashboard.v5.metrics.interviews'), value: summary.value ? summary.value.interviewSessionCount : '—', path: '/interviews' },
  { label: t('dashboard.v5.metrics.credits'), value: summary.value?.creditBalance ?? userStore.remainingCredits, path: '/credits' }
])

const aiServiceState = computed(() => {
  if (!aiStatusResolved.value) {
    return { status: 'PENDING', label: t('dashboard.v5.aiChecking') }
  }

  if (!aiStatus.value) {
    return { status: 'UNKNOWN', label: t('dashboard.v5.aiUnavailable') }
  }

  if (aiStatus.value.enabled && aiStatus.value.configured) {
    return { status: 'AVAILABLE', label: t('dashboard.v5.aiAvailable') }
  }

  return { status: 'RULES_ONLY', label: t('dashboard.v5.aiRulesOnly') }
})

const recentProjects = computed<Project[]>(() => summary.value?.recentProjects || [])
const recentReports = computed<ReportListItem[]>(() => summary.value?.recentReports || [])
const recentInterviews = computed<InterviewSessionListItem[]>(() => summary.value?.recentInterviews || [])

function formatScore(score?: number) {
  return Number.isFinite(score) ? Math.round(Number(score)) : '—'
}

function formatDate(value?: string) {
  if (!value) {
    return '—'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(locale.value, {
    year: 'numeric',
    month: 'short',
    day: '2-digit'
  }).format(date)
}

async function loadDashboard() {
  loading.value = true
  loadError.value = false
  aiStatusResolved.value = false

  const [summaryResult, aiResult] = await Promise.allSettled([getDashboardSummary(), getAiStatus()])

  if (summaryResult.status === 'fulfilled') {
    summary.value = summaryResult.value
    userStore.updateCredits(summaryResult.value.creditBalance)
  } else {
    loadError.value = true
  }

  aiStatus.value = aiResult.status === 'fulfilled' ? aiResult.value : null
  aiStatusResolved.value = true
  loading.value = false
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-workspace {
  display: grid;
  gap: var(--pm-space-10);
}

.workspace-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--pm-space-5);
  padding: var(--pm-space-4) var(--pm-space-5);
  border: 1px solid var(--pm-risk-border);
  border-radius: var(--pm-radius-sm);
  background: var(--pm-risk-bg);
}

.workspace-error strong,
.workspace-error p {
  display: block;
}

.workspace-error strong {
  margin-top: 8px;
  color: var(--pm-ink);
  font-size: 15px;
}

.workspace-error p {
  margin: 4px 0 0;
  color: var(--pm-graphite);
  font-size: 14px;
  line-height: 1.5;
}

.workspace-summary,
.recent-ledger {
  min-width: 0;
}

.workspace-section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--pm-space-6);
  margin-bottom: var(--pm-space-4);
}

.workspace-section-heading h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: var(--pm-type-section-title);
  font-weight: 600;
  letter-spacing: -0.012em;
  line-height: 1.3;
}

.workspace-section-heading p {
  max-width: 68ch;
  margin: 5px 0 0;
  color: var(--pm-muted);
  font-size: 14px;
  line-height: 1.55;
}

.ai-service-state {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--pm-space-3);
  padding-bottom: 2px;
}

.ai-service-state > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.075em;
  text-transform: uppercase;
}

.metric-ledger {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid var(--pm-stone-strong);
  border-bottom: 1px solid var(--pm-stone-strong);
  background: var(--pm-surface);
}

.metric-ledger-item {
  display: flex;
  min-width: 0;
  min-height: 104px;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--pm-space-3);
  padding: var(--pm-space-5);
  border-right: 1px solid var(--pm-stone);
  transition: background-color var(--pm-motion-base) var(--pm-ease-standard);
}

.metric-ledger-item:last-child {
  border-right: 0;
}

.metric-ledger-item:hover {
  background: var(--pm-paper);
}

.metric-ledger-item > span:first-child {
  color: var(--pm-muted);
  font-size: 13px;
}

.metric-ledger-item strong {
  color: var(--pm-ink);
  font-size: 30px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.025em;
  line-height: 1;
}

.metric-skeleton {
  display: block;
  width: 46%;
  height: 28px;
  background: var(--pm-soft);
}

.workspace-actions {
  display: flex;
  align-items: center;
  gap: var(--pm-space-5);
  min-height: 50px;
  border-bottom: 1px solid var(--pm-stone);
  overflow-x: auto;
}

.workspace-actions > span {
  flex: 0 0 auto;
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  letter-spacing: 0.075em;
  text-transform: uppercase;
}

.workspace-actions a {
  display: inline-flex;
  min-height: 44px;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  color: var(--pm-graphite);
  font-size: 13px;
  font-weight: 600;
  text-underline-offset: 4px;
  transition: color var(--pm-motion-fast) ease;
}

.workspace-actions a:hover {
  color: var(--pm-primary);
  text-decoration: underline;
}

.workspace-boundary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--pm-space-3);
  padding: var(--pm-space-4);
  border: 1px solid var(--pm-stone);
  border-radius: var(--pm-radius-sm);
  background: var(--pm-surface);
}

.workspace-boundary :deep(.el-icon) {
  margin-top: 2px;
  color: var(--pm-amber);
  font-size: 16px;
}

.workspace-boundary strong {
  display: block;
  color: var(--pm-ink);
  font-size: 13px;
  font-weight: 600;
}

.workspace-boundary p {
  max-width: 75ch;
  margin: 3px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.55;
}

.recent-ledger-heading {
  margin-bottom: 0;
}

.tab-label {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
}

.tab-label > span {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.table-action {
  color: var(--pm-primary);
  font-size: 13px;
  font-weight: 600;
  text-underline-offset: 4px;
}

.table-action:hover {
  text-decoration: underline;
}

.numeric-data {
  font-variant-numeric: tabular-nums;
}

.record-skeleton {
  display: grid;
  gap: 1px;
  padding: 1px 0;
  background: var(--pm-stone);
}

.record-skeleton i {
  display: block;
  height: 52px;
  background: var(--pm-surface);
  border-bottom: 12px solid var(--pm-paper);
}

.records-unavailable {
  margin: 0;
  padding: var(--pm-space-6) 0;
  border-top: 1px solid var(--pm-stone);
  border-bottom: 1px solid var(--pm-stone);
  color: var(--pm-muted);
  font-size: 14px;
}

.mobile-record-list {
  display: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--pm-stone-strong);
  list-style: none;
}

.mobile-record-list li {
  border-bottom: 1px solid var(--pm-stone);
}

.mobile-record-list a {
  display: grid;
  gap: var(--pm-space-3);
  min-height: 76px;
  padding: var(--pm-space-4) 0;
}

.mobile-record-main,
.mobile-record-meta {
  display: flex;
  min-width: 0;
  justify-content: space-between;
  gap: var(--pm-space-3);
}

.mobile-record-main strong {
  min-width: 0;
  overflow: hidden;
  color: var(--pm-ink);
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-record-main small,
.mobile-record-meta time {
  flex: 0 0 auto;
  color: var(--pm-muted);
  font-size: 12px;
}

.mobile-record-meta {
  align-items: center;
}

@media (max-width: 900px) {
  .metric-ledger {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-ledger-item:nth-child(2) {
    border-right: 0;
  }

  .metric-ledger-item:nth-child(-n + 2) {
    border-bottom: 1px solid var(--pm-stone);
  }
}

@media (max-width: 820px) {
  .desktop-record-table {
    display: none;
  }

  .mobile-record-list {
    display: block;
  }
}

@media (max-width: 640px) {
  .dashboard-workspace {
    gap: var(--pm-space-8);
  }

  .workspace-error,
  .workspace-section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .workspace-error {
    padding: var(--pm-space-4);
  }

  .ai-service-state {
    width: 100%;
    justify-content: space-between;
    padding-top: var(--pm-space-2);
    border-top: 1px solid var(--pm-stone);
  }

  .metric-ledger-item {
    min-height: 92px;
    padding: var(--pm-space-4);
  }

  .metric-ledger-item strong {
    font-size: 26px;
  }

  .workspace-actions {
    gap: var(--pm-space-4);
    padding: var(--pm-space-1) 0 var(--pm-space-2);
    flex-wrap: wrap;
    overflow-x: visible;
  }

  .mobile-record-main {
    display: grid;
    justify-content: normal;
    gap: 3px;
  }

  .mobile-record-main small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
