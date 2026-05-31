<template>
  <aside class="app-sidebar">
    <RouterLink class="brand" to="/dashboard">
      <span class="brand-mark">PM</span>
      <span>
        <strong>ProjectMentor</strong>
        <small>AI Career Audit</small>
      </span>
    </RouterLink>

    <el-menu :default-active="activePath" router class="side-menu">
      <el-menu-item index="/dashboard">
        <el-icon><DataBoard /></el-icon>
        <span>Dashboard</span>
      </el-menu-item>
      <el-menu-item index="/projects">
        <el-icon><FolderOpened /></el-icon>
        <span>我的项目</span>
      </el-menu-item>
      <el-menu-item index="/projects/create">
        <el-icon><CirclePlus /></el-icon>
        <span>创建项目</span>
      </el-menu-item>
      <el-menu-item index="/hallucination">
        <el-icon><Warning /></el-icon>
        <span>AI 幻觉检测</span>
      </el-menu-item>
      <el-menu-item index="/interview">
        <el-icon><ChatDotRound /></el-icon>
        <span>模拟面试</span>
      </el-menu-item>
      <el-menu-item index="/credits">
        <el-icon><Coin /></el-icon>
        <span>额度中心</span>
      </el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin">
        <el-icon><Monitor /></el-icon>
        <span>管理员后台</span>
      </el-menu-item>
    </el-menu>

    <div class="sidebar-footnote">
      上传 README 与代码证据后，再生成报告，会得到更可信的面试风险判断。
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, CirclePlus, Coin, DataBoard, FolderOpened, Monitor, Warning } from '@element-plus/icons-vue'
import { getAdminMe } from '@/api/admin'

const route = useRoute()
const isAdmin = ref(false)

const activePath = computed(() => {
  if (route.path === '/projects/create') {
    return '/projects/create'
  }

  if (route.path.startsWith('/projects/')) {
    return '/projects'
  }

  return route.path
})

async function checkAdminEntry() {
  try {
    const me = await getAdminMe()
    isAdmin.value = Boolean(me.admin)
  } catch {
    isAdmin.value = false
  }
}

onMounted(checkAdminEntry)
</script>
