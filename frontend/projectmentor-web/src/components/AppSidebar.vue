<template>
  <aside class="app-sidebar">
    <RouterLink class="brand" to="/dashboard">
      <span class="brand-mark">PM</span>
      <span>
        <strong>{{ t('common.brandShort') }}</strong>
        <small>{{ t('common.productTagline') }}</small>
      </span>
    </RouterLink>

    <el-menu :default-active="activePath" router class="side-menu">
      <el-menu-item index="/dashboard">
        <el-icon><DataBoard /></el-icon>
        <span>{{ t('route.dashboard') }}</span>
      </el-menu-item>
      <el-menu-item index="/projects">
        <el-icon><FolderOpened /></el-icon>
        <span>{{ t('route.projects') }}</span>
      </el-menu-item>
      <el-menu-item index="/projects/create">
        <el-icon><CirclePlus /></el-icon>
        <span>{{ t('route.projectCreate') }}</span>
      </el-menu-item>
      <el-menu-item index="/hallucination">
        <el-icon><Warning /></el-icon>
        <span>{{ t('route.hallucination') }}</span>
      </el-menu-item>
      <el-menu-item index="/interview">
        <el-icon><ChatDotRound /></el-icon>
        <span>{{ t('route.interview') }}</span>
      </el-menu-item>
      <el-menu-item index="/credits">
        <el-icon><Coin /></el-icon>
        <span>{{ t('route.credits') }}</span>
      </el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin">
        <el-icon><Monitor /></el-icon>
        <span>{{ t('route.admin') }}</span>
      </el-menu-item>
    </el-menu>

    <div class="sidebar-footnote">
      {{ t('sidebar.footnote') }}
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChatDotRound, CirclePlus, Coin, DataBoard, FolderOpened, Monitor, Warning } from '@element-plus/icons-vue'
import { getAdminMe } from '@/api/admin'

const route = useRoute()
const { t } = useI18n()
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
