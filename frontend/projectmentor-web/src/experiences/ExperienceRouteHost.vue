<template>
  <component
    :is="resolvedComponent"
    :key="routeKey"
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

const resolvedComponent = computed(() => {
  const loader = resolution.value.component
  const cachedComponent = componentCache.get(loader)

  if (cachedComponent) {
    return cachedComponent
  }

  const asyncComponent = defineAsyncComponent(loader)
  componentCache.set(loader, asyncComponent)
  return asyncComponent
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
