<template>
  <header class="app-header">
    <div>
      <p class="eyebrow">ProjectMentor AI</p>
      <h1>{{ title }}</h1>
    </div>

    <div class="header-actions">
      <el-tag effect="light" type="success">
        剩余额度 {{ userStore.remainingCredits }}
      </el-tag>
      <div class="user-pill">
        <el-avatar :size="32">{{ userInitial }}</el-avatar>
        <span>{{ userStore.userInfo?.username || '用户' }}</span>
      </div>
      <el-button :icon="SwitchButton" @click="handleLogout">退出</el-button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { logout as logoutApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const title = computed(() => {
  const titleMap: Record<string, string> = {
    dashboard: '工作台',
    projects: '项目库',
    'project-create': '新建项目',
    'project-detail': '项目详情',
    'report-detail': '审计报告',
    hallucination: 'AI 幻觉检测',
    interview: '面试深挖',
    credits: '额度中心'
  }

  return titleMap[String(route.name)] || 'ProjectMentor AI'
})

const userInitial = computed(() => userStore.userInfo?.username?.slice(0, 1).toUpperCase() || 'U')

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
</script>
