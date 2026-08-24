import { defineStore } from 'pinia'

import type {
  ExperienceMode,
  ExperienceResolutionStatus
} from '@/types/experience'

export const DEFAULT_EXPERIENCE_MODE: ExperienceMode = 'classic'

function readWorkbenchFeatureFlag(): boolean {
  const configuredValue = import.meta.env.VITE_WORKBENCH_EXPERIENCE_ENABLED

  if (configuredValue === undefined || configuredValue === '') {
    return import.meta.env.DEV
  }

  return ['1', 'true', 'yes', 'on'].includes(configuredValue.toLowerCase())
}

export const WORKBENCH_EXPERIENCE_ENABLED = readWorkbenchFeatureFlag()

interface ExperienceFallbackState {
  active: boolean
  routeKey: string | null
  requestedMode: ExperienceMode
  resolvedMode: ExperienceMode
}

interface ExperienceUnavailableState {
  active: boolean
  routeKey: string | null
  targetMode: ExperienceMode | null
}

interface ExperienceStoreState {
  experienceMode: ExperienceMode
  activeExperienceMode: ExperienceMode
  workbenchEnabled: boolean
  resolutionStatus: ExperienceResolutionStatus
  fallback: ExperienceFallbackState
  unavailable: ExperienceUnavailableState
}

function applyExperienceAttribute(mode: ExperienceMode) {
  document.documentElement.dataset.experience = mode
}

export const useExperienceStore = defineStore('experience', {
  state: (): ExperienceStoreState => {
    const initialMode = DEFAULT_EXPERIENCE_MODE

    return {
      experienceMode: initialMode,
      activeExperienceMode: initialMode,
      workbenchEnabled: WORKBENCH_EXPERIENCE_ENABLED,
      resolutionStatus: 'direct',
      fallback: {
        active: false,
        routeKey: null,
        requestedMode: initialMode,
        resolvedMode: initialMode
      },
      unavailable: {
        active: false,
        routeKey: null,
        targetMode: null
      }
    }
  },
  actions: {
    setExperienceMode(mode: ExperienceMode) {
      if (mode === 'workbench' && !this.workbenchEnabled) {
        return
      }

      this.experienceMode = mode
      applyExperienceAttribute(mode)
    },
    setRouteResolution(
      routeKey: string,
      resolvedMode: ExperienceMode,
      status: ExperienceResolutionStatus,
      unavailableTarget: ExperienceMode | null = null
    ) {
      this.activeExperienceMode = resolvedMode
      this.resolutionStatus = status
      this.fallback = {
        active: status === 'fallback',
        routeKey: status === 'fallback' ? routeKey : null,
        requestedMode: this.experienceMode,
        resolvedMode
      }
      this.unavailable = {
        active: status === 'boundary',
        routeKey: status === 'boundary' ? routeKey : null,
        targetMode: status === 'boundary' ? unavailableTarget : null
      }
      applyExperienceAttribute(resolvedMode)
    }
  }
})
