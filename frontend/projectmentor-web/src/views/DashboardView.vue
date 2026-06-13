<template>
  <div class="page-stack">
    <section class="panel dashboard-hero-panel pm-command-panel pm-gradient-border">
      <div class="panel-body dashboard-hero">
        <div class="dashboard-hero-copy">
          <p class="eyebrow">{{ t('dashboard.eyebrow') }}</p>
          <h2>{{ t('dashboard.greeting', { name: userStore.userInfo?.username || t('common.classmate') }) }}</h2>
          <p class="muted">
            {{ t('dashboard.subtitle') }}
          </p>
          <div class="pm-chip-row dashboard-hero-chips">
            <span v-for="chip in heroChips" :key="chip" class="pm-chip">{{ chip }}</span>
          </div>
        </div>
        <div class="dashboard-hero-card">
          <div>
            <span class="pm-status-chip">{{ t('dashboard.workspaceTitle') }}</span>
            <strong>{{ aiStatusLabel }}</strong>
            <p>{{ t('dashboard.workspaceDesc') }}</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">{{ t('common.createProject') }}</el-button>
        </div>
      </div>
    </section>

    <el-alert
      class="pm-premium-alert"
      :title="t('dashboard.trialTitle')"
      type="warning"
      show-icon
      :closable="false"
      :description="t('dashboard.trialDesc')"
    />

    <section v-if="showOnboarding" class="panel onboarding-panel">
      <div class="panel-title">
        <div>
          <p class="eyebrow">{{ t('dashboard.onboarding.eyebrow') }}</p>
          <h3>{{ t('dashboard.onboarding.title') }}</h3>
          <p class="muted">{{ t('dashboard.onboarding.description') }}</p>
        </div>
        <span class="pm-status-chip">{{ t('dashboard.onboarding.badge') }}</span>
      </div>
      <div class="panel-body">
        <ol class="onboarding-steps">
          <li v-for="(step, index) in onboardingSteps" :key="step.title">
            <span>{{ index + 1 }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <p>{{ step.description }}</p>
            </div>
          </li>
        </ol>
        <div class="onboarding-actions">
          <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">
            {{ t('dashboard.onboarding.create') }}
          </el-button>
          <el-button :icon="Coin" @click="router.push('/credits')">
            {{ t('dashboard.onboarding.credits') }}
          </el-button>
          <el-button @click="scrollToDemo">{{ t('dashboard.onboarding.demo') }}</el-button>
        </div>
      </div>
    </section>

    <section class="metric-grid dashboard-metric-grid" v-loading="loading">
      <button
        v-for="metric in metrics"
        :key="metric.label"
        class="metric-card dashboard-metric pm-premium-card pm-hover-lift"
        :class="{ clickable: metric.path }"
        type="button"
        @click="handleMetricClick(metric.path)"
      >
        <i :class="['dashboard-metric-accent', metric.tone]" />
        <span>{{ metric.label }}</span>
        <strong :class="{ 'small-value': String(metric.value).length > 8 }">{{ metric.value }}</strong>
      </button>
    </section>

    <section class="quick-grid">
      <button v-for="entry in quickEntries" :key="entry.title" class="quick-entry pm-premium-card pm-hover-lift" @click="router.push(entry.path)">
        <el-icon :size="22">
          <component :is="entry.icon" />
        </el-icon>
        <span>
          <strong>{{ entry.title }}</strong>
          <small>{{ entry.description }}</small>
        </span>
      </button>
    </section>

    <section class="panel" v-loading="loading">
      <div class="panel-title">
        <div>
          <h3>{{ t('dashboard.recentTitle') }}</h3>
          <p class="muted">{{ t('dashboard.recentDesc') }}</p>
        </div>
      </div>
      <div class="panel-body">
        <el-tabs>
          <el-tab-pane :label="t('dashboard.recent.projects')" name="projects">
            <el-table v-if="recentProjects.length" :data="recentProjects" stripe>
              <el-table-column prop="name" :label="t('common.projectName')" min-width="160" />
              <el-table-column prop="techStack" :label="t('common.techStack')" min-width="180" show-overflow-tooltip />
              <el-table-column prop="status" :label="t('common.status')" width="130">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'PENDING' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
              <el-table-column :label="t('common.operation')" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="router.push(`/projects/${row.id}`)">{{ t('common.view') }}</el-button>
                </template>
              </el-table-column>
            </el-table>

            <EmptyState
              v-else
              :title="t('dashboard.emptyTitle')"
              :description="t('dashboard.emptyDesc')"
            >
              <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">{{ t('common.createProject') }}</el-button>
            </EmptyState>
          </el-tab-pane>

          <el-tab-pane :label="t('dashboard.recent.reports')" name="reports">
            <el-table v-if="recentReports.length" :data="recentReports" stripe>
              <el-table-column prop="projectName" :label="t('common.projectName')" min-width="170" show-overflow-tooltip />
              <el-table-column :label="t('common.score')" width="130">
                <template #default="{ row }">{{ formatScore(row.healthScore) }}</template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="130">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'FINISHED' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
              <el-table-column :label="t('common.operation')" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="router.push(`/reports/${row.reportId}`)">{{ t('common.view') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
            <EmptyState v-else :title="t('dashboard.noReportsTitle')" :description="t('dashboard.noReportsDesc')">
              <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">
                {{ t('dashboard.noReportsAction') }}
              </el-button>
            </EmptyState>
          </el-tab-pane>

          <el-tab-pane :label="t('dashboard.recent.interviews')" name="interviews">
            <el-table v-if="recentInterviews.length" :data="recentInterviews" stripe>
              <el-table-column prop="projectName" :label="t('common.projectName')" min-width="170" show-overflow-tooltip />
              <el-table-column :label="t('dashboard.interviewScore')" width="130">
                <template #default="{ row }">{{ formatScore(row.totalScore) }}</template>
              </el-table-column>
              <el-table-column :label="t('dashboard.interviewQuestions')" min-width="150">
                <template #default="{ row }">{{ row.answeredCount }} / {{ row.questionCount }}</template>
              </el-table-column>
              <el-table-column prop="status" :label="t('common.status')" width="130">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'RUNNING' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
              <el-table-column :label="t('common.operation')" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="router.push(`/interview?sessionId=${row.sessionId}`)">{{ t('common.view') }}</el-button>
                </template>
              </el-table-column>
            </el-table>
            <EmptyState v-else :title="t('dashboard.noInterviewsTitle')" :description="t('dashboard.noInterviewsDesc')">
              <el-button type="primary" @click="router.push('/interview')">
                {{ t('dashboard.noInterviewsAction') }}
              </el-button>
            </EmptyState>
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>

    <section id="dashboard-demo-flow" class="panel">
      <div class="panel-body">
        <DemoWorkflow compact />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChatDotRound, Coin, MagicStick, Plus } from '@element-plus/icons-vue'

import { getAiStatus } from '@/api/ai'
import { getDashboardSummary } from '@/api/dashboard'
import DemoWorkflow from '@/components/DemoWorkflow.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import type { AiStatus, DashboardSummary, InterviewSessionListItem, Project, ReportListItem } from '@/types/api'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const summary = ref<DashboardSummary>()
const aiStatus = ref<AiStatus | null>(null)

const aiStatusLabel = computed(() =>
  aiStatus.value?.enabled && aiStatus.value?.configured ? t('dashboard.aiConfigured') : t('dashboard.aiFallback')
)

const heroChips = computed(() => [
  t('dashboard.heroChips.evidence'),
  t('dashboard.heroChips.qa'),
  t('dashboard.heroChips.beta')
])

const metrics = computed(() => [
  { label: t('dashboard.metrics.projectCount'), value: summary.value?.projectCount ?? 0, tone: 'blue', path: '/projects' },
  { label: t('dashboard.metrics.reports'), value: summary.value?.reportCount ?? 0, tone: 'amber', path: '/reports' },
  { label: t('dashboard.metrics.interviews'), value: summary.value?.interviewSessionCount ?? 0, tone: 'blue', path: '/interviews' },
  { label: t('dashboard.metrics.credits'), value: summary.value?.creditBalance ?? userStore.remainingCredits, tone: 'teal', path: '/credits' },
  { label: t('dashboard.metrics.aiStatus'), value: aiStatusLabel.value, tone: 'green' }
])

const recentProjects = computed<Project[]>(() => summary.value?.recentProjects || [])

const recentReports = computed<ReportListItem[]>(() => summary.value?.recentReports || [])

const recentInterviews = computed<InterviewSessionListItem[]>(() => summary.value?.recentInterviews || [])
const showOnboarding = computed(() => !loading.value && (summary.value?.projectCount ?? 0) === 0)

const onboardingSteps = computed(() => [
  {
    title: t('dashboard.onboarding.steps.create.title'),
    description: t('dashboard.onboarding.steps.create.description')
  },
  {
    title: t('dashboard.onboarding.steps.upload.title'),
    description: t('dashboard.onboarding.steps.upload.description')
  },
  {
    title: t('dashboard.onboarding.steps.scan.title'),
    description: t('dashboard.onboarding.steps.scan.description')
  },
  {
    title: t('dashboard.onboarding.steps.ai.title'),
    description: t('dashboard.onboarding.steps.ai.description')
  }
])

const quickEntries = computed(() => [
  {
    title: t('dashboard.quick.create.title'),
    description: t('dashboard.quick.create.description'),
    path: '/projects/create',
    icon: Plus
  },
  {
    title: t('dashboard.quick.hallucination.title'),
    description: t('dashboard.quick.hallucination.description'),
    path: '/hallucination',
    icon: MagicStick
  },
  {
    title: t('dashboard.quick.interview.title'),
    description: t('dashboard.quick.interview.description'),
    path: '/interview',
    icon: ChatDotRound
  },
  {
    title: t('dashboard.quick.credits.title'),
    description: t('dashboard.quick.credits.description'),
    path: '/credits',
    icon: Coin
  }
])

function statusTagType(status?: string) {
  const statusMap: Record<string, 'info' | 'primary' | 'success' | 'danger' | 'warning'> = {
    PENDING: 'info',
    ANALYZING: 'primary',
    RUNNING: 'primary',
    FINISHED: 'success',
    SUCCESS: 'success',
    FAILED: 'danger'
  }

  return statusMap[status || 'PENDING'] || 'info'
}

function formatScore(score?: number) {
  return Number.isFinite(score) ? Math.round(Number(score)) : '-'
}

function handleMetricClick(path?: string) {
  if (path) {
    router.push(path)
  }
}

function scrollToDemo() {
  document.querySelector('#dashboard-demo-flow')?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    block: 'start'
  })
}

