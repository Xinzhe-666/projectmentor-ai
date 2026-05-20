<template>
  <section class="panel form-page">
    <div class="panel-title">
      <div>
        <h2>新建项目</h2>
        <p class="muted">先记录项目基础信息，后续再上传 README 或 ZIP。</p>
      </div>
    </div>
    <div class="panel-body">
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="例如：AI 简历助手" />
        </el-form-item>
        <el-form-item label="GitHub">
          <el-input v-model="form.githubUrl" placeholder="https://github.com/..." />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="简要说明项目目标和核心功能" />
        </el-form-item>
        <el-form-item label="项目类型">
          <el-select v-model="form.projectType" placeholder="请选择" clearable>
            <el-option label="后端" value="BACKEND" />
            <el-option label="前端" value="FRONTEND" />
            <el-option label="全栈" value="FULLSTACK" />
            <el-option label="AI 项目" value="AI" />
          </el-select>
        </el-form-item>
        <el-form-item label="技术栈">
          <el-input v-model="form.techStack" placeholder="Java, Spring Boot, Vue, MySQL" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">创建项目</el-button>
          <el-button @click="router.push('/projects')">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { createProject } from '@/api/project'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  name: '',
  githubUrl: '',
  description: '',
  projectType: '',
  techStack: ''
})

async function handleSubmit() {
  if (!form.name) {
    ElMessage.warning('请输入项目名称')
    return
  }

  loading.value = true
  try {
    const project = await createProject(form)
    ElMessage.success('项目创建成功')
    router.push(`/projects/${project.id}`)
  } finally {
    loading.value = false
  }
}
</script>
