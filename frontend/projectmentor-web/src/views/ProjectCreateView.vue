<template>
  <section class="panel form-page">
    <div class="panel-title">
      <div>
        <h2>{{ t('projects.createTitle') }}</h2>
        <p class="muted">{{ t('projects.createDesc') }}</p>
      </div>
    </div>
    <div class="panel-body">
      <el-form :model="form" label-width="110px">
        <el-form-item :label="t('common.projectName')" required>
          <el-input v-model="form.name" :placeholder="t('projects.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.github')">
          <el-input v-model="form.githubUrl" :placeholder="t('projects.githubPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('common.description')">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="10000"
            show-word-limit
            :placeholder="t('projects.descPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('common.projectType')">
          <el-select v-model="form.projectType" :placeholder="t('projects.typePlaceholder')" clearable class="wide-control">
            <el-option :label="t('projects.typeBackend')" value="BACKEND" />
            <el-option :label="t('projects.typeFrontend')" value="FRONTEND" />
            <el-option :label="t('projects.typeFullstack')" value="FULLSTACK" />
            <el-option :label="t('projects.typeAi')" value="AI" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.techStack')">
          <el-input
            v-model="form.techStack"
            type="textarea"
            :rows="3"
            maxlength="5000"
            show-word-limit
            :placeholder="t('projects.techPlaceholder')"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">{{ t('common.createProject') }}</el-button>
          <el-button @click="router.push('/projects')">{{ t('common.cancel') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'

import { createProject } from '@/api/project'

const router = useRouter()
const { t } = useI18n()
const loading = ref(false)
const form = reactive({
  name: '',
  githubUrl: '',
  description: '',
  projectType: '',
  techStack: ''
})

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning(t('projects.nameRequired'))
    return
  }

  loading.value = true
  try {
    const project = await createProject(form)
    ElMessage.success(t('projects.createSuccess'))
    router.push(`/projects/${project.id}`)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wide-control {
  width: min(420px, 100%);
}
</style>
