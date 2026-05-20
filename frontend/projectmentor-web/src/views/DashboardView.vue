<template>
  <div class="page-stack">
    <section class="panel">
      <div class="panel-body dashboard-hero">
        <div>
          <p class="eyebrow">Welcome back</p>
          <h2>你好，{{ userStore.userInfo?.username || '同学' }}</h2>
          <p class="muted">把项目、README 和代码证据放进来，让 AI 先替面试官问一轮狠问题。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="router.push('/projects/create')">新建项目</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <div class="metric-card" v-for="metric in metrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const metrics = computed(() => [
  { label: '我的项目', value: '--' },
  { label: '剩余额度', value: userStore.remainingCredits },
  { label: '审计报告', value: '--' },
  { label: '面试会话', value: '--' }
])
</script>

<style scoped>
.dashboard-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.dashboard-hero h2 {
  margin: 8px 0;
  font-size: 30px;
}
</style>
