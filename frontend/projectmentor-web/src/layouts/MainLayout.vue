<template>
  <div class="shell">
    <AppSidebar />
    <section class="shell-main">
      <AppHeader />
      <main class="page-container">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
</script>
