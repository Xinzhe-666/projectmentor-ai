<template>
  <div class="page-stack">
    <section class="page-title">
      <div>
        <p class="eyebrow">Admin</p>
        <h2>管理员后台</h2>
        <p class="muted">用于查看 ProjectMentor AI 当前试用版的用户、项目、报告和问答运行情况。</p>
      </div>
      <el-tag v-if="adminMe?.admin" type="success" effect="light">只读看板</el-tag>
    </section>

    <el-alert
      title="当前管理员后台为只读 MVP，仅用于查看试用版运行情况。请勿在页面中展示或导出敏感信息。"
      type="warning"
      show-icon
      :closable="false"
    />

    <section v-if="checked && !isAdmin" class="panel">
      <div class="panel-body admin-denied">
        <h3>无权限访问管理员后台</h3>
        <p class="muted">当前账号未配置为管理员，无法查看系统运行数据。</p>
      </div>
    </section>

    <template v-else-if="checked && isAdmin">
      <section class="metric-grid" v-loading="loading">
        <div class="metric-card" v-for="metric in metrics" :key="metric.label">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </section>

      <section class="admin-table-grid" v-loading="loading">
        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>最近用户</h3>
              <p class="muted">默认展示最近 10 条注册记录</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentUsers" stripe empty-text="暂无用户记录">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
              <el-table-column prop="nickname" label="昵称" min-width="130" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" min-width="170" />
            </el-table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>最近项目</h3>
              <p class="muted">仅展示项目摘要，不展示源码内容</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentProjects" stripe empty-text="暂无项目记录">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="userId" label="用户ID" width="100" />
              <el-table-column prop="name" label="项目名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="techStack" label="技术栈" min-width="160" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'PENDING' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="170" />
            </el-table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>最近报告</h3>
              <p class="muted">展示报告分数与归属项目</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentReports" stripe empty-text="暂无报告记录">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="projectId" label="项目ID" width="100" />
              <el-table-column prop="userId" label="用户ID" width="100" />
              <el-table-column prop="totalScore" label="总分" width="100">
                <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="170" />
            </el-table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>最近问答</h3>
              <p class="muted">仅展示问题摘要，不返回证据 JSON</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentQa" stripe empty-text="暂无问答记录">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="userId" label="用户ID" width="100" />
              <el-table-column prop="projectId" label="项目ID" width="100" />
              <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
              <el-table-column prop="aiUsed" label="AI" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.aiUsed ? 'success' : 'info'" effect="light">
                    {{ row.aiUsed ? '已参与' : '未参与' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="170" />
            </el-table>
          </div>
        </article>
      </section>
    </template>

    <section v-else class="panel" v-loading="loading">
      <div class="panel-body admin-denied">
        <h3>正在校验管理员权限</h3>
        <p class="muted">请稍候。</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import {
  getAdminMe,
  getAdminRecentProjects,
  getAdminRecentQa,
  getAdminRecentReports,
  getAdminRecentUsers,
  getAdminStats
} from '@/api/admin'
import type {
  AdminMe,
  AdminRecentProject,
  AdminRecentQa,
  AdminRecentReport,
  AdminRecentUser,
  AdminStats
} from '@/types/api'

const loading = ref(false)
const checked = ref(false)
const adminMe = ref<AdminMe | null>(null)
const stats = ref<AdminStats | null>(null)
const recentUsers = ref<AdminRecentUser[]>([])
const recentProjects = ref<AdminRecentProject[]>([])
const recentReports = ref<AdminRecentReport[]>([])
const recentQa = ref<AdminRecentQa[]>([])

const isAdmin = computed(() => Boolean(adminMe.value?.admin))

const metrics = computed(() => [
  { label: '用户数', value: stats.value?.userCount ?? '-' },
  { label: '项目数', value: stats.value?.projectCount ?? '-' },
  { label: '报告数', value: stats.value?.reportCount ?? '-' },
  { label: '项目问答数', value: stats.value?.qaCount ?? '-' },
  { label: '分享报告数', value: stats.value?.shareCount ?? '-' },
  { label: '今日新增用户', value: stats.value?.todayUserCount ?? '-' },
  { label: '今日新增项目', value: stats.value?.todayProjectCount ?? '-' },
  { label: '今日新增报告', value: stats.value?.todayReportCount ?? '-' },
  { label: '今日问答数', value: stats.value?.todayQaCount ?? '-' }
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

async function loadAdminDashboard() {
  loading.value = true
  try {
    adminMe.value = await getAdminMe()
    checked.value = true

    if (!adminMe.value.admin) {
      return
    }

    const [statsData, users, projects, reports, qa] = await Promise.all([
      getAdminStats(),
      getAdminRecentUsers(),
      getAdminRecentProjects(),
      getAdminRecentReports(),
      getAdminRecentQa()
    ])

    stats.value = statsData
    recentUsers.value = users
    recentProjects.value = projects
    recentReports.value = reports
    recentQa.value = qa
  } catch {
    checked.value = true
    adminMe.value = { admin: false }
  } finally {
    loading.value = false
  }
}

onMounted(loadAdminDashboard)
</script>

<style scoped>
.admin-denied {
  text-align: center;
}

.admin-denied h3 {
  margin: 0 0 8px;
  font-size: 22px;
}

.admin-table-grid {
  display: grid;
  gap: 18px;
}

.admin-table-grid .panel-body {
  overflow: auto;
}
</style>