async function loadDashboard() {
  loading.value = true
  try {
    const [dashboardSummary, status] = await Promise.all([
      getDashboardSummary(),
      getAiStatus().catch(() => null)
    ])
    summary.value = dashboardSummary
    aiStatus.value = status
    userStore.updateCredits(dashboardSummary.creditBalance || 0)
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-hero-panel {
  overflow: hidden;
}

.dashboard-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 380px);
  align-items: center;
  gap: 24px;
  overflow: hidden;
  border-radius: 8px;
  background:
    radial-gradient(circle at 8% 0%, rgba(20, 184, 166, 0.18), transparent 34%),
    radial-gradient(circle at 82% 4%, rgba(31, 111, 235, 0.18), transparent 36%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 251, 255, 0.7)),
    rgba(255, 255, 255, 0.86);
}

.dashboard-hero::after {
  position: absolute;
  right: -12%;
  bottom: -70%;
  width: 48%;
  height: 180%;
  background: linear-gradient(135deg, transparent, rgba(31, 111, 235, 0.08), transparent);
  content: "";
  transform: rotate(18deg);
}

.dashboard-hero > * {
  position: relative;
  z-index: 1;
}

.dashboard-hero h2 {
  margin: 8px 0;
  color: #111827;
  font-size: clamp(30px, 4vw, 42px);
  line-height: 1.08;
}

