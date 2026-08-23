import { createI18n } from 'vue-i18n'

import enUS from './en-US'
import zhCN from './zh-CN'
import landingV5EnUS from './landing-v5.en-US'
import landingV5ZhCN from './landing-v5.zh-CN'
import workspaceV5EnUS from './workspace-v5.en-US'
import workspaceV5ZhCN from './workspace-v5.zh-CN'
import projectDetailV5EnUS from './project-detail-v5.en-US'
import projectDetailV5ZhCN from './project-detail-v5.zh-CN'
import reportV5EnUS from './report-v5.en-US'
import reportV5ZhCN from './report-v5.zh-CN'
import evidenceQaV6EnUS from './evidence-qa-v6.en-US'
import evidenceQaV6ZhCN from './evidence-qa-v6.zh-CN'
import settingsEnUS from './settings.en-US'
import settingsZhCN from './settings.zh-CN'
import defenseEnUS from './defense.en-US'
import defenseZhCN from './defense.zh-CN'

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const LOCALE_STORAGE_KEY = 'pmai-locale'

const messages = {
  'zh-CN': {
    ...zhCN,
    landing: { ...zhCN.landing, v5: landingV5ZhCN },
    dashboard: { ...zhCN.dashboard, v5: workspaceV5ZhCN.dashboard },
    projects: { ...zhCN.projects, v5: projectDetailV5ZhCN },
    qa: { ...zhCN.qa, v6: evidenceQaV6ZhCN },
    reportV5: reportV5ZhCN,
    shellV5: workspaceV5ZhCN.shell,
    settings: settingsZhCN,
    defense: defenseZhCN
  },
  'en-US': {
    ...enUS,
    landing: { ...enUS.landing, v5: landingV5EnUS },
    dashboard: { ...enUS.dashboard, v5: workspaceV5EnUS.dashboard },
    projects: { ...enUS.projects, v5: projectDetailV5EnUS },
    qa: { ...enUS.qa, v6: evidenceQaV6EnUS },
    reportV5: reportV5EnUS,
    shellV5: workspaceV5EnUS.shell,
    settings: settingsEnUS,
    defense: defenseEnUS
  }
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
