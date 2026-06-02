<template>
  <header class="app-header">
    <div>
      <p class="eyebrow">ProjectMentor AI</p>
      <h1>{{ title }}</h1>
    </div>

    <div class="header-actions">
      <LanguageSwitch />
      <el-button :icon="Message" @click="feedbackVisible = true">{{ t('common.feedback') }}</el-button>
      <el-button :icon="Coffee" @click="donateVisible = true">{{ t('common.coffeeShort') }}</el-button>
      <el-tag effect="light" type="success">{{ t('common.creditsRemaining') }} {{ userStore.remainingCredits }}</el-tag>
      <div class="user-pill">
        <el-avatar :size="32">{{ userInitial }}</el-avatar>
        <span>{{ userStore.userInfo?.username || t('common.user') }}</span>
      </div>
      <el-button :icon="SwitchButton" @click="handleLogout">{{ t('common.logout') }}</el-button>
    </div>
  </header>

  <DonateDialog v-model="donateVisible" />
  <FeedbackDialog v-model="feedbackVisible" />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Coffee, Message, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { logout as logoutApi } from '@/api/auth'
import DonateDialog from '@/components/DonateDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { getMyCredits } from '@/api/credit'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const donateVisible = ref(false)
const feedbackVisible = ref(false)

const title = computed(() => {
  const titleMap: Record<string, string> = {
    dashboard: t('route.dashboard'),
    projects: t('route.projects'),
    'project-create': t('route.projectCreate'),
    'project-detail': t('route.projectDetail'),
    'report-detail': t('route.reportDetail'),
    hallucination: t('route.hallucination'),
    interview: t('route.interview'),
    credits: t('route.credits'),
    admin: t('route.admin')
  }

  return titleMap[String(route.name)] || t('common.appName')
})

const userInitial = computed(() => userStore.userInfo?.username?.slice(0, 1).toUpperCase() || 'U')

async function refreshCredits() {
  try {
    const info = await getMyCredits()
    userStore.updateCredits(info.remainingCredits)
  } catch {
    // Header 中的额度刷新失败不阻塞页面本身。
  }
}

async function handleLogout() {
  try {
    await logoutApi()
  } catch {
    // Token 失效时也允许本地退出。
  } finally {
    userStore.logout()
    ElMessage.success(t('header.logoutSuccess'))
    router.push('/login')
  }
}

onMounted(refreshCredits)
</script>
