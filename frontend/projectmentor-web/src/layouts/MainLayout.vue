<template>
  <div class="shell">
    <div
      v-if="navigationOpen"
      class="workspace-nav-scrim"
      aria-hidden="true"
      @click="closeNavigation()"
    />
    <AppSidebar :mobile-open="navigationOpen" @close="closeNavigation()" />
    <section class="shell-main" :inert="navigationOpen" :aria-hidden="navigationOpen || undefined">
      <AppHeader @toggle-navigation="openNavigation" />
      <main class="page-container app-content">
        <ExperienceFallbackNotice />
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import ExperienceFallbackNotice from '@/components/ExperienceFallbackNotice.vue'

const route = useRoute()
const navigationOpen = ref(false)

function openNavigation() {
  navigationOpen.value = true
  nextTick(() => document.querySelector<HTMLButtonElement>('.sidebar-close')?.focus())
}

function closeNavigation(restoreFocus = true) {
  const wasOpen = navigationOpen.value
  navigationOpen.value = false

  if (wasOpen && restoreFocus) {
    nextTick(() => document.querySelector<HTMLButtonElement>('.workspace-nav-toggle')?.focus())
  }
}

watch(
  () => route.fullPath,
  () => closeNavigation(false)
)

watch(navigationOpen, (open) => {
  document.documentElement.classList.toggle('workspace-nav-open', open)
})

onBeforeUnmount(() => {
  document.documentElement.classList.remove('workspace-nav-open')
})
</script>

<style scoped>
.app-content {
  flex: 1;
}
</style>
