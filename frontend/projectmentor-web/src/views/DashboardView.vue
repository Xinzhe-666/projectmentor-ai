<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-body dashboard-hero">
        <div>
          <p class="eyebrow">{{ t('dashboard.eyebrow') }}</p>
          <h2>{{ t('dashboard.greeting', { name: userStore.userInfo?.username || t('common.classmate') }) }}</h2>
          <p class="muted">
            {{ t('dashboard.subtitle') }}
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">{{ t('common.createProject') }}</el-button>
      </div>
    </section>

    <el-alert
      :title="t('dashboard.trialTitle')"
      type="warning"
      show-icon
      :closable="false"
      :description="t('dashboard.trialDesc')"
    />

    <section class="metric-grid" v-loading="loading">
      <div class="metric-card" v-for="metric in metrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </div>
    </section>

    <section class="quick-grid">
      <button v-for="entry in quickEntries" :key="entry.title" class="quick-entry" @click="router.push(entry.path)">
        <el-icon :size="22">
          <component :is="entry.icon" />
        </el-icon>
        <span>
          <strong>{{ entry.title }}</strong>
          <small>{{ entry.description }}</small>
        </span>
      </button>
    </section>

    <section class="panel">
      <div class="panel-title">
        <div>
          <h3>{{ t('dashboard.recentTitle') }}</h3>
          <p class="muted">{{ t('dashboard.recentDesc') }}</p>
        </div>
        <el-button @click="router.push('/projects')">{{ t('common.viewAll') }}</el-button>
      </div>
      <div class="panel-body">
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
import { getMyCredits } from '@/api/credit'
import { listProjects } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import type { AiStatus, Project } from '@/types/api'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()

const loading = ref(false)
const projects = ref<Project[]>([])
const aiStatus = ref<AiStatus | null>(null)

const aiStatusLabel = computed(() =>
  aiStatus.value?.enabled && aiStatus.value?.configured ? t('dashboard.aiConfigured') : t('dashboard.aiFallback')
)

const metrics = computed(() => [
  { label: t('dashboard.metrics.projectCount'), value: projects.value.length },
  { label: t('dashboard.metrics.credits'), value: userStore.remainingCredits },
  { label: t('dashboard.metrics.aiStatus'), value: aiStatusLabel.value },
  { label: t('dashboard.metrics.reports'), value: '--' },
  { label: t('dashboard.metrics.interviews'), value: '--' }
])

const recentProjects = computed(() =>
  [...projects.value]
    .sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
    .slice(0, 5)
)

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
    FINISHED: 'success',
    FAILED: 'danger'
  }

  return statusMap[status || 'PENDING'] || 'info'
}

async function loadDashboard() {
  loading.value = true
  try {
    const [projectList, creditInfo, status] = await Promise.all([
      listProjects(),
      getMyCredits(),
      getAiStatus().catch(() => null)
    ])
    projects.value = projectList
    aiStatus.value = status
    userStore.updateCredits(creditInfo.remainingCredits)
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(31, 111, 235, 0.08), rgba(20, 184, 166, 0.06)),
    rgba(255, 255, 255, 0.9);
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
  font-size: 30px;
}

.dashboard-hero p:last-child {
  max-width: 620px;
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
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.9)),
    rgba(255, 255, 255, 0.94);
  color: inherit;
  text-align: left;
  box-shadow: var(--pm-shadow-soft);
  cursor: pointer;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.quick-entry:hover {
  border-color: rgba(31, 111, 235, 0.28);
  box-shadow: 0 18px 36px rgba(31, 111, 235, 0.11);
  transform: translateY(-3px);
}

.quick-entry :deep(.el-icon) {
  color: var(--pm-primary);
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
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
