<template>
  <div class="experience-unavailable-boundary">
    <EmptyState :title="boundaryTitle">
      <el-button
        v-if="canSwitch"
        type="primary"
        @click="switchExperience"
      >
        {{ switchLabel }}
      </el-button>
    </EmptyState>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import EmptyState from '@/components/EmptyState.vue'
import { useExperienceStore } from '@/stores/experience'

const { t } = useI18n()
const experienceStore = useExperienceStore()

const targetMode = computed(() => experienceStore.unavailable.targetMode)

const boundaryTitle = computed(() => {
  if (targetMode.value === 'workbench') {
    return t('settings.experience.onlyWorkbench')
  }

  if (targetMode.value === 'classic') {
    return t('settings.experience.onlyClassic')
  }

  return t('settings.experience.unavailableFeature')
})

const canSwitch = computed(() =>
  targetMode.value === 'classic'
  || targetMode.value === 'workbench' && experienceStore.workbenchEnabled
)

const switchLabel = computed(() =>
  targetMode.value === 'workbench'
    ? t('settings.experience.switchToWorkbench')
    : t('settings.experience.switchToClassic')
)

function switchExperience() {
  if (targetMode.value) {
    experienceStore.setExperienceMode(targetMode.value)
  }
}
</script>

<style scoped>
.experience-unavailable-boundary {
  width: 100%;
  max-width: 840px;
}

.experience-unavailable-boundary :deep(.empty-state) {
  display: grid;
  align-content: center;
  justify-items: center;
}
</style>
