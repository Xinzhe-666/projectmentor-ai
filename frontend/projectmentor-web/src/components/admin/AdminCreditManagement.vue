<template>
  <section class="panel admin-credit-panel">
    <div class="panel-title">
      <div>
        <h3>{{ t('admin.creditTitle') }}</h3>
        <p class="muted">{{ t('admin.creditDesc') }}</p>
      </div>
    </div>

    <div class="panel-body credit-stack">
      <div class="credit-toolbar">
        <el-input
          v-model="keyword"
          clearable
          :placeholder="t('admin.creditPlaceholder')"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="sort" :placeholder="t('admin.sortBy')" @change="handleSearch">
          <el-option :label="t('admin.sortNewest')" value="createdAtDesc" />
          <el-option :label="t('admin.sortBalanceDesc')" value="balanceDesc" />
          <el-option :label="t('admin.sortBalanceAsc')" value="balanceAsc" />
          <el-option :label="t('admin.sortLastChange')" value="lastCreditChangeAtDesc" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="handleSearch">{{ t('common.search') }}</el-button>
      </div>

      <el-table :data="users" stripe v-loading="loading" :empty-text="t('admin.noUsers')">
        <el-table-column prop="userId" :label="t('common.userId')" width="95" />
        <el-table-column prop="username" :label="t('common.username')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="email" :label="t('common.email')" min-width="190" show-overflow-tooltip />
        <el-table-column prop="creditBalance" :label="t('admin.balance')" width="105">
          <template #default="{ row }"><strong class="balance">{{ row.creditBalance }}</strong></template>
        </el-table-column>
        <el-table-column prop="totalConsumed" :label="t('admin.totalConsumed')" width="120" />
        <el-table-column prop="totalRefunded" :label="t('admin.totalRefunded')" width="120" />
        <el-table-column prop="totalAdminGranted" :label="t('admin.totalAdminGranted')" width="130" />
        <el-table-column prop="lastCreditChangeAt" :label="t('admin.lastCreditChange')" min-width="170" />
        <el-table-column :label="t('common.operation')" width="285" fixed="right">
          <template #default="{ row }">
            <div class="credit-actions">
              <el-button type="success" plain @click="openAdjustment(row, 'grant')">{{ t('admin.grantCredit') }}</el-button>
              <el-button type="danger" plain @click="openAdjustment(row, 'deduct')">{{ t('admin.deductCredit') }}</el-button>
              <el-button @click="openLogs(row)">{{ t('admin.viewLogs') }}</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-if="total > 0"
          background
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :current-page="page"
          :page-size="size"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="adjustmentVisible"
      :title="adjustmentMode === 'grant' ? t('admin.grantCredit') : t('admin.deductCredit')"
      width="520px"
    >
      <div v-if="selectedUser" class="dialog-stack">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('common.username')">{{ selectedUser.username || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('common.email')">{{ selectedUser.email }}</el-descriptions-item>
          <el-descriptions-item :label="t('admin.balance')">{{ selectedUser.creditBalance }}</el-descriptions-item>
        </el-descriptions>

        <el-form label-width="100px" @submit.prevent>
          <el-form-item :label="t('admin.adjustAmount')">
            <el-input-number
              v-model="adjustmentForm.amount"
              :min="1"
              :max="10000"
              :step="1"
              step-strictly
              controls-position="right"
            />
          </el-form-item>
          <el-form-item :label="adjustmentMode === 'grant' ? t('admin.grantReason') : t('admin.deductReason')">
            <el-input
              v-model="adjustmentForm.reason"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              :placeholder="adjustmentMode === 'grant'
                ? t('admin.grantReasonPlaceholder')
                : t('admin.deductReasonPlaceholder')"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="adjustmentVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button
          :type="adjustmentMode === 'grant' ? 'success' : 'danger'"
          :loading="saving"
          @click="submitAdjustment"
        >
          {{ adjustmentMode === 'grant' ? t('admin.confirmGrant') : t('admin.confirmDeduct') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logsVisible" :title="t('admin.creditLogs')" width="900px">
      <div v-if="selectedUser" class="dialog-stack">
        <p class="muted">
          {{ selectedUser.username || selectedUser.email }} · {{ t('admin.balance') }} {{ selectedUser.creditBalance }}
        </p>
        <el-table :data="logs" stripe v-loading="logsLoading" :empty-text="t('admin.noCreditLogs')">
          <el-table-column prop="amount" :label="t('credits.change')" width="95">
            <template #default="{ row }">
              <el-tag :type="row.amount > 0 ? 'success' : 'warning'" effect="light">
                {{ row.amount > 0 ? `+${row.amount}` : row.amount }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="type" :label="t('admin.transactionType')" width="110">
            <template #default="{ row }">{{ creditTypeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="module" :label="t('admin.module')" min-width="200">
            <template #default="{ row }">{{ operationLabel(row.module) }}</template>
          </el-table-column>
          <el-table-column prop="description" :label="t('admin.transactionDescription')" min-width="260" show-overflow-tooltip />
          <el-table-column prop="balanceAfter" :label="t('admin.balanceAfter')" width="115" />
          <el-table-column prop="createdAt" :label="t('credits.time')" min-width="170" />
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-if="logsTotal > 0"
            background
            layout="total, prev, pager, next"
            :current-page="logsPage"
            :page-size="logsSize"
            :total="logsTotal"
            @current-change="handleLogsPageChange"
          />
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  deductUserCredits,
  getAdminCreditLogs,
  getAdminCreditUsers,
  grantUserCredits
} from '@/api/admin'
import type { AdminCreditTransaction, AdminCreditUser } from '@/types/api'

const { t, te } = useI18n()
const loading = ref(false)
const users = ref<AdminCreditUser[]>([])
const keyword = ref('')
const sort = ref<'createdAtDesc' | 'balanceDesc' | 'balanceAsc' | 'lastCreditChangeAtDesc'>('createdAtDesc')
const page = ref(1)
const size = ref(10)
const total = ref(0)

const selectedUser = ref<AdminCreditUser | null>(null)
const adjustmentVisible = ref(false)
const adjustmentMode = ref<'grant' | 'deduct'>('grant')
const adjustmentForm = ref({ amount: 1, reason: '' })
const saving = ref(false)

const logsVisible = ref(false)
const logsLoading = ref(false)
const logs = ref<AdminCreditTransaction[]>([])
const logsPage = ref(1)
const logsSize = ref(10)
const logsTotal = ref(0)

async function loadUsers() {
  loading.value = true
  try {
    const data = await getAdminCreditUsers({
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: size.value,
      sort: sort.value
    })
    users.value = data.records
    total.value = data.total
    page.value = data.page
    size.value = data.size
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadUsers()
}

function handleSizeChange(value: number) {
  size.value = value
  page.value = 1
  loadUsers()
}

function handlePageChange(value: number) {
  page.value = value
  loadUsers()
}

function openAdjustment(user: AdminCreditUser, mode: 'grant' | 'deduct') {
  selectedUser.value = user
  adjustmentMode.value = mode
  adjustmentForm.value = { amount: 1, reason: '' }
  adjustmentVisible.value = true
}

async function submitAdjustment() {
  if (!selectedUser.value) {
    return
  }
  if (!Number.isInteger(adjustmentForm.value.amount) || adjustmentForm.value.amount <= 0) {
    ElMessage.warning(t('admin.amountRequired'))
    return
  }

  const reason = adjustmentForm.value.reason.trim()
  if (reason.length < 2) {
    ElMessage.warning(t('admin.reasonRequired'))
    return
  }

  const isGrant = adjustmentMode.value === 'grant'
  try {
    await ElMessageBox.confirm(
      t(isGrant ? 'admin.confirmGrantMessage' : 'admin.confirmDeductMessage', {
        amount: adjustmentForm.value.amount,
        user: selectedUser.value.username || selectedUser.value.email
      }),
      t('common.confirm'),
      { type: 'warning' }
    )
  } catch {
    return
  }

  saving.value = true
  try {
    const payload = { amount: adjustmentForm.value.amount, reason }
    const result = isGrant
      ? await grantUserCredits(selectedUser.value.userId, payload)
      : await deductUserCredits(selectedUser.value.userId, payload)

    selectedUser.value.creditBalance = result.newBalance
    ElMessage.success(t(isGrant ? 'admin.creditGranted' : 'admin.creditDeducted'))
    adjustmentVisible.value = false
    await loadUsers()
    if (logsVisible.value) {
      await loadLogs()
    }
  } finally {
    saving.value = false
  }
}

async function openLogs(user: AdminCreditUser) {
  selectedUser.value = user
  logsPage.value = 1
  logsVisible.value = true
  await loadLogs()
}

async function loadLogs() {
  if (!selectedUser.value) {
    return
  }

  logsLoading.value = true
  try {
    const data = await getAdminCreditLogs(selectedUser.value.userId, {
      page: logsPage.value,
      size: logsSize.value
    })
    logs.value = data.records
    logsTotal.value = data.total
    logsPage.value = data.page
    logsSize.value = data.size
  } finally {
    logsLoading.value = false
  }
}

function handleLogsPageChange(value: number) {
  logsPage.value = value
  loadLogs()
}

function operationLabel(operation: string) {
  const key = `credits.operations.${operation}`
  return te(key) ? t(key) : operation
}

function creditTypeLabel(type?: string) {
  if (!type) {
    return '-'
  }
  const key = `admin.creditTypes.${type}`
  return te(key) ? t(key) : type
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-credit-panel {
  overflow: hidden;
}

.credit-stack,
.dialog-stack {
  display: grid;
  gap: 16px;
}

.credit-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 420px) 210px auto;
  justify-content: flex-start;
  gap: 10px;
  padding: 14px;
  border: 1px solid rgba(223, 230, 240, 0.82);
  border-radius: 8px;
  background: rgba(248, 251, 255, 0.92);
}

.credit-actions {
  display: flex;
  gap: 6px;
  white-space: nowrap;
}

.credit-actions :deep(.el-button) {
  margin-left: 0;
}

.balance {
  color: #0f766e;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .credit-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
