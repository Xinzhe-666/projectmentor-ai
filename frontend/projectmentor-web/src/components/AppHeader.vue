<template>
  <header class="app-header">
    <div class="workspace-header-main">
      <button
        class="workspace-nav-toggle"
        type="button"
        :aria-label="t('shellV5.openNavigation')"
        @click="emit('toggle-navigation')"
      >
        <el-icon><Expand /></el-icon>
      </button>
      <div class="workspace-header-copy">
        <nav class="workspace-breadcrumb" :aria-label="t('shellV5.breadcrumbLabel')">
          <span>{{ sectionTitle }}</span>
          <span aria-hidden="true">/</span>
          <span aria-current="page">{{ title }}</span>
        </nav>
        <h1>{{ title }}</h1>
      </div>
    </div>

    <div class="header-actions">
      <LanguageSwitch />
      <el-button
        v-if="showCreateAction"
        class="header-primary-action"
        type="primary"
        :icon="Plus"
        :aria-label="t('shellV5.createProject')"
        @click="router.push('/projects/create')"
      >
        <span class="header-primary-label">{{ t('shellV5.createProject') }}</span>
      </el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Expand, Plus } from '@element-plus/icons-vue'

import LanguageSwitch from '@/components/LanguageSwitch.vue'

const emit = defineEmits<{
  (event: 'toggle-navigation'): void
}>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const title = computed(() => {
  const titleMap: Record<string, string> = {
    dashboard: t('dashboard.v5.title'),
    projects: t('route.projects'),
    'project-create': t('route.projectCreate'),
    'project-detail': t('route.projectDetail'),
    'project-defense': t('defense.navigation'),
    reports: t('route.reports'),
    'report-detail': t('route.reportDetail'),
    hallucination: t('route.hallucination'),
    interview: t('route.interview'),
    interviews: t('route.interviews'),
    credits: t('route.credits'),
    settings: t('settings.title'),
    admin: t('route.admin')
  }

  return titleMap[String(route.name)] || t('common.appName')
})

const sectionTitle = computed(() => {
  const workspaceRoutes = ['dashboard', 'projects', 'project-create', 'project-detail', 'project-defense', 'reports', 'report-detail', 'interviews']
  const toolRoutes = ['hallucination', 'interview']
  const routeName = String(route.name)

  if (toolRoutes.includes(routeName)) {
    return t('shellV5.groups.tools')
  }

  if (!workspaceRoutes.includes(routeName)) {
    return t('shellV5.groups.system')
  }

  return t('shellV5.groups.workspace')
})

const showCreateAction = computed(() => route.name === 'dashboard')
</script>
