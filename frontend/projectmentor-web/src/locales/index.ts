import { createI18n } from 'vue-i18n'

import enUS from './en-US'
import zhCN from './zh-CN'

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const LOCALE_STORAGE_KEY = 'pmai-locale'

const messages = {
  'zh-CN': zhCN,
  'en-US': enUS
}

function isSupportedLocale(value: string | null | undefined): value is SupportedLocale {
  return Boolean(value && SUPPORTED_LOCALES.includes(value as SupportedLocale))
}

function normalizeLocale(value: string | null | undefined): SupportedLocale | null {
  if (!value) {
    return null
  }

  if (isSupportedLocale(value)) {
    return value
  }

  const normalized = value.toLowerCase()
  if (normalized.startsWith('zh')) {
    return 'zh-CN'
  }

  if (normalized.startsWith('en')) {
    return 'en-US'
  }

  return null
}

export function getInitialLocale(): SupportedLocale {
  const savedLocale = normalizeLocale(localStorage.getItem(LOCALE_STORAGE_KEY))
  if (savedLocale) {
    return savedLocale
  }

  const browserLocale = normalizeLocale(navigator.language)
  return browserLocale || 'zh-CN'
}

export function persistLocale(locale: SupportedLocale) {
  localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  document.documentElement.lang = locale
}

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages
})

document.documentElement.lang = i18n.global.locale.value
