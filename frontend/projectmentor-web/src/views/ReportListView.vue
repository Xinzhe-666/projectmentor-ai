<template>
  <section class="report-index" aria-labelledby="report-index-title">
    <header class="report-index-header">
      <div>
        <h2 id="report-index-title">{{ t('reports.title') }}</h2>
        <p>{{ t('reports.desc') }}</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadReports">{{ t('common.refresh') }}</el-button>
    </header>

    <div class="list-filters">
      <el-select
        v-model="filters.projectId"
        clearable
        filterable
        :aria-label="t('reports.projectFilter')"
        :placeholder="t('reports.projectFilter')"
        :loading="projectLoading"
      >
        <el-option
          v-for="project in projects"
          :key="project.id"
          :label="project.name"
          :value="project.id"
        />
      </el-select>
      <el-input
        v-model="filters.keyword"
        clearable
        :aria-label="t('reports.keywordPlaceholder')"
        :placeholder="t('reports.keywordPlaceholder')"
        @keyup.enter="handleSearch"
      />
      <div class="filter-actions">
        <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
        <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
      </div>
    </div>

    <el-table v-if="records.length || loading" v-loading="loading" class="report-table" :data="records">
      <el-table-column prop="projectName" :label="t('common.projectName')" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <strong class="report-project-name">{{ row.projectName || `${t('common.projectId')} #${row.projectId}` }}</strong>
        </template>
      </el-table-column>
      <el-table-column :label="t('reports.scores')" min-width="180">
        <template #default="{ row }">
          <dl class="score-pair">
            <div><dt>{{ t('reportV5.auditScore') }}</dt><dd>{{ formatScore(row.healthScore) }}</dd></div>
            <div><dt>{{ t('report.scores.authenticity') }}</dt><dd>{{ formatScore(row.authenticityScore) }}</dd></div>
          </dl>
        </template>
      </el-table-column>
      <el-table-column prop="status" :label="t('common.status')" width="150">
        <template #default="{ row }">
          <StatusLabel :status="row.status || 'FINISHED'" :label="reportStatusLabel(row.status)" />
        </template>
      </el-table-column>
      <el-table-column prop="summary" :label="t('common.summary')" min-width="260" show-overflow-tooltip />
      <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
      <el-table-column :label="t('common.share')" width="130">
        <template #default="{ row }">
          <StatusLabel
            :status="row.shared ? 'AVAILABLE' : 'UNAVAILABLE'"
            :label="row.shared ? t('reports.shared') : t('reports.notShared')"
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('common.operation')" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="DocumentChecked" @click="router.push(`/reports/${row.reportId}`)">
            {{ t('reports.viewReport') }}
          </el-button>
          <el-button link type="primary" :icon="Link" :loading="sharingId === row.reportId" @click="handleShare(row)">
            {{ row.shared ? t('reports.copyShare') : t('reports.shareReport') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="records.length" class="mobile-report-list">
      <article v-for="row in records" :key="row.reportId">
        <header>
          <div>
            <span>{{ t('reportV5.reportId') }} #{{ row.reportId }}</span>
            <h3>{{ row.projectName || `${t('common.projectId')} #${row.projectId}` }}</h3>
          </div>
          <StatusLabel :status="row.status || 'FINISHED'" :label="reportStatusLabel(row.status)" />
        </header>
        <p v-if="row.summary">{{ row.summary }}</p>
        <dl>
          <div><dt>{{ t('reportV5.auditScore') }}</dt><dd>{{ formatScore(row.healthScore) }}</dd></div>
          <div><dt>{{ t('report.scores.authenticity') }}</dt><dd>{{ formatScore(row.authenticityScore) }}</dd></div>
          <div><dt>{{ t('common.createTime') }}</dt><dd>{{ row.createTime || '—' }}</dd></div>
          <div>
            <dt>{{ t('common.share') }}</dt>
            <dd>{{ row.shared ? t('reports.shared') : t('reports.notShared') }}</dd>
          </div>
        </dl>
        <footer>
          <el-button type="primary" :icon="DocumentChecked" @click="router.push(`/reports/${row.reportId}`)">
            {{ t('reports.viewReport') }}
          </el-button>
          <el-button :icon="Link" :loading="sharingId === row.reportId" @click="handleShare(row)">
            {{ row.shared ? t('reports.copyShare') : t('reports.shareReport') }}
          </el-button>
        </footer>
      </article>
    </div>

    <EmptyState v-if="!records.length && !loading" :title="t('reports.emptyTitle')" :description="t('reports.emptyDesc')">
      <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">
        {{ t('reports.emptyAction') }}
      </el-button>
    </EmptyState>

    <div class="pagination-row">
      <el-pagination
        v-if="total > 0"
        v-model:current-page="page"
        v-model:page-size="size"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="loadReports"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { DocumentChecked, Link, Plus, Refresh, Search } from '@element-plus/icons-vue'

import { listMyReports, type ReportListParams } from '@/api/analysis'
import { listProjects } from '@/api/project'
import { createReportShare } from '@/api/share'
import EmptyState from '@/components/EmptyState.vue'
import StatusLabel from '@/components/StatusLabel.vue'
import type { PageResult, Project, ReportListItem } from '@/types/api'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const loading = ref(false)
const projectLoading = ref(false)
const sharingId = ref<number>()
const page = ref(1)
const size = ref(10)
const total = ref(0)
const pageData = ref<PageResult<ReportListItem>>()
const projects = ref<Project[]>([])
const filters = reactive<{ projectId?: number; keyword: string }>({ projectId: undefined, keyword: '' })
const records = computed(() => pageData.value?.records || [])

function buildParams(): ReportListParams {
  return {
    page: page.value,
    size: size.value,
    projectId: filters.projectId,
    keyword: filters.keyword.trim() || undefined
  }
}

function formatScore(score?: number) {
  return Number.isFinite(score) ? Math.round(Number(score)) : '—'
}

function reportStatusLabel(status?: string) {
  const normalized = (status || 'FINISHED').toUpperCase()
  const knownStatuses = ['PENDING', 'ANALYZING', 'RUNNING', 'FINISHED', 'SUCCESS', 'FAILED']
  return knownStatuses.includes(normalized)
    ? t(`reportV5.enums.reportStatus.${normalized}`)
    : normalized.replace(/_/g, ' ')
}

async function loadProjects() {
  projectLoading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    projectLoading.value = false
  }
}

async function loadReports() {
  loading.value = true
  try {
    pageData.value = await listMyReports(buildParams())
    total.value = pageData.value.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadReports()
}

function handleReset() {
  filters.projectId = undefined
  filters.keyword = ''
  page.value = 1
  loadReports()
}

function handleSizeChange() {
  page.value = 1
  loadReports()
}

function applyQueryFilters() {
  const rawProjectId = Array.isArray(route.query.projectId) ? route.query.projectId[0] : route.query.projectId
  const projectId = Number(rawProjectId)
  if (Number.isFinite(projectId) && projectId > 0) filters.projectId = projectId
}

async function handleShare(row: ReportListItem) {
  sharingId.value = row.reportId
  try {
    if (!row.shared || !row.shareToken) {
      const shareInfo = await createReportShare(row.reportId)
      row.shared = shareInfo.enabled
      row.shareToken = shareInfo.shareToken
    }

    if (row.shareToken) {
      const copied = await copyText(`${window.location.origin}/share/reports/${row.shareToken}`)
      ElMessage.success(copied ? t('reports.shareCopied') : t('reports.shareReady'))
    }
  } finally {
    sharingId.value = undefined
  }
}

async function copyText(text: string) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // Continue with the textarea fallback on non-secure origins.
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  let copied = false
  try {
    copied = document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
  }
  return copied
}

onMounted(() => {
  applyQueryFilters()
  loadProjects()
  loadReports()
})
</script>

<style scoped>
.report-index {
  background: var(--pm-surface);
  border-top: 3px solid var(--pm-ink);
}

.report-index-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 30px;
  border-bottom: 1px solid var(--pm-stone-strong);
}

