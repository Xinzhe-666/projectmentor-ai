<template>
  <el-alert
    v-if="experienceStore.fallback.active"
    class="experience-fallback-notice"
    type="info"
    show-icon
    :closable="false"
    :title="t('settings.experience.unavailable')"
    :description="t('settings.experience.fallbackDescription')"
  />
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import { useExperienceStore } from '@/stores/experience'

const { t } = useI18n()
const experienceStore = useExperienceStore()
</script>

<style scoped>
.experience-fallback-notice {
  margin-bottom: 20px;
}
</style>
