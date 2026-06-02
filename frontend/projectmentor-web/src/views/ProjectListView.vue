<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>{{ t('projects.listTitle') }}</h2>
          <p class="muted">{{ t('projects.listDesc') }}</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">{{ t('common.createProject') }}</el-button>
      </div>
      <div class="panel-body">
        <el-table v-if="projects.length || loading" v-loading="loading" :data="projects" stripe>
          <el-table-column prop="name" :label="t('common.projectName')" min-width="160" />
          <el-table-column prop="githubUrl" :label="t('common.github')" min-width="240" show-overflow-tooltip />
          <el-table-column prop="techStack" :label="t('common.techStack')" min-width="180" show-overflow-tooltip />
          <el-table-column prop="status" :label="t('common.status')" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" effect="light">{{ row.status || 'PENDING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" :label="t('common.createTime')" min-width="180" />
          <el-table-column :label="t('common.operation')" width="170" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="router.push(`/projects/${row.id}`)">{{ t('common.viewDetails') }}</el-button>
              <el-button text type="danger" @click="handleDelete(row.id)">{{ t('common.delete') }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState
          v-else
          :title="t('projects.emptyTitle')"
          :description="t('projects.emptyDesc')"
        >
          <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">{{ t('common.createProject') }}</el-button>
        </EmptyState>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { deleteProject, listProjects } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import type { Project } from '@/types/api'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const projects = ref<Project[]>([])

function statusTagType(status?: string) {
  const statusMap: Record<string, 'info' | 'primary' | 'success' | 'danger' | 'warning'> = {
    PENDING: 'info',
    ANALYZING: 'primary',
    FINISHED: 'success',
    FAILED: 'danger'
  }

  return statusMap[status || 'PENDING'] || 'info'
}

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm(t('projects.deleteConfirm'), t('projects.deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('common.delete'),
      cancelButtonText: t('common.cancel')
    })
  } catch {
    return
  }

  await deleteProject(id)
  ElMessage.success(t('projects.deleted'))
  loadProjects()
}

onMounted(loadProjects)
</script>
