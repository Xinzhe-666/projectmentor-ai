<template>
  <VersionSwitcher />
  <RouterView />
</template>

<script setup lang="ts">
import { watch } from 'vue'

import VersionSwitcher from '@/components/VersionSwitcher.vue'
import { useExperienceStore } from '@/stores/experience'

const experienceStore = useExperienceStore()

watch(
  () => experienceStore.experienceMode,
  (mode) => {
    document.documentElement.dataset.experience = mode
  },
  { immediate: true }
)
</script>
