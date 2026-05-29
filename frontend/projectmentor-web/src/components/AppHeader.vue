<template>
  <header class="app-header">
    <div>
      <p class="eyebrow">ProjectMentor AI</p>
      <h1>{{ title }}</h1>
    </div>

    <div class="header-actions">
      <el-button :icon="Message" @click="feedbackVisible = true">反馈</el-button>
      <el-button :icon="Coffee" @click="donateVisible = true">喝咖啡</el-button>
      <el-tag effect="light" type="success">剩余额度 {{ userStore.remainingCredits }}</el-tag>
      <div class="user-pill">
        <el-avatar :size="32">{{ userInitial }}</el-avatar>
        <span>{{ userStore.userInfo?.username || '用户' }}</span>
      </div>
      <el-button :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
    </div>
  </header>

  <DonateDialog v-model="donateVisible" />
  <FeedbackDialog v-model="feedbackVisible" />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Coffee, Message, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { logout as logoutApi } from '@/api/auth'
import DonateDialog from '@/components/DonateDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import { getMyCredits } from '@/api/credit'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const donateVisible = ref(false)
const feedbackVisible = ref(false)

const title = computed(() => {
  const titleMap: Record<string, string> = {
    dashboard: 'Dashboard',
    projects: '我的项目',
    'project-create': '创建项目',
    'project-detail': '项目详情',
    'report-detail': '审计报告',
    hallucination: 'AI 幻觉检测',
    interview: '模拟面试',
    credits: '额度中心'
  }

  return titleMap[String(route.name)] || 'ProjectMentor AI'
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
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

onMounted(refreshCredits)
</script>