.dashboard-hero p:last-child {
  max-width: 620px;
}

.dashboard-hero-chips {
  margin-top: 18px;
}

.dashboard-hero-card {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 18px;
  padding: 18px;
  border: 1px solid rgba(214, 224, 236, 0.82);
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.82), rgba(248, 251, 255, 0.64)),
    rgba(255, 255, 255, 0.72);
  box-shadow: 0 22px 56px rgba(31, 111, 235, 0.1);
  backdrop-filter: blur(16px);
}

.dashboard-hero-card span {
  text-transform: uppercase;
}

.dashboard-hero-card strong {
  display: block;
  margin-top: 8px;
  color: #111827;
  font-size: 24px;
  line-height: 1.2;
}

.dashboard-hero-card p {
  margin: 8px 0 0;
  color: var(--pm-muted);
  line-height: 1.7;
}

.dashboard-metric-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.onboarding-panel {
  overflow: hidden;
}

.onboarding-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.onboarding-steps li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 15px;
  border: 1px solid rgba(223, 230, 240, 0.9);
  border-radius: 8px;
  background: #fbfdff;
}

.onboarding-steps li > span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--pm-primary), var(--pm-teal));
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
}

.onboarding-steps strong {
  color: var(--pm-ink);
}

.onboarding-steps p {
  margin: 6px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.onboarding-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.dashboard-metric {
  position: relative;
  overflow: hidden;
  min-height: 150px;
  width: 100%;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: default;
}

button.dashboard-metric {
  appearance: none;
}

.dashboard-metric.clickable {
  cursor: pointer;
}

.dashboard-metric.clickable:focus-visible {
  outline: 3px solid rgba(31, 111, 235, 0.24);
  outline-offset: 3px;
}

.dashboard-metric::after {
  position: absolute;
  inset: auto 14px 12px auto;
  width: 46px;
  height: 46px;
  border-radius: 999px;
  background: rgba(31, 111, 235, 0.08);
  content: "";
}

.dashboard-metric-accent {
  display: block;
  width: 28px;
  height: 3px;
  margin-bottom: 14px;
  border-radius: 999px;
  background: var(--pm-primary);
}

.dashboard-metric-accent.teal {
  background: var(--pm-teal);
}

.dashboard-metric-accent.green {
  background: var(--pm-green);
}

.dashboard-metric-accent.amber {
  background: var(--pm-amber);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.quick-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 86px;
  padding: 16px;
  border: 1px solid rgba(223, 230, 240, 0.92);
  border-radius: 8px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.quick-entry :deep(.el-icon) {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(31, 111, 235, 0.12), rgba(20, 184, 166, 0.1));
  color: var(--pm-primary);
}

.quick-entry span {
  position: relative;
  z-index: 1;
}

.quick-entry strong,
.quick-entry small {
  display: block;
}

.quick-entry small {
  margin-top: 4px;
  color: var(--pm-muted);
}

@media (max-width: 920px) {
  .dashboard-hero,
  .dashboard-metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .onboarding-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .dashboard-hero,
  .dashboard-metric-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .onboarding-steps {
    grid-template-columns: 1fr;
  }

  .dashboard-hero-card {
    width: 100%;
  }
}
</style>
