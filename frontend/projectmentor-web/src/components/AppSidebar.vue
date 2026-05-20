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
        <span>工作台</span>
      </el-menu-item>
      <el-menu-item index="/projects">
        <el-icon><FolderOpened /></el-icon>
        <span>项目管理</span>
      </el-menu-item>
      <el-menu-item index="/hallucination">
        <el-icon><Warning /></el-icon>
        <span>幻觉检测</span>
      </el-menu-item>
      <el-menu-item index="/interview">
        <el-icon><ChatDotRound /></el-icon>
        <span>面试深挖</span>
      </el-menu-item>
      <el-menu-item index="/credits">
        <el-icon><Coin /></el-icon>
        <span>额度中心</span>
      </el-menu-item>
    </el-menu>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound, Coin, DataBoard, FolderOpened, Warning } from '@element-plus/icons-vue'

const route = useRoute()

const activePath = computed(() => {
  if (route.path.startsWith('/projects')) {
    return '/projects'
  }

  return route.path
})
</script>
