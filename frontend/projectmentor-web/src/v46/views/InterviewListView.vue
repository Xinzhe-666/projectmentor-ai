<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('interviews.title') }}</h2>
          <p class="muted">{{ t('interviews.desc') }}</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadSessions">{{ t('common.refresh') }}</el-button>
      </div>
      <div class="panel-body page-stack">
        <div class="list-filters">
          <el-select
            v-model="filters.projectId"
            clearable
            filterable
            :placeholder="t('interviews.projectFilter')"
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
            :placeholder="t('interviews.keywordPlaceholder')"
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
          <el-table-column :label="t('interviews.totalScore')" width="130">
            <template #default="{ row }">
              <strong>{{ formatScore(row.totalScore) }}</strong>
            </template>
          </el-table-column>
          <el-table-column :label="t('interviews.questionStats')" min-width="190">
            <template #default="{ row }">
              <div class="stat-tags">
                <el-tag effect="light">{{ t('interviews.questionCount', { count: row.questionCount }) }}</el-tag>
                <el-tag type="success" effect="light">{{ t('interviews.answeredCount', { count: row.answeredCount }) }}</el-tag>
                <el-tag type="warning" effect="light">{{ t('interviews.skippedCount', { count: row.skippedCount }) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('common.status')" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'RUNNING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
          <el-table-column prop="updateTime" :label="t('common.updateTime')" min-width="180" />
          <el-table-column :label="t('common.operation')" width="150" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" :icon="Tickets" @click="router.push(`/interview?sessionId=${row.sessionId}`)">
                {{ t('interviews.viewSession') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState v-else :title="t('interviews.emptyTitle')" :description="t('interviews.emptyDesc')">
          <el-button type="primary" @click="router.push('/interview')">{{ t('interviews.startInterview') }}</el-button>
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
            @current-change="loadSessions"
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
import { Refresh, Search, Tickets } from '@element-plus/icons-vue'

import { listInterviewSessions, type InterviewListParams } from '@/v46/api/interview'
import { listProjects } from '@/v46/api/project'
import EmptyState from '@/v46/components/EmptyState.vue'
import type { InterviewSessionListItem, PageResult, Project } from '@/v46/types/api'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()

const loading = ref(false)
const projectLoading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const pageData = ref<PageResult<InterviewSessionListItem>>()
const projects = ref<Project[]>([])

const filters = reactive<{
  projectId?: number
  keyword: string
}>({
  projectId: undefined,
  keyword: ''
})

const records = computed(() => pageData.value?.records || [])

function buildParams(): InterviewListParams {
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
    RUNNING: 'primary',
    FINISHED: 'success',
    FAILED: 'danger'
  }

  return statusMap[status || 'RUNNING'] || 'info'
}

async function loadProjects() {
  projectLoading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    projectLoading.value = false
  }
}

async function loadSessions() {
  loading.value = true
  try {
    pageData.value = await listInterviewSessions(buildParams())
    total.value = pageData.value.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadSessions()
}

function handleReset() {
  filters.projectId = undefined
  filters.keyword = ''
  page.value = 1
  loadSessions()
}

function handleSizeChange() {
  page.value = 1
  loadSessions()
}

function applyQueryFilters() {
  const rawProjectId = Array.isArray(route.query.projectId) ? route.query.projectId[0] : route.query.projectId
  const projectId = Number(rawProjectId)

  if (Number.isFinite(projectId) && projectId > 0) {
    filters.projectId = projectId
  }
}

onMounted(() => {
  applyQueryFilters()
  loadProjects()
  loadSessions()
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
.stat-tags,
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
