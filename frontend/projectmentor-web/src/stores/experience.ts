import { defineStore } from 'pinia'

import type {
  ExperienceMode,
  ExperienceResolutionStatus
} from '@/types/experience'

export const EXPERIENCE_STORAGE_KEY = 'pmai-experience-mode:v1'
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

function isExperienceMode(value: string | null): value is ExperienceMode {
  return value === 'classic' || value === 'workbench'
}

function readStoredExperienceMode(): ExperienceMode {
  try {
    const storedMode = localStorage.getItem(EXPERIENCE_STORAGE_KEY)
    const validMode = isExperienceMode(storedMode) ? storedMode : DEFAULT_EXPERIENCE_MODE
    return validMode === 'workbench' && !WORKBENCH_EXPERIENCE_ENABLED
      ? DEFAULT_EXPERIENCE_MODE
      : validMode
  } catch {
    return DEFAULT_EXPERIENCE_MODE
  }
}

function persistExperienceMode(mode: ExperienceMode) {
  try {
    localStorage.setItem(EXPERIENCE_STORAGE_KEY, mode)
  } catch {
    // A storage failure must not prevent the selected experience from running.
  }
}

function applyExperienceAttribute(mode: ExperienceMode) {
  document.documentElement.dataset.experience = mode
}

export const useExperienceStore = defineStore('experience', {
  state: (): ExperienceStoreState => {
    const initialMode = readStoredExperienceMode()

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
      persistExperienceMode(mode)
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
