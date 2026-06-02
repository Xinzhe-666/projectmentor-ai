<template>
  <div class="page-stack">
    <section class="page-title">
      <div>
        <p class="eyebrow">{{ t('admin.eyebrow') }}</p>
        <h2>{{ t('admin.title') }}</h2>
        <p class="muted">{{ t('admin.desc') }}</p>
      </div>
      <el-tag v-if="adminMe?.admin" type="success" effect="light">{{ t('admin.role') }}</el-tag>
    </section>

    <el-alert
      :title="t('admin.readonlyNotice')"
      type="warning"
      show-icon
      :closable="false"
    />

    <section v-if="checked && !isAdmin" class="panel">
      <div class="panel-body admin-denied">
        <h3>{{ t('admin.deniedTitle') }}</h3>
        <p class="muted">{{ t('admin.deniedDesc') }}</p>
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
            <h3>{{ t('admin.creditTitle') }}</h3>
            <p class="muted">{{ t('admin.creditDesc') }}</p>
          </div>
        </div>
        <div class="panel-body admin-credit-panel">
          <div class="admin-credit-toolbar">
            <el-input
              v-model="creditKeyword"
              clearable
              :placeholder="t('admin.creditPlaceholder')"
              @keyup.enter="loadCreditUsers"
            />
            <el-button type="primary" :loading="creditLoading" @click="loadCreditUsers">{{ t('common.search') }}</el-button>
          </div>

          <el-table :data="creditUsers" stripe v-loading="creditLoading" :empty-text="t('admin.noUsers')">
            <el-table-column prop="userId" :label="t('common.userId')" width="100" />
            <el-table-column prop="email" :label="t('common.email')" min-width="190" show-overflow-tooltip />
            <el-table-column prop="nickname" :label="t('admin.nickname')" min-width="130" show-overflow-tooltip />
            <el-table-column prop="creditBalance" :label="t('admin.balance')" width="120" />
            <el-table-column prop="createTime" :label="t('admin.registerTime')" min-width="170" />
            <el-table-column :label="t('common.operation')" width="190" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openGrantDialog(row)">{{ t('admin.grantCredit') }}</el-button>
                <el-button text @click="openDetailDialog(row)">{{ t('admin.viewLogs') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">
          <div>
            <h3>{{ t('admin.feedbackTitle') }}</h3>
            <p class="muted">{{ t('admin.feedbackDesc') }}</p>
          </div>
        </div>
        <div class="panel-body admin-feedback-panel">
          <div class="feedback-toolbar">
            <el-select v-model="feedbackTypeFilter" clearable :placeholder="t('admin.typePlaceholder')">
              <el-option v-for="option in feedbackTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-select v-model="feedbackStatusFilter" clearable :placeholder="t('admin.statusPlaceholder')">
              <el-option v-for="option in feedbackStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-input
              v-model="feedbackKeyword"
              clearable
              :placeholder="t('admin.feedbackKeyword')"
              @keyup.enter="handleFeedbackSearch"
            />
            <el-button type="primary" :loading="feedbackLoading" @click="handleFeedbackSearch">{{ t('common.filter') }}</el-button>
            <el-button @click="resetFeedbackFilters">{{ t('common.reset') }}</el-button>
          </div>

          <el-table :data="feedbackRecords" stripe v-loading="feedbackLoading" :empty-text="t('admin.noFeedback')">
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="userEmail" :label="t('admin.userEmail')" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">{{ row.userEmail || '-' }}</template>
            </el-table-column>
            <el-table-column prop="type" :label="t('feedback.type')" width="140">
              <template #default="{ row }">{{ feedbackTypeLabel(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="content" :label="t('admin.contentSummary')" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ feedbackSummary(row.content) }}</template>
            </el-table-column>
            <el-table-column prop="status" :label="t('common.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="feedbackStatusTagType(row.status)" effect="light">
                  {{ feedbackStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="t('admin.submitTime')" min-width="170" />
            <el-table-column :label="t('common.operation')" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openFeedbackDialog(row)">{{ t('admin.updateFeedback') }}</el-button>
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
              <h3>{{ t('admin.recentUsers') }}</h3>
              <p class="muted">{{ t('admin.recentUsersDesc') }}</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentUsers" stripe :empty-text="t('admin.noUsers')">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="email" :label="t('common.email')" min-width="180" show-overflow-tooltip />
              <el-table-column prop="nickname" :label="t('admin.nickname')" min-width="130" show-overflow-tooltip />
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="170" />
            </el-table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('admin.recentProjects') }}</h3>
              <p class="muted">{{ t('admin.recentProjectsDesc') }}</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentProjects" stripe :empty-text="t('admin.noProjects')">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="userId" :label="t('common.userId')" width="100" />
              <el-table-column prop="name" :label="t('common.projectName')" min-width="160" show-overflow-tooltip />
              <el-table-column prop="techStack" :label="t('common.techStack')" min-width="160" show-overflow-tooltip />
              <el-table-column prop="status" :label="t('common.status')" width="120">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'PENDING' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="170" />
            </el-table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('admin.recentReports') }}</h3>
              <p class="muted">{{ t('admin.recentReportsDesc') }}</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentReports" stripe :empty-text="t('admin.noReports')">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="projectId" :label="t('common.projectId')" width="100" />
              <el-table-column prop="userId" :label="t('common.userId')" width="100" />
              <el-table-column prop="totalScore" :label="t('common.totalScore')" width="100">
                <template #default="{ row }">{{ row.totalScore ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="170" />
            </el-table>
          </div>
        </article>

        <article class="panel">
          <div class="panel-title">
            <div>
              <h3>{{ t('admin.recentQa') }}</h3>
              <p class="muted">{{ t('admin.recentQaDesc') }}</p>
            </div>
          </div>
          <div class="panel-body">
            <el-table :data="recentQa" stripe :empty-text="t('admin.noQa')">
              <el-table-column prop="id" label="ID" width="90" />
              <el-table-column prop="userId" :label="t('common.userId')" width="100" />
              <el-table-column prop="projectId" :label="t('common.projectId')" width="100" />
              <el-table-column prop="question" :label="t('admin.question')" min-width="220" show-overflow-tooltip />
              <el-table-column prop="aiUsed" label="AI" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.aiUsed ? 'success' : 'info'" effect="light">
                    {{ row.aiUsed ? t('admin.aiUsed') : t('admin.aiNotUsed') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="t('common.createTime')" min-width="170" />
            </el-table>
          </div>
        </article>
      </section>

      <el-dialog v-model="creditDialogVisible" :title="t('admin.creditDialogTitle')" width="720px">
        <div v-if="selectedCreditUser" class="credit-dialog-stack">
          <div class="credit-user-summary">
            <span>{{ t('common.userId') }}：{{ selectedCreditUser.userId }}</span>
            <span>{{ t('common.email') }}：{{ selectedCreditUser.email }}</span>
            <span>{{ t('admin.nickname') }}：{{ selectedCreditUser.nickname || '-' }}</span>
            <span>{{ t('admin.balance') }}：{{ creditDetail?.creditBalance ?? selectedCreditUser.creditBalance }}</span>
          </div>

          <el-form label-width="92px" @submit.prevent>
            <el-form-item :label="t('admin.grantAmount')">
              <el-input-number
                v-model="creditForm.amount"
                :min="1"
                :max="10000"
                :step="1"
                step-strictly
                controls-position="right"
              />
            </el-form-item>
            <el-form-item :label="t('admin.grantReason')">
              <el-input
                v-model="creditForm.reason"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                :placeholder="t('admin.grantReasonPlaceholder')"
              />
            </el-form-item>
          </el-form>

          <div class="credit-log-block">
            <h4>{{ t('admin.recentLogs') }}</h4>
            <el-table
              :data="creditDetail?.recentTransactions || []"
              size="small"
              stripe
              :empty-text="t('admin.noCreditLogs')"
            >
              <el-table-column prop="changeAmount" :label="t('credits.change')" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.changeAmount > 0 ? 'success' : 'warning'" effect="light">
                    {{ row.changeAmount > 0 ? `+${row.changeAmount}` : row.changeAmount }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="type" :label="t('feedback.type')" width="140" show-overflow-tooltip />
              <el-table-column prop="reason" :label="t('admin.grantReason')" min-width="220" show-overflow-tooltip />
              <el-table-column prop="createTime" :label="t('credits.time')" min-width="170" />
            </el-table>
          </div>
        </div>

        <template #footer>
          <el-button @click="creditDialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="grantLoading" @click="handleGrantCredit">{{ t('admin.confirmGrant') }}</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="feedbackDialogVisible" :title="t('admin.feedbackDialogTitle')" width="760px">
        <div v-if="selectedFeedback" v-loading="feedbackDetailLoading" class="feedback-detail-stack">
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="t('admin.feedbackId')">{{ selectedFeedback.id }}</el-descriptions-item>
            <el-descriptions-item :label="t('admin.userEmail')">{{ selectedFeedback.userEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="t('feedback.type')">{{ feedbackTypeLabel(selectedFeedback.type) }}</el-descriptions-item>
            <el-descriptions-item :label="t('common.status')">
              <el-tag :type="feedbackStatusTagType(selectedFeedback.status)" effect="light">
                {{ feedbackStatusLabel(selectedFeedback.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item :label="t('feedback.contact')">{{ selectedFeedback.contact || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="t('admin.submitTime')">{{ selectedFeedback.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item :label="t('feedback.pageUrl')" :span="2">
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
            <h4>{{ t('admin.fullContent') }}</h4>
            <p>{{ selectedFeedback.content }}</p>
          </div>

          <el-form label-width="96px" @submit.prevent>
            <el-form-item :label="t('admin.processStatus')">
              <el-select v-model="feedbackForm.status">
                <el-option v-for="option in feedbackStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('admin.adminNote')">
              <el-input
                v-model="feedbackForm.adminNote"
                type="textarea"
                maxlength="1000"
                show-word-limit
                :rows="4"
                :placeholder="t('admin.adminNotePlaceholder')"
              />
            </el-form-item>
          </el-form>
        </div>

        <template #footer>
          <el-button @click="feedbackDialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" :loading="feedbackSaving" @click="handleUpdateFeedbackStatus">{{ t('admin.saveStatus') }}</el-button>
        </template>
      </el-dialog>
    </template>

    <section v-else class="panel" v-loading="loading">
      <div class="panel-body admin-denied">
        <h3>{{ t('admin.checkingTitle') }}</h3>
        <p class="muted">{{ t('admin.checkingDesc') }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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
const { t } = useI18n()
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

const feedbackTypeOptions = computed<Array<{ label: string; value: FeedbackType }>>(() => [
  { label: t('feedback.types.BUG'), value: 'BUG' },
  { label: t('feedback.types.UX'), value: 'UX' },
  { label: t('feedback.types.AUDIT_INACCURATE'), value: 'AUDIT_INACCURATE' },
  { label: t('feedback.types.QA_INACCURATE'), value: 'QA_INACCURATE' },
  { label: t('feedback.types.INTERVIEW_QUESTION'), value: 'INTERVIEW_QUESTION' },
  { label: t('feedback.types.UPLOAD'), value: 'UPLOAD' },
  { label: t('feedback.types.OTHER'), value: 'OTHER' }
])

const feedbackStatusOptions = computed<Array<{ label: string; value: FeedbackStatus }>>(() => [
  { label: t('admin.feedbackStatuses.PENDING'), value: 'PENDING' },
  { label: t('admin.feedbackStatuses.PROCESSING'), value: 'PROCESSING' },
  { label: t('admin.feedbackStatuses.RESOLVED'), value: 'RESOLVED' },
  { label: t('admin.feedbackStatuses.WONTFIX'), value: 'WONTFIX' }
])

const metrics = computed(() => [
  { label: t('admin.metrics.users'), value: stats.value?.userCount ?? '-' },
  { label: t('admin.metrics.projects'), value: stats.value?.projectCount ?? '-' },
  { label: t('admin.metrics.reports'), value: stats.value?.reportCount ?? '-' },
  { label: t('admin.metrics.qa'), value: stats.value?.qaCount ?? '-' },
  { label: t('admin.metrics.shares'), value: stats.value?.shareCount ?? '-' },
  { label: t('admin.metrics.todayUsers'), value: stats.value?.todayUserCount ?? '-' },
  { label: t('admin.metrics.todayProjects'), value: stats.value?.todayProjectCount ?? '-' },
  { label: t('admin.metrics.todayReports'), value: stats.value?.todayReportCount ?? '-' },
  { label: t('admin.metrics.todayQa'), value: stats.value?.todayQaCount ?? '-' }
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
  return feedbackTypeOptions.value.find((option) => option.value === type)?.label || type || '-'
}

function feedbackStatusLabel(status?: string) {
  return feedbackStatusOptions.value.find((option) => option.value === status)?.label || status || '-'
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
    ElMessage.success(t('admin.feedbackUpdated'))
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
    ElMessage.warning(t('admin.amountRequired'))
    return
  }

  const reason = creditForm.value.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning(t('admin.reasonRequired'))
    return
  }

  grantLoading.value = true
  try {
    const result = await grantAdminCredit({
      userId: selectedCreditUser.value.userId,
      amount: creditForm.value.amount,
      reason
    })
    ElMessage.success(t('admin.creditGranted'))
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
  padding: 26px;
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

.page-title {
  padding: 4px 0 2px;
}

.page-title h2 {
  color: var(--pm-ink);
}

.admin-credit-panel,
.admin-feedback-panel {
  border-radius: 8px;
  background:
    linear-gradient(145deg, rgba(248, 251, 255, 0.92), rgba(255, 255, 255, 0.78)),
    rgba(255, 255, 255, 0.86);
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
  padding: 12px;
  border: 1px solid rgba(223, 230, 240, 0.82);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
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
  padding: 12px;
  border: 1px solid rgba(223, 230, 240, 0.82);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
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
  background: linear-gradient(135deg, rgba(238, 246, 255, 0.86), rgba(240, 251, 249, 0.68));
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
