<template>
  <div class="page-stack create-page">
    <section class="panel create-guide">
      <div class="panel-title">
        <div>
          <p class="eyebrow">{{ t('projects.createGuide.eyebrow') }}</p>
          <h2>{{ t('projects.createGuide.title') }}</h2>
          <p class="muted">{{ t('projects.createGuide.description') }}</p>
        </div>
        <el-tag type="success" effect="light">{{ t('projects.createGuide.ruleFree') }}</el-tag>
      </div>
      <div class="panel-body guide-grid">
        <article v-for="(tip, index) in guideTips" :key="tip">
          <span>{{ index + 1 }}</span>
          <p>{{ tip }}</p>
        </article>
      </div>
    </section>

    <div class="create-layout">
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

      <aside class="panel example-panel">
        <div class="panel-title">
          <div>
            <h3>{{ t('projects.example.title') }}</h3>
            <p class="muted">{{ t('projects.example.description') }}</p>
          </div>
        </div>
        <div class="panel-body example-stack">
          <dl>
            <div>
              <dt>{{ t('common.projectName') }}</dt>
              <dd>{{ t('projects.example.name') }}</dd>
            </div>
            <div>
              <dt>{{ t('common.techStack') }}</dt>
              <dd>{{ t('projects.example.techStack') }}</dd>
            </div>
            <div>
              <dt>{{ t('common.description') }}</dt>
              <dd>{{ t('projects.example.projectDescription') }}</dd>
            </div>
          </dl>
          <el-alert
            :title="t('projects.example.notice')"
            type="info"
            show-icon
            :closable="false"
          />
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
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

const guideTips = computed(() => [
  t('projects.createGuide.tips.name'),
  t('projects.createGuide.tips.techStack'),
  t('projects.createGuide.tips.description'),
  t('projects.createGuide.tips.upload'),
  t('projects.createGuide.tips.credits')
])

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
.create-page {
  max-width: 1080px;
}

.create-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 360px);
  align-items: start;
  gap: 18px;
}

.form-page {
  max-width: none;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.guide-grid article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.guide-grid span {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 8px;
  background: #111827;
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
}

.guide-grid p {
  margin: 0;
  color: #475467;
  font-size: 13px;
  line-height: 1.6;
}

.example-stack,
.example-stack dl {
  display: grid;
  gap: 14px;
}

.example-stack dl {
  margin: 0;
}

.example-stack dl > div {
  padding: 12px;
  border: 1px solid var(--pm-border);
  border-radius: 8px;
  background: #fbfdff;
}

.example-stack dt {
  color: var(--pm-muted);
  font-size: 12px;
  font-weight: 800;
}

.example-stack dd {
  margin: 6px 0 0;
  color: #344054;
  line-height: 1.7;
}

.wide-control {
  width: min(420px, 100%);
}

@media (max-width: 920px) {
  .create-layout,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>
