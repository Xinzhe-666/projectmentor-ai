<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('reports.title') }}</h2>
          <p class="muted">{{ t('reports.desc') }}</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadReports">{{ t('common.refresh') }}</el-button>
      </div>
      <div class="panel-body page-stack">
        <div class="list-filters">
          <el-select
            v-model="filters.projectId"
            clearable
            filterable
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
            :placeholder="t('reports.keywordPlaceholder')"
            @keyup.enter="handleSearch"
          />
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleSearch">{{ t('common.search') }}</el-button>
            <el-button @click="handleReset">{{ t('common.reset') }}</el-button>
          </div>
        </div>

        <el-table v-if="records.length || loading" v-loading="loading" :data="records" stripe>
          <el-table-column prop="projectName" :label="t('common.projectName')" min-width="170" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.projectName || `${t('common.projectId')} #${row.projectId}` }}
            </template>
          </el-table-column>
          <el-table-column :label="t('reports.scores')" min-width="180">
            <template #default="{ row }">
              <div class="score-pair">
                <el-tag type="success" effect="light">{{ t('reports.healthScore', { score: formatScore(row.healthScore) }) }}</el-tag>
                <el-tag effect="light">{{ t('reports.authenticityScore', { score: formatScore(row.authenticityScore) }) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('common.status')" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'FINISHED' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="summary" :label="t('common.summary')" min-width="260" show-overflow-tooltip />
          <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
          <el-table-column :label="t('common.share')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.shared ? 'success' : 'info'" effect="light">
                {{ row.shared ? t('reports.shared') : t('reports.notShared') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.operation')" width="210" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" :icon="DocumentChecked" @click="router.push(`/reports/${row.reportId}`)">
                {{ t('reports.viewReport') }}
              </el-button>
              <el-button text type="primary" :icon="Link" :loading="sharingId === row.reportId" @click="handleShare(row)">
                {{ row.shared ? t('reports.copyShare') : t('reports.shareReport') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState v-else :title="t('reports.emptyTitle')" :description="t('reports.emptyDesc')" />

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
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { DocumentChecked, Link, Refresh, Search } from '@element-plus/icons-vue'

import { listMyReports, type ReportListParams } from '@/api/analysis'
import { listProjects } from '@/api/project'
import { createReportShare } from '@/api/share'
import EmptyState from '@/components/EmptyState.vue'
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

const filters = reactive<{
  projectId?: number
  keyword: string
}>({
  projectId: undefined,
  keyword: ''
})

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
  return Number.isFinite(score) ? Math.round(Number(score)) : '-'
}

function statusTagType(status?: string) {
  const statusMap: Record<string, 'info' | 'primary' | 'success' | 'danger' | 'warning'> = {
    PENDING: 'info',
    RUNNING: 'primary',
    FINISHED: 'success',
    FAILED: 'danger'
  }

  return statusMap[status || 'FINISHED'] || 'info'
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

  if (Number.isFinite(projectId) && projectId > 0) {
    filters.projectId = projectId
  }
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
    // fall through
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '-9999px'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()

  let copied = false
  try {
    copied = document.execCommand('copy')
  } catch {
    copied = false
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
.list-filters {
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(220px, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.filter-actions,
.score-pair,
.pagination-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-row {
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .list-filters {
    grid-template-columns: 1fr;
  }
}
</style>
