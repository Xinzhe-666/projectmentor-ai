<template>
  <div class="page-stack settings-page">
    <header class="settings-page-header">
      <h2>{{ t('settings.title') }}</h2>
      <p>{{ t('settings.description') }}</p>
    </header>

    <section class="panel settings-panel" aria-labelledby="experience-settings-title">
      <div class="panel-title settings-panel-title">
        <div>
          <h2 id="experience-settings-title">{{ t('settings.experience.title') }}</h2>
          <p class="muted">{{ t('settings.experience.description') }}</p>
        </div>
        <div class="current-experience" aria-live="polite">
          <span>{{ t('settings.experience.current') }}</span>
          <strong>{{ currentExperienceLabel }}</strong>
        </div>
      </div>

      <div class="panel-body">
        <fieldset class="experience-options">
          <legend>{{ t('settings.experience.current') }}</legend>

          <label
            v-for="option in experienceOptions"
            :key="option.mode"
            class="experience-option"
            :class="{
              'is-selected': experienceStore.experienceMode === option.mode,
              'is-disabled': option.disabled
            }"
          >
            <input
              type="radio"
              name="experience-mode"
              :value="option.mode"
              :checked="experienceStore.experienceMode === option.mode"
              :disabled="option.disabled"
              @change="selectExperience(option.mode)"
            >
            <span class="experience-option-copy">
              <strong>{{ option.title }}</strong>
              <span>{{ option.description }}</span>
            </span>
            <span
              v-if="experienceStore.experienceMode === option.mode"
              class="experience-option-state"
            >
              {{ t('settings.experience.selected') }}
            </span>
          </label>
        </fieldset>

        <p v-if="!experienceStore.workbenchEnabled" class="experience-feature-note" role="note">
          {{ t('settings.experience.disabled') }}
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { useExperienceStore } from '@/stores/experience'
import type { ExperienceMode } from '@/types/experience'

const { t } = useI18n()
const experienceStore = useExperienceStore()

const experienceOptions = computed(() => [
  {
    mode: 'classic' as const,
    title: t('settings.experience.classic.title'),
    description: t('settings.experience.classic.description'),
    disabled: false
  },
  {
    mode: 'workbench' as const,
    title: t('settings.experience.workbench.title'),
    description: t('settings.experience.workbench.description'),
    disabled: !experienceStore.workbenchEnabled
  }
])

const currentExperienceLabel = computed(() =>
  experienceOptions.value.find((option) => option.mode === experienceStore.activeExperienceMode)?.title
    || t('settings.experience.classic.title')
)

function selectExperience(mode: ExperienceMode) {
  experienceStore.setExperienceMode(mode)
}
</script>

<style scoped>
.settings-page {
  max-width: 960px;
}

.settings-page-header {
  max-width: 720px;
}

.settings-page-header h2 {
  margin: 0;
  color: var(--pm-ink);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: -0.025em;
  line-height: 1.2;
}

.settings-page-header p {
  margin: 10px 0 0;
  color: var(--pm-muted);
  font-size: 14px;
  line-height: 1.65;
}

.settings-panel-title {
  align-items: flex-start;
}

.current-experience {
  display: grid;
  min-width: 190px;
  gap: 6px;
  padding-left: 20px;
  border-left: 1px solid var(--pm-stone);
}

.current-experience span,
.experience-option-state {
  color: var(--pm-muted);
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  letter-spacing: 0.07em;
  line-height: 1.4;
  text-transform: uppercase;
}

.current-experience strong {
  color: var(--pm-ink);
  font-size: 14px;
  font-weight: 600;
}

.experience-options {
  margin: 0;
  padding: 0;
  border: 0;
  border-top: 1px solid var(--pm-stone);
}

.experience-options legend {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.experience-option {
  display: grid;
  min-height: 76px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 16px 4px;
  border-bottom: 1px solid var(--pm-stone);
  cursor: pointer;
  transition: background-color var(--pm-motion-fast) ease;
}

.experience-option:hover {
  background: var(--pm-paper);
}

.experience-option:has(input:focus-visible) {
  outline: 2px solid var(--pm-primary);
  outline-offset: 3px;
}

.experience-option.is-selected {
  background: var(--pm-primary-soft);
}

.experience-option.is-disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.experience-option input {
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: var(--pm-primary);
}

.experience-option-copy {
  display: grid;
  gap: 5px;
}

.experience-option-copy strong {
  color: var(--pm-ink);
  font-size: 15px;
  font-weight: 600;
}

.experience-option-copy > span {
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.55;
}

.experience-option-state {
  color: var(--pm-primary-dark);
}

.experience-feature-note {
  margin: 16px 0 0;
  color: var(--pm-muted);
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 680px) {
  .settings-panel-title {
    display: grid;
    gap: 18px;
  }

  .current-experience {
    min-width: 0;
    padding-top: 14px;
    padding-left: 0;
    border-top: 1px solid var(--pm-stone);
    border-left: 0;
  }

  .experience-option {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .experience-option-state {
    grid-column: 2;
  }
}
</style>
