<template>
  <div class="language-switch" role="group" :aria-label="t('lang.label')">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      :class="{ active: currentLocale === option.value }"
      :aria-pressed="currentLocale === option.value"
      :title="option.title"
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

const options: Array<{ label: string; title: string; value: SupportedLocale }> = [
  { label: '中', title: '中文', value: 'zh-CN' },
  { label: 'EN', title: 'English', value: 'en-US' }
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
  border: 1px solid var(--pm-stone-strong);
  border-radius: var(--pm-radius-sm);
  background: var(--pm-surface);
}

.language-switch button {
  min-width: 40px;
  min-height: 44px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #475467;
  cursor: pointer;
  font: inherit;
  font-family: var(--pm-font-mono);
  font-size: 10px;
  font-weight: 400;
  line-height: 1;
  padding: 0 8px;
  transition:
    background 180ms ease,
    color 180ms ease;
}

.language-switch button + button {
  border-left: 1px solid var(--pm-stone-strong);
}

.language-switch button:hover {
  color: var(--pm-primary);
}

.language-switch button.active {
  background: var(--pm-primary-soft);
  color: var(--pm-primary-dark);
}

</style>
