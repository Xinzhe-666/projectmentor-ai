<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-title">
        <div>
          <h2>我的项目</h2>
          <p class="muted">管理 GitHub 项目、README 和审计报告。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">新建项目</el-button>
      </div>
      <div class="panel-body">
        <el-table v-loading="loading" :data="projects" stripe>
          <el-table-column prop="name" label="项目名" min-width="160" />
          <el-table-column prop="githubUrl" label="GitHub" min-width="220" show-overflow-tooltip />
          <el-table-column prop="techStack" label="技术栈" min-width="180" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag>{{ row.status || 'PENDING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="180" />
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="router.push(`/projects/${row.id}`)">查看详情</el-button>
              <el-button text type="danger" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <EmptyState
          v-if="!loading && projects.length === 0"
          title="还没有项目"
          description="创建一个项目后，就可以保存 README、上传 ZIP 并启动审计。"
        >
          <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">新建项目</el-button>
        </EmptyState>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import { deleteProject, listProjects } from '@/api/project'
import EmptyState from '@/components/EmptyState.vue'
import type { Project } from '@/types/api'

const router = useRouter()
const loading = ref(false)
const projects = ref<Project[]>([])

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listProjects()
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除这个项目吗？相关文件和报告可能无法继续访问。', '删除项目', {
    type: 'warning'
  })
  await deleteProject(id)
  ElMessage.success('已删除项目')
  loadProjects()
}

onMounted(loadProjects)
</script>
