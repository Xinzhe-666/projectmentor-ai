<template>
  <aside
    ref="sidebarRef"
    class="app-sidebar"
    :class="{ 'is-mobile-open': mobileOpen }"
    :aria-hidden="isMobileViewport && !mobileOpen || undefined"
    :inert="isMobileViewport && !mobileOpen"
    @keydown.esc.stop="emit('close')"
    @keydown.tab="handleSidebarTab"
  >
    <div class="classic-sidebar-brand-row">
    <RouterLink class="brand" to="/dashboard" @click="emit('close')">
      <span class="brand-mark">PM</span>
      <span>
        <strong>{{ t('common.brandShort') }}</strong>
        <small>{{ t('common.productTagline') }}</small>
      </span>
    </RouterLink>
      <button class="sidebar-close" type="button" :aria-label="t('shellV5.closeNavigation')" @click="emit('close')">
        <el-icon><Close /></el-icon>
      </button>
    </div>

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
      <el-menu-item index="/reports">
        <el-icon><DocumentChecked /></el-icon>
        <span>{{ t('route.reports') }}</span>
      </el-menu-item>
      <el-menu-item index="/interview">
        <el-icon><ChatDotRound /></el-icon>
        <span>{{ t('route.interview') }}</span>
      </el-menu-item>
      <el-menu-item index="/interviews">
        <el-icon><Tickets /></el-icon>
        <span>{{ t('route.interviews') }}</span>
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ChatDotRound, CirclePlus, Close, Coin, DataBoard, DocumentChecked, FolderOpened, Monitor, Tickets, Warning } from '@element-plus/icons-vue'
import { getAdminMe } from '@/v46/api/admin'

const route = useRoute()
const { t } = useI18n()
const isAdmin = ref(false)
const props = withDefaults(defineProps<{ mobileOpen?: boolean }>(), { mobileOpen: false })
const emit = defineEmits<{ (event: 'close'): void }>()
const isMobileViewport = ref(false)
const sidebarRef = ref<HTMLElement>()
let mobileMediaQuery: MediaQueryList | undefined

const activePath = computed(() => {
  if (route.path === '/projects/create') {
    return '/projects/create'
  }

  if (route.path.startsWith('/projects/')) {
    return '/projects'
  }

  if (route.path.startsWith('/reports/')) {
    return '/reports'
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

function syncMobileViewport(event?: MediaQueryListEvent) {
  const matches = event?.matches ?? mobileMediaQuery?.matches ?? false
  isMobileViewport.value = matches
  if (!matches && props.mobileOpen) emit('close')
}

function handleSidebarTab(event: KeyboardEvent) {
  if (!isMobileViewport.value || !props.mobileOpen || !sidebarRef.value) return
  const focusable = Array.from(sidebarRef.value.querySelectorAll<HTMLElement>(
    'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'
  )).filter((element) => element.offsetParent !== null)
  if (!focusable.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
}

onMounted(() => {
  mobileMediaQuery = window.matchMedia('(max-width: 920px)')
  syncMobileViewport()
  mobileMediaQuery.addEventListener('change', syncMobileViewport)
  checkAdminEntry()
})
onBeforeUnmount(() => mobileMediaQuery?.removeEventListener('change', syncMobileViewport))
</script>