.report-index-header h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.025em;
}

.report-index-header p {
  margin: 7px 0 0;
  color: var(--pm-muted);
  line-height: 1.6;
}

.report-index-header :deep(.el-button),
.filter-actions :deep(.el-button),
.mobile-report-list footer :deep(.el-button) {
  min-height: 44px;
}

.list-filters {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(220px, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 18px 30px;
  border-bottom: 1px solid var(--pm-stone-strong);
}

.filter-actions,
.pagination-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.report-table {
  --el-table-border-color: var(--pm-stone);
  --el-table-header-bg-color: var(--pm-surface);
  --el-table-header-text-color: var(--pm-muted);
  --el-table-row-hover-bg-color: var(--pm-surface-hover);
  width: 100%;
  border-radius: 0;
}

.report-table :deep(th.el-table__cell) {
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.report-table :deep(td.el-table__cell) {
  padding-top: 15px;
  padding-bottom: 15px;
}

.report-project-name {
  color: var(--pm-ink);
  font-weight: 600;
}

.score-pair {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
}

.score-pair div {
  min-width: 0;
}

.score-pair dt,
.mobile-report-list dt {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 9px;
  line-height: 1.4;
  text-transform: uppercase;
}

.score-pair dd,
.mobile-report-list dd {
  margin: 4px 0 0;
  color: var(--pm-ink);
  font-family: var(--pm-font-mono);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

.pagination-row {
  justify-content: flex-end;
  padding: 20px 30px 24px;
  border-top: 1px solid var(--pm-stone-strong);
}

.mobile-report-list {
  display: none;
}

@media (max-width: 760px) {
  .report-index-header {
    padding: 24px 20px;
  }

  .list-filters {
    grid-template-columns: 1fr;
    padding: 16px 20px;
  }

  .report-table {
    display: none;
  }

  .mobile-report-list {
    display: grid;
  }

  .mobile-report-list article {
    padding: 24px 20px;
    border-bottom: 1px solid var(--pm-stone-strong);
  }

  .mobile-report-list header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 14px;
  }

  .mobile-report-list header > div > span {
    color: var(--pm-muted);
    font-family: var(--pm-font-mono);
    font-size: 9px;
    text-transform: uppercase;
  }

  .mobile-report-list h3 {
    margin: 6px 0 0;
    color: var(--pm-ink);
    font-size: 18px;
    line-height: 1.4;
  }

  .mobile-report-list > article > p {
    margin: 14px 0 0;
    color: var(--pm-graphite);
    font-size: 14px;
    line-height: 1.65;
  }

  .mobile-report-list dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
    margin: 18px 0 0;
    padding: 14px 0;
    border-top: 1px solid var(--pm-stone);
    border-bottom: 1px solid var(--pm-stone);
  }

  .mobile-report-list footer {
    display: flex;
    gap: 8px;
    margin-top: 16px;
    flex-wrap: wrap;
  }

  .pagination-row {
    justify-content: flex-start;
    padding: 18px 20px;
    overflow-x: auto;
  }
}

@media (max-width: 460px) {
  .report-index-header {
    flex-direction: column;
  }
}
</style>
