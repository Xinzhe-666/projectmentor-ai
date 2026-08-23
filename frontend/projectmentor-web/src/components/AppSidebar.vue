<template>
  <aside
    ref="sidebarRef"
    class="app-sidebar"
    :class="{ 'is-mobile-open': mobileOpen }"
    :aria-label="t('shellV5.navigationLabel')"
    :aria-hidden="isMobileViewport && !mobileOpen || undefined"
    :inert="isMobileViewport && !mobileOpen"
    @keydown.esc.stop="emit('close')"
    @keydown.tab="handleSidebarTab"
  >
    <div class="sidebar-brand-row">
      <button class="sidebar-close" type="button" :aria-label="t('shellV5.closeNavigation')" @click="emit('close')">
        <el-icon><Close /></el-icon>
      </button>
      <RouterLink class="brand" to="/dashboard" @click="emit('close')">
        <BrandLogo variant="compact" />
      </RouterLink>
    </div>

    <nav class="side-menu">
      <section v-for="group in navigationGroups" :key="group.key" class="side-menu-group">
        <h2>{{ group.label }}</h2>
        <RouterLink
          v-for="item in group.items"
          :key="item.path"
          class="side-menu-item"
          :class="{ 'is-active': isPathActive(item.path) }"
          :to="item.path"
          :aria-current="isPathActive(item.path) ? 'page' : undefined"
          @click="emit('close')"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </RouterLink>
      </section>
    </nav>

    <div class="sidebar-account">
      <RouterLink class="sidebar-credit-link" to="/credits" @click="emit('close')">
        <span>{{ t('shellV5.credits') }}</span>
        <strong>{{ userStore.remainingCredits }}</strong>
      </RouterLink>

      <el-dropdown trigger="click" placement="top-start" @command="handleUserCommand">
        <button class="workspace-user-menu" type="button" :aria-label="t('shellV5.userMenu')">
          <el-avatar :size="30">{{ userInitial }}</el-avatar>
          <span>{{ userStore.userInfo?.username || t('common.user') }}</span>
          <el-icon><ArrowUp /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="settings">{{ t('settings.navigation') }}</el-dropdown-item>
            <el-dropdown-item command="credits">{{ t('route.credits') }}</el-dropdown-item>
            <el-dropdown-item command="feedback">{{ t('common.feedback') }}</el-dropdown-item>
            <el-dropdown-item command="donate">{{ t('common.coffeeShort') }}</el-dropdown-item>
            <el-dropdown-item command="logout" divided>{{ t('common.logout') }}</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </aside>

  <DonateDialog v-model="donateVisible" />
  <FeedbackDialog v-model="feedbackVisible" />
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ArrowUp, ChatDotRound, Close, Coin, DataBoard, DocumentChecked, FolderOpened, Monitor, Setting, Tickets, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { getAdminMe } from '@/api/admin'
import { logout as logoutApi } from '@/api/auth'
import { getMyCredits } from '@/api/credit'
import BrandLogo from '@/components/BrandLogo.vue'
import DonateDialog from '@/components/DonateDialog.vue'
import FeedbackDialog from '@/components/FeedbackDialog.vue'
import { useUserStore } from '@/stores/user'

const emit = defineEmits<{
  (event: 'close'): void
}>()

const props = withDefaults(
  defineProps<{
    mobileOpen?: boolean
  }>(),
  {
    mobileOpen: false
  }
)

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const isAdmin = ref(false)
const donateVisible = ref(false)
const feedbackVisible = ref(false)
const isMobileViewport = ref(false)
const sidebarRef = ref<HTMLElement>()
let mobileMediaQuery: MediaQueryList | undefined

const userInitial = computed(() => userStore.userInfo?.username?.slice(0, 1).toUpperCase() || 'U')

const navigationGroups = computed(() => [
  {
    key: 'workspace',
    label: t('shellV5.groups.workspace'),
    items: [
      { path: '/dashboard', label: t('dashboard.v5.title'), icon: DataBoard },
      { path: '/projects', label: t('route.projects'), icon: FolderOpened },
      { path: '/reports', label: t('route.reports'), icon: DocumentChecked },
      { path: '/interviews', label: t('route.interviews'), icon: Tickets }
    ]
  },
  {
    key: 'tools',
    label: t('shellV5.groups.tools'),
    items: [
      { path: '/hallucination', label: t('route.hallucination'), icon: Warning },
      { path: '/interview', label: t('route.interview'), icon: ChatDotRound }
    ]
  },
  {
    key: 'system',
    label: t('shellV5.groups.system'),
    items: [
      { path: '/credits', label: t('route.credits'), icon: Coin },
      { path: '/settings', label: t('settings.navigation'), icon: Setting },
      ...(isAdmin.value ? [{ path: '/admin', label: t('route.admin'), icon: Monitor }] : [])
    ]
  }
])

function isPathActive(path: string) {
  if (path === '/projects') {
    return route.path === '/projects' || route.path.startsWith('/projects/')
  }

  if (path === '/reports') {
    return route.path === '/reports' || route.path.startsWith('/reports/')
  }

  if (path === '/interview') {
    return route.path === '/interview'
  }

  return route.path === path
}

async function checkAdminEntry() {
  try {
    const me = await getAdminMe()
    isAdmin.value = Boolean(me.admin)
  } catch {
    isAdmin.value = false
  }
}

async function refreshCredits() {
  try {
    const info = await getMyCredits()
    userStore.updateCredits(info.remainingCredits)
  } catch {
    // 额度刷新失败不阻塞导航或页面内容。
  }
}

async function handleLogout() {
  try {
    await logoutApi()
  } catch {
    // Token 已失效时仍完成本地退出。
  } finally {
    userStore.logout()
    emit('close')
    ElMessage.success(t('header.logoutSuccess'))
    router.push('/login')
  }
}

function handleUserCommand(command: string) {
  if (command === 'settings') {
    router.push('/settings')
    emit('close')
    return
  }

  if (command === 'credits') {
    router.push('/credits')
    emit('close')
    return
  }

  if (command === 'feedback') {
    emit('close')
    feedbackVisible.value = true
    return
  }

  if (command === 'donate') {
    emit('close')
    donateVisible.value = true
    return
  }

  if (command === 'logout') {
    handleLogout()
  }
}

function syncMobileViewport(event?: MediaQueryListEvent) {
  const matches = event?.matches ?? mobileMediaQuery?.matches ?? false
  isMobileViewport.value = matches

  if (!matches && props.mobileOpen) {
    emit('close')
  }
}

function handleSidebarTab(event: KeyboardEvent) {
  if (!isMobileViewport.value || !props.mobileOpen || !sidebarRef.value) {
    return
  }

  const focusable = Array.from(
    sidebarRef.value.querySelectorAll<HTMLElement>('a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])')
  ).filter((element) => element.offsetParent !== null)

  if (!focusable.length) {
    return
  }

  const first = focusable[0]
  const last = focusable[focusable.length - 1]

  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

onMounted(() => {
  mobileMediaQuery = window.matchMedia('(max-width: 820px)')
  syncMobileViewport()
  mobileMediaQuery.addEventListener('change', syncMobileViewport)
  checkAdminEntry()
  refreshCredits()
})

onBeforeUnmount(() => {
  mobileMediaQuery?.removeEventListener('change', syncMobileViewport)
})
</script>
