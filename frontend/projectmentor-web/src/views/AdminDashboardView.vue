<template>
  <div class="page-stack">
    <section class="page-title">
      <div>
        <p class="eyebrow">Admin</p>
        <h2>管理员后台</h2>
        <p class="muted">用于查看 ProjectMentor AI 当前试用版的用户、项目、报告和问答运行情况。</p>
      </div>
      <el-tag v-if="adminMe?.admin" type="success" effect="light">管理员</el-tag>
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

      <section class="panel">
        <div class="panel-title">
          <div>
            <h3>额度管理</h3>
            <p class="muted">搜索用户、查看余额，并为指定用户发放额度。</p>
          </div>
        </div>
        <div class="panel-body admin-credit-panel">
          <div class="admin-credit-toolbar">
            <el-input
              v-model="creditKeyword"
              clearable
              placeholder="输入邮箱 / 昵称 / 用户 ID"
              @keyup.enter="loadCreditUsers"
            />
            <el-button type="primary" :loading="creditLoading" @click="loadCreditUsers">搜索</el-button>
          </div>

          <el-table :data="creditUsers" stripe v-loading="creditLoading" empty-text="暂无用户记录">
            <el-table-column prop="userId" label="用户 ID" width="100" />
            <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
            <el-table-column prop="nickname" label="昵称" min-width="130" show-overflow-tooltip />
            <el-table-column prop="creditBalance" label="当前额度" width="120" />
            <el-table-column prop="createTime" label="注册时间" min-width="170" />
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openGrantDialog(row)">发放额度</el-button>
                <el-button text @click="openDetailDialog(row)">查看流水</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <h3>反馈管理</h3>
            <p class="muted">查看站内反馈，按类型、状态和关键词筛选，并更新处理状态。</p>
          </div>
        </div>
        <div class="panel-body admin-feedback-panel">
          <div class="feedback-toolbar">
            <el-select v-model="feedbackTypeFilter" clearable placeholder="类型">
              <el-option v-for="option in feedbackTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-select v-model="feedbackStatusFilter" clearable placeholder="状态">
              <el-option v-for="option in feedbackStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-input
              v-model="feedbackKeyword"
              clearable
              placeholder="搜索内容 / 联系方式 / 邮箱"
              @keyup.enter="handleFeedbackSearch"
            />
            <el-button type="primary" :loading="feedbackLoading" @click="handleFeedbackSearch">筛选</el-button>
            <el-button @click="resetFeedbackFilters">重置</el-button>
          </div>

          <el-table :data="feedbackRecords" stripe v-loading="feedbackLoading" empty-text="暂无反馈">
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="userEmail" label="用户邮箱" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">{{ row.userEmail || '-' }}</template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="140">
              <template #default="{ row }">{{ feedbackTypeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="content" label="内容摘要" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ feedbackSummary(row.content) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="feedbackStatusTagType(row.status)" effect="light">
                  {{ feedbackStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" min-width="170" />
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openFeedbackDialog(row)">查看 / 更新</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="feedback-pagination">
            <el-pagination
              v-if="feedbackTotal > 0"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50]"
              :current-page="feedbackPage"
              :page-size="feedbackSize"
              :total="feedbackTotal"
              @size-change="handleFeedbackSizeChange"
              @current-change="handleFeedbackPageChange"
            />
          </div>
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

      <el-dialog v-model="creditDialogVisible" title="额度发放" width="720px">
        <div v-if="selectedCreditUser" class="credit-dialog-stack">
          <div class="credit-user-summary">
            <span>用户 ID：{{ selectedCreditUser.userId }}</span>
            <span>邮箱：{{ selectedCreditUser.email }}</span>
            <span>昵称：{{ selectedCreditUser.nickname || '-' }}</span>
            <span>当前额度：{{ creditDetail?.creditBalance ?? selectedCreditUser.creditBalance }}</span>
          </div>

          <el-form label-width="92px" @submit.prevent>
            <el-form-item label="发放额度">
              <el-input-number
                v-model="creditForm.amount"
                :min="1"
                :max="10000"
                :step="1"
                step-strictly
                controls-position="right"
              />
            </el-form-item>
            <el-form-item label="发放原因">
              <el-input
                v-model="creditForm.reason"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="例如：测试用户补充额度"
              />
            </el-form-item>
          </el-form>

          <div class="credit-log-block">
            <h4>最近流水</h4>
            <el-table
              :data="creditDetail?.recentTransactions || []"
              size="small"
              stripe
              empty-text="暂无额度流水"
            >
              <el-table-column prop="changeAmount" label="变动额度" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.changeAmount > 0 ? 'success' : 'warning'" effect="light">
                    {{ row.changeAmount > 0 ? `+${row.changeAmount}` : row.changeAmount }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="140" show-overflow-tooltip />
              <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
              <el-table-column prop="createTime" label="时间" min-width="170" />
            </el-table>
          </div>
        </div>

        <template #footer>
          <el-button @click="creditDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="grantLoading" @click="handleGrantCredit">确认发放</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="feedbackDialogVisible" title="反馈详情" width="760px">
        <div v-if="selectedFeedback" v-loading="feedbackDetailLoading" class="feedback-detail-stack">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="反馈 ID">{{ selectedFeedback.id }}</el-descriptions-item>
            <el-descriptions-item label="用户邮箱">{{ selectedFeedback.userEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ feedbackTypeLabel(selectedFeedback.type) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="feedbackStatusTagType(selectedFeedback.status)" effect="light">
                {{ feedbackStatusLabel(selectedFeedback.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="联系方式">{{ selectedFeedback.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ selectedFeedback.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源页面" :span="2">
              <el-link
                v-if="selectedFeedback.pageUrl"
                :href="selectedFeedback.pageUrl"
                target="_blank"
                type="primary"
              >
                {{ selectedFeedback.pageUrl }}
              </el-link>
              <span v-else>-</span>
            </el-descriptions-item>
          </el-descriptions>

          <div class="feedback-content-block">
            <h4>完整内容</h4>
            <p>{{ selectedFeedback.content }}</p>
          </div>

          <el-form label-width="96px" @submit.prevent>
            <el-form-item label="处理状态">
              <el-select v-model="feedbackForm.status">
                <el-option v-for="option in feedbackStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="管理员备注">
              <el-input
                v-model="feedbackForm.adminNote"
                type="textarea"
                maxlength="1000"
                show-word-limit
                :rows="4"
                placeholder="记录处理进展、判断或暂不处理原因"
              />
            </el-form-item>
          </el-form>
        </div>

        <template #footer>
          <el-button @click="feedbackDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="feedbackSaving" @click="handleUpdateFeedbackStatus">保存状态</el-button>
        </template>
      </el-dialog>
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
import { ElMessage } from 'element-plus'

import {
  getAdminCreditUserDetail,
  getAdminFeedbackDetail,
  getAdminFeedbackList,
  getAdminMe,
  getAdminRecentProjects,
  getAdminRecentQa,
  getAdminRecentReports,
  getAdminRecentUsers,
  getAdminStats,
  grantAdminCredit,
  searchAdminCreditUsers,
  updateAdminFeedbackStatus
} from '@/api/admin'
import type {
  AdminCreditUser,
  AdminCreditUserDetail,
  AdminFeedback,
  AdminFeedbackDetail,
  AdminMe,
  AdminRecentProject,
  AdminRecentQa,
  AdminRecentReport,
  AdminRecentUser,
  AdminStats,
  FeedbackStatus,
  FeedbackType
} from '@/types/api'

const loading = ref(false)
const checked = ref(false)
const adminMe = ref<AdminMe | null>(null)
const stats = ref<AdminStats | null>(null)
const recentUsers = ref<AdminRecentUser[]>([])
const recentProjects = ref<AdminRecentProject[]>([])
const recentReports = ref<AdminRecentReport[]>([])
const recentQa = ref<AdminRecentQa[]>([])
const creditKeyword = ref('')
const creditUsers = ref<AdminCreditUser[]>([])
const creditLoading = ref(false)
const creditDialogVisible = ref(false)
const selectedCreditUser = ref<AdminCreditUser | null>(null)
const creditDetail = ref<AdminCreditUserDetail | null>(null)
const grantLoading = ref(false)
const creditForm = ref({
  amount: 1,
  reason: ''
})
const feedbackTypeFilter = ref<FeedbackType | ''>('')
const feedbackStatusFilter = ref<FeedbackStatus | ''>('')
const feedbackKeyword = ref('')
const feedbackPage = ref(1)
const feedbackSize = ref(10)
const feedbackTotal = ref(0)
const feedbackRecords = ref<AdminFeedback[]>([])
const feedbackLoading = ref(false)
const feedbackDialogVisible = ref(false)
const feedbackDetailLoading = ref(false)
const feedbackSaving = ref(false)
const selectedFeedback = ref<AdminFeedbackDetail | null>(null)
const feedbackForm = ref({
  status: 'PENDING' as FeedbackStatus,
  adminNote: ''
})

const isAdmin = computed(() => Boolean(adminMe.value?.admin))

const feedbackTypeOptions: Array<{ label: string; value: FeedbackType }> = [
  { label: '功能 Bug', value: 'BUG' },
  { label: '体验建议', value: 'UX' },
  { label: '审计不准确', value: 'AUDIT_INACCURATE' },
  { label: '问答不准确', value: 'QA_INACCURATE' },
  { label: '面试问题', value: 'INTERVIEW_QUESTION' },
  { label: '上传问题', value: 'UPLOAD' },
  { label: '其他', value: 'OTHER' }
]

const feedbackStatusOptions: Array<{ label: string; value: FeedbackStatus }> = [
  { label: '待处理', value: 'PENDING' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
  { label: '暂不处理', value: 'WONTFIX' }
]

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

function feedbackTypeLabel(type?: string) {
  return feedbackTypeOptions.find((option) => option.value === type)?.label || type || '-'
}

function feedbackStatusLabel(status?: string) {
  return feedbackStatusOptions.find((option) => option.value === status)?.label || status || '-'
}

function feedbackStatusTagType(status?: string) {
  const statusMap: Record<string, 'info' | 'primary' | 'success' | 'danger' | 'warning'> = {
    PENDING: 'info',
    PROCESSING: 'primary',
    RESOLVED: 'success',
    WONTFIX: 'warning'
  }

  return statusMap[status || 'PENDING'] || 'info'
}

function feedbackSummary(content?: string) {
  if (!content) {
    return '-'
  }

  return content.length > 80 ? `${content.slice(0, 80)}...` : content
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
    await Promise.allSettled([
      loadCreditUsers(),
      loadFeedbackList()
    ])
  } catch {
    checked.value = true
    adminMe.value = { admin: false }
  } finally {
    loading.value = false
  }
}

async function loadCreditUsers() {
  if (!isAdmin.value) {
    return
  }

  creditLoading.value = true
  try {
    creditUsers.value = await searchAdminCreditUsers(creditKeyword.value)
  } finally {
    creditLoading.value = false
  }
}

async function loadFeedbackList() {
  if (!isAdmin.value) {
    return
  }

  feedbackLoading.value = true
  try {
    const data = await getAdminFeedbackList({
      type: feedbackTypeFilter.value || undefined,
      status: feedbackStatusFilter.value || undefined,
      keyword: feedbackKeyword.value.trim() || undefined,
      page: feedbackPage.value,
      size: feedbackSize.value
    })
    feedbackRecords.value = data.records
    feedbackTotal.value = data.total
    feedbackPage.value = data.page
    feedbackSize.value = data.size
  } finally {
    feedbackLoading.value = false
  }
}

function handleFeedbackSearch() {
  feedbackPage.value = 1
  loadFeedbackList()
}

function resetFeedbackFilters() {
  feedbackTypeFilter.value = ''
  feedbackStatusFilter.value = ''
  feedbackKeyword.value = ''
  feedbackPage.value = 1
  loadFeedbackList()
}

function handleFeedbackSizeChange(size: number) {
  feedbackSize.value = size
  feedbackPage.value = 1
  loadFeedbackList()
}

function handleFeedbackPageChange(page: number) {
  feedbackPage.value = page
  loadFeedbackList()
}

async function openFeedbackDialog(row: AdminFeedback) {
  selectedFeedback.value = row
  feedbackForm.value = {
    status: row.status,
    adminNote: row.adminNote || ''
  }
  feedbackDialogVisible.value = true
  feedbackDetailLoading.value = true

  try {
    const detail = await getAdminFeedbackDetail(row.id)
    selectedFeedback.value = detail
    feedbackForm.value = {
      status: detail.status,
      adminNote: detail.adminNote || ''
    }
  } finally {
    feedbackDetailLoading.value = false
  }
}

async function handleUpdateFeedbackStatus() {
  if (!selectedFeedback.value) {
    return
  }

  feedbackSaving.value = true
  try {
    const detail = await updateAdminFeedbackStatus(selectedFeedback.value.id, {
      status: feedbackForm.value.status,
      adminNote: feedbackForm.value.adminNote.trim() || undefined
    })
    selectedFeedback.value = detail
    ElMessage.success('反馈状态已更新')
    feedbackDialogVisible.value = false
    await loadFeedbackList()
  } finally {
    feedbackSaving.value = false
  }
}

async function openGrantDialog(user: AdminCreditUser) {
  selectedCreditUser.value = user
  creditForm.value = {
    amount: 1,
    reason: ''
  }
  creditDialogVisible.value = true
  await loadCreditDetail(user.userId)
}

async function openDetailDialog(user: AdminCreditUser) {
  selectedCreditUser.value = user
  creditForm.value = {
    amount: 1,
    reason: ''
  }
  creditDialogVisible.value = true
  await loadCreditDetail(user.userId)
}

async function loadCreditDetail(userId: number) {
  creditDetail.value = await getAdminCreditUserDetail(userId)
}

async function handleGrantCredit() {
  if (!selectedCreditUser.value) {
    return
  }

  if (!Number.isInteger(creditForm.value.amount) || creditForm.value.amount <= 0) {
    ElMessage.warning('发放额度必须为正整数')
    return
  }

  const reason = creditForm.value.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning('请填写至少 2 个字符的发放原因')
    return
  }

  grantLoading.value = true
  try {
    const result = await grantAdminCredit({
      userId: selectedCreditUser.value.userId,
      amount: creditForm.value.amount,
      reason
    })
    ElMessage.success('额度已发放')
    selectedCreditUser.value.creditBalance = result.newBalance
    await Promise.all([
      loadCreditDetail(selectedCreditUser.value.userId),
      loadCreditUsers()
    ])
    creditForm.value.amount = 1
    creditForm.value.reason = ''
  } finally {
    grantLoading.value = false
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

.admin-credit-panel {
  display: grid;
  gap: 16px;
}

.admin-credit-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 360px) auto;
  justify-content: flex-start;
  gap: 10px;
}

.admin-feedback-panel {
  display: grid;
  gap: 16px;
}

.feedback-toolbar {
  display: grid;
  grid-template-columns: 150px 150px minmax(220px, 360px) auto auto;
  justify-content: flex-start;
  gap: 10px;
}

.feedback-pagination {
  display: flex;
  justify-content: flex-end;
}

.feedback-detail-stack {
  display: grid;
  gap: 18px;
}

.feedback-content-block {
  display: grid;
  gap: 8px;
}

.feedback-content-block h4 {
  margin: 0;
}

.feedback-content-block p {
  margin: 0;
  padding: 12px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #f8fbff;
  color: #344054;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.credit-dialog-stack {
  display: grid;
  gap: 18px;
}

.credit-user-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #f8fbff;
  color: #344054;
  font-size: 13px;
}

.credit-log-block h4 {
  margin: 0 0 10px;
}

@media (max-width: 720px) {
  .admin-credit-toolbar,
  .feedback-toolbar,
  .credit-user-summary {
    grid-template-columns: 1fr;
  }
}
</style>
