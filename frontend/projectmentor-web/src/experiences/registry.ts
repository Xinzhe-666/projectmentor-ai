import type { Component } from 'vue'

import type { ExperienceRouteKey } from '@/router/routeCatalog'
import type {
  ExperienceMode,
  ExperienceResolutionStatus,
  ExperienceRouteLayout
} from '@/types/experience'

export type ExperienceComponentLoader = () => Promise<{ default: Component }>

export type ExperienceUnavailablePolicy = 'fallback-classic' | 'show-boundary'

export interface ExperienceRoute {
  key: ExperienceRouteKey
  classic?: ExperienceComponentLoader
  workbench?: ExperienceComponentLoader
  unavailablePolicy: ExperienceUnavailablePolicy
  layout: ExperienceRouteLayout
}

type ExperienceRegistry = {
  [Key in ExperienceRouteKey]: ExperienceRoute & { key: Key }
}

export interface ResolvedExperienceLayout {
  key: ExperienceRouteLayout
  loader: ExperienceComponentLoader
}

export interface ResolvedExperienceRoute {
  component: ExperienceComponentLoader
  layout: ResolvedExperienceLayout
  requestedMode: ExperienceMode
  resolvedExperience: ExperienceMode
  status: ExperienceResolutionStatus
  fallback: boolean
  unavailable: boolean
  unavailableTarget: ExperienceMode | null
}

const MainLayout = () => import('@/layouts/MainLayout.vue')
const ExperienceUnavailableBoundary = () => import('@/experiences/ExperienceUnavailableBoundary.vue')
const LandingView = () => import('@/views/LandingView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const DashboardView = () => import('@/views/DashboardView.vue')
const ProjectListView = () => import('@/views/ProjectListView.vue')
const ProjectCreateView = () => import('@/views/ProjectCreateView.vue')
const ProjectDetailView = () => import('@/views/ProjectDetailView.vue')
const ProjectDefenseView = () => import('@/views/ProjectDefenseView.vue')
const ReportListView = () => import('@/views/ReportListView.vue')
const ReportDetailView = () => import('@/views/ReportDetailView.vue')
const PublicReportView = () => import('@/views/PublicReportView.vue')
const HallucinationCheckView = () => import('@/views/HallucinationCheckView.vue')
const InterviewView = () => import('@/views/InterviewView.vue')
const InterviewListView = () => import('@/views/InterviewListView.vue')
const CreditView = () => import('@/views/CreditView.vue')
const SettingsView = () => import('@/views/SettingsView.vue')
const AdminDashboardView = () => import('@/views/AdminDashboardView.vue')

// The three ownership slots intentionally alias the current MainLayout during
// the foundation phase. Distinct keys prevent a page from crossing experience
// ownership when dedicated layouts are extracted later.
const experienceLayoutRegistry: Record<ExperienceRouteLayout, ExperienceComponentLoader> = {
  classic: MainLayout,
  workbench: MainLayout,
  shared: MainLayout
}

// Phase 6.1 introduces the routing boundary without moving or copying existing pages.
// Entries that already represent the Workbench direction intentionally alias the
// current component in both modes until the stable Classic sources are extracted.
export const experienceRegistry: ExperienceRegistry = {
  landing: hybridRoute('landing', LandingView),
  login: hybridRoute('login', LoginView),
  register: hybridRoute('register', RegisterView),
  'public-report': hybridRoute('public-report', PublicReportView),
  dashboard: hybridRoute('dashboard', DashboardView),
  projects: classicOnlyRoute('projects', ProjectListView),
  'project-create': classicOnlyRoute('project-create', ProjectCreateView),
  'project-detail': hybridRoute('project-detail', ProjectDetailView),
  'project-defense': {
    key: 'project-defense',
    workbench: ProjectDefenseView,
    unavailablePolicy: 'show-boundary',
    layout: 'workbench'
  },
  reports: hybridRoute('reports', ReportListView),
  'report-detail': hybridRoute('report-detail', ReportDetailView),
  hallucination: classicOnlyRoute('hallucination', HallucinationCheckView),
  interview: classicOnlyRoute('interview', InterviewView),
  interviews: classicOnlyRoute('interviews', InterviewListView),
  credits: classicOnlyRoute('credits', CreditView),
  settings: hybridRoute('settings', SettingsView),
  admin: classicOnlyRoute('admin', AdminDashboardView),
  'experience-workbench-only-test': {
    key: 'experience-workbench-only-test',
    workbench: SettingsView,
    unavailablePolicy: 'show-boundary',
    layout: 'workbench'
  }
}

function hybridRoute<Key extends ExperienceRouteKey>(
  key: Key,
  loader: ExperienceComponentLoader
): ExperienceRoute & { key: Key } {
  return {
    key,
    classic: loader,
    workbench: loader,
    unavailablePolicy: 'fallback-classic',
    layout: 'shared'
  }
}

function classicOnlyRoute<Key extends ExperienceRouteKey>(
  key: Key,
  loader: ExperienceComponentLoader
): ExperienceRoute & { key: Key } {
  return {
    key,
    classic: loader,
    unavailablePolicy: 'fallback-classic',
    layout: 'classic'
  }
}

export function resolveExperienceRoute(
  routeKey: ExperienceRouteKey,
  requestedMode: ExperienceMode
): ResolvedExperienceRoute {
  const entry = experienceRegistry[routeKey]

  if (requestedMode === 'classic') {
    if (entry.classic) {
      return createResolution(entry, entry.classic, requestedMode, 'classic', 'direct')
    }

    return createBoundaryResolution(entry, requestedMode, entry.workbench ? 'workbench' : null)
  }

  if (entry.workbench) {
    return createResolution(entry, entry.workbench, requestedMode, 'workbench', 'direct')
  }

  if (entry.unavailablePolicy === 'fallback-classic' && entry.classic) {
    return createResolution(entry, entry.classic, requestedMode, 'classic', 'fallback')
  }

  return createBoundaryResolution(entry, requestedMode, entry.classic ? 'classic' : null)
}

function createResolution(
  entry: ExperienceRoute,
  component: ExperienceComponentLoader,
  requestedMode: ExperienceMode,
  resolvedExperience: ExperienceMode,
  status: Exclude<ExperienceResolutionStatus, 'boundary'>
): ResolvedExperienceRoute {
  const layoutKey = entry.layout === 'shared' ? 'shared' : resolvedExperience

  return {
    component,
    layout: {
      key: layoutKey,
      loader: experienceLayoutRegistry[layoutKey]
    },
    requestedMode,
    resolvedExperience,
    status,
    fallback: status === 'fallback',
    unavailable: false,
    unavailableTarget: null
  }
}

function createBoundaryResolution(
  entry: ExperienceRoute,
  requestedMode: ExperienceMode,
  unavailableTarget: ExperienceMode | null
): ResolvedExperienceRoute {
  return {
    component: ExperienceUnavailableBoundary,
    layout: {
      key: requestedMode,
      loader: experienceLayoutRegistry[requestedMode]
    },
    requestedMode,
    resolvedExperience: requestedMode,
    status: 'boundary',
    fallback: false,
    unavailable: true,
    unavailableTarget
  }
}
