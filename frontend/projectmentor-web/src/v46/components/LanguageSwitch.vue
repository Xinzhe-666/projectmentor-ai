<template>
  <div class="language-switch" role="group" :aria-label="t('lang.label')">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      :class="{ active: currentLocale === option.value }"
      @click="switchLocale(option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { persistLocale, type SupportedLocale } from '@/locales'

const { locale, t } = useI18n()

const options: Array<{ label: string; value: SupportedLocale }> = [
  { label: '中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' }
]

const currentLocale = computed(() => locale.value as SupportedLocale)

function switchLocale(value: SupportedLocale) {
  locale.value = value
  persistLocale(value)
}
</script>

<style scoped>
.language-switch {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 24px rgba(31, 111, 235, 0.08);
  backdrop-filter: blur(14px);
}

.language-switch button {
  min-width: 64px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: #475467;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  padding: 9px 12px;
  transition:
    background 180ms ease,
    color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.language-switch button:hover {
  color: var(--pm-primary);
  transform: translateY(-1px);
}

.language-switch button.active {
  background: linear-gradient(135deg, var(--pm-primary), var(--pm-teal));
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(31, 111, 235, 0.18);
}

@media (max-width: 620px) {
  .language-switch {
    width: 100%;
  }

  .language-switch button {
    flex: 1;
  }
}
</style>
