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
import { useRoute } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
import ExperienceFallbackNotice from '@/components/ExperienceFallbackNotice.vue'
import { useNavigationDrawer } from '@/composables/useNavigationDrawer'

const route = useRoute()
const { navigationOpen, openNavigation, closeNavigation } = useNavigationDrawer(route)
</script>

<style scoped>
.app-content {
  flex: 1;
}
</style>
