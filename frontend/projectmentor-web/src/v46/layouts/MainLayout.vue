<template>
  <div class="shell">
    <AppSidebar />
    <section class="shell-main">
      <AppHeader />
      <main class="page-container app-content">
        <RouterView />
      </main>
      <AppFooter />
    </section>
  </div>
</template>

<script setup lang="ts">
import AppFooter from '@/v46/components/AppFooter.vue'
import AppHeader from '@/v46/components/AppHeader.vue'
import AppSidebar from '@/v46/components/AppSidebar.vue'
</script>

<style scoped>
.app-content {
  flex: 1;
}
</style>
