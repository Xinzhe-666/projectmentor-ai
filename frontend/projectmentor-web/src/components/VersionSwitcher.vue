<template>
  <nav class="version-switcher" :aria-label="t('versionSwitcher.label')">
    <span class="version-switcher__label">{{ t('versionSwitcher.label') }}</span>
    <div class="version-switcher__options" role="group">
      <button
        v-for="option in options"
        :key="option.mode"
        class="version-switcher__option"
        :class="{ 'is-active': experienceStore.experienceMode === option.mode }"
        type="button"
        :aria-pressed="experienceStore.experienceMode === option.mode"
        :disabled="option.disabled"
        @click="selectExperience(option.mode)"
      >
        <span class="version-switcher__option-label version-switcher__option-label--full">{{ option.label }}</span>
        <span class="version-switcher__option-label version-switcher__option-label--compact">{{ option.compactLabel }}</span>
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import { getClassicRouteRedirect } from '@/experiences/classicRedirect'
import { useExperienceStore } from '@/stores/experience'
import type { ExperienceMode } from '@/types/experience'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const experienceStore = useExperienceStore()

const options = computed(() => [
  {
    mode: 'classic' as const,
    label: t('versionSwitcher.classic'),
    compactLabel: t('versionSwitcher.classicCompact'),
    disabled: false
  },
  {
    mode: 'workbench' as const,
    label: t('versionSwitcher.workbench'),
    compactLabel: t('versionSwitcher.workbenchCompact'),
    disabled: !experienceStore.workbenchEnabled
  }
])

async function selectExperience(mode: ExperienceMode) {
  if (mode === experienceStore.experienceMode) return

  if (mode === 'classic') {
    const classicRedirect = getClassicRouteRedirect(route)

    if (classicRedirect) {
      await router.replace(classicRedirect)
    }
  }

  experienceStore.setExperienceMode(mode)
}
</script>
