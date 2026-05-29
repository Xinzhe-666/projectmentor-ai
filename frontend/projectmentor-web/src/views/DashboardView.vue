<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-body dashboard-hero">
        <div>
          <p class="eyebrow">Welcome back</p>
          <h2>你好，{{ userStore.userInfo?.username || '同学' }}</h2>
          <p class="muted">
            把 GitHub 项目、README 和代码证据放进来，让 AI 先替面试官追问一轮。
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">创建项目</el-button>
      </div>
    </section>

    <el-alert
      title="试用提示"
      type="warning"
      show-icon
      :closable="false"
      description="请勿上传真实商业机密、真实密钥或公司内部代码。ProjectMentor AI 会基于你提供的 README 和项目文件生成辅助分析，结论仅供学习、项目复盘和面试准备参考。"
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
          <h3>最近项目</h3>
          <p class="muted">按创建时间展示最近 5 个项目。</p>
        </div>
        <el-button @click="router.push('/projects')">查看全部</el-button>
      </div>
      <div class="panel-body">
        <el-table v-if="recentProjects.length" :data="recentProjects" stripe>
          <el-table-column prop="name" label="项目名称" min-width="160" />
          <el-table-column prop="techStack" label="技术栈" min-width="180" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'PENDING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="180" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="router.push(`/projects/${row.id}`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState
          v-else
          title="还没有项目"
          description="创建第一个项目后，可以继续保存 README、上传 ZIP 并生成审计报告。"
        >
          <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">创建项目</el-button>
        </EmptyState>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Coin, MagicStick, Plus } from '@element-plus/icons-vue'

import { getAiStatus } from '@/api/ai'
import { getMyCredits } from '@/api/credit'
import { listProjects } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import type { AiStatus, Project } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const projects = ref<Project[]>([])
const aiStatus = ref<AiStatus | null>(null)

const aiStatusLabel = computed(() =>
  aiStatus.value?.enabled && aiStatus.value?.configured ? 'AI 增强已配置' : '规则版降级中'
)

const metrics = computed(() => [
  { label: '项目数量', value: projects.value.length },
  { label: '剩余额度', value: userStore.remainingCredits },
  { label: 'AI 状态', value: aiStatusLabel.value },
  { label: '审计报告', value: '--' },
  { label: '面试会话', value: '--' }
])

const recentProjects = computed(() =>
  [...projects.value]
    .sort((a, b) => (b.createTime || '').localeCompare(a.createTime || ''))
    .slice(0, 5)
)

const quickEntries = [
  {
    title: '创建项目',
    description: '录入项目基础信息',
    path: '/projects/create',
    icon: Plus
  },
  {
    title: 'AI 幻觉检测',
    description: '检查 AI 描述是否过度包装',
    path: '/hallucination',
    icon: MagicStick
  },
  {
    title: '模拟面试',
    description: '围绕项目细节连续追问',
    path: '/interview',
    icon: ChatDotRound
  },
  {
    title: '查看额度',
    description: '跟踪报告生成消耗',
    path: '/credits',
    icon: Coin
  }
]

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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
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
  background: rgba(255, 255, 255, 0.94);
  color: inherit;
  text-align: left;
  box-shadow: var(--pm-shadow-soft);
  cursor: pointer;
}

.quick-entry:hover {
  border-color: rgba(31, 111, 235, 0.28);
  transform: translateY(-1px);
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
