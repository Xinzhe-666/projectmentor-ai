<template>
  <component
    :is="resolvedLayout"
    :key="resolution.layout.key"
  />
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, type Component, watch } from 'vue'

import { resolveExperienceRoute, type ExperienceComponentLoader } from '@/experiences/registry'
import type { ExperienceRouteKey } from '@/router/routeCatalog'
import { useExperienceStore } from '@/stores/experience'

const props = defineProps<{
  routeKey: ExperienceRouteKey
}>()

const experienceStore = useExperienceStore()
const componentCache = new Map<ExperienceComponentLoader, Component>()

const resolution = computed(() =>
  resolveExperienceRoute(props.routeKey, experienceStore.experienceMode)
)

const resolvedLayout = computed(() => {
  const loader = resolution.value.layout.loader
  const cachedLayout = componentCache.get(loader)

  if (cachedLayout) {
    return cachedLayout
  }

  const asyncLayout = defineAsyncComponent(loader)
  componentCache.set(loader, asyncLayout)
  return asyncLayout
})

watch(
  resolution,
  (currentResolution) => {
    experienceStore.setRouteResolution(
      props.routeKey,
      currentResolution.resolvedExperience,
      currentResolution.status,
      currentResolution.unavailableTarget
    )
  },
  { immediate: true, flush: 'sync' }
)
</script>
