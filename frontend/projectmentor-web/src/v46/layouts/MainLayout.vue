<template>
  <div class="shell">
    <div v-if="navigationOpen" class="workspace-nav-scrim" aria-hidden="true" @click="closeNavigation()" />
    <AppSidebar :mobile-open="navigationOpen" @close="closeNavigation()" />
    <section class="shell-main" :inert="navigationOpen" :aria-hidden="navigationOpen || undefined">
      <AppHeader @toggle-navigation="openNavigation" />
      <main class="page-container app-content">
        <RouterView />
      </main>
      <AppFooter />
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import AppFooter from '@/v46/components/AppFooter.vue'
import AppHeader from '@/v46/components/AppHeader.vue'
import AppSidebar from '@/v46/components/AppSidebar.vue'
import { useNavigationDrawer } from '@/composables/useNavigationDrawer'

const route = useRoute()
const { navigationOpen, openNavigation, closeNavigation } = useNavigationDrawer(route)
</script>

<style scoped>
.app-content {
  flex: 1;
}
</style>
