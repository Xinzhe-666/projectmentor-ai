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

const ClassicMainLayout = () => import('@/v46/layouts/MainLayout.vue')
const WorkbenchMainLayout = () => import('@/layouts/MainLayout.vue')
const ExperienceUnavailableBoundary = () => import('@/experiences/ExperienceUnavailableBoundary.vue')
const ClassicLandingView = () => import('@/v46/views/LandingView.vue')
const WorkbenchLandingView = () => import('@/views/LandingView.vue')
const ClassicLoginView = () => import('@/v46/views/LoginView.vue')
const WorkbenchLoginView = () => import('@/views/LoginView.vue')
const ClassicRegisterView = () => import('@/v46/views/RegisterView.vue')
const WorkbenchRegisterView = () => import('@/views/RegisterView.vue')
const ClassicDashboardView = () => import('@/v46/views/DashboardView.vue')
const WorkbenchDashboardView = () => import('@/views/DashboardView.vue')
const ClassicProjectListView = () => import('@/v46/views/ProjectListView.vue')
const WorkbenchProjectListView = () => import('@/views/ProjectListView.vue')
const ClassicProjectCreateView = () => import('@/v46/views/ProjectCreateView.vue')
const WorkbenchProjectCreateView = () => import('@/views/ProjectCreateView.vue')
const ClassicProjectDetailView = () => import('@/v46/views/ProjectDetailView.vue')
const WorkbenchProjectDetailView = () => import('@/views/ProjectDetailView.vue')
const WorkbenchProjectDefenseView = () => import('@/views/ProjectDefenseView.vue')
const ClassicReportListView = () => import('@/v46/views/ReportListView.vue')
const WorkbenchReportListView = () => import('@/views/ReportListView.vue')
const ClassicReportDetailView = () => import('@/v46/views/ReportDetailView.vue')
const WorkbenchReportDetailView = () => import('@/views/ReportDetailView.vue')
const ClassicPublicReportView = () => import('@/v46/views/PublicReportView.vue')
const WorkbenchPublicReportView = () => import('@/views/PublicReportView.vue')
const ClassicHallucinationCheckView = () => import('@/v46/views/HallucinationCheckView.vue')
const WorkbenchHallucinationCheckView = () => import('@/views/HallucinationCheckView.vue')
const ClassicInterviewView = () => import('@/v46/views/InterviewView.vue')
const WorkbenchInterviewView = () => import('@/views/InterviewView.vue')
const ClassicInterviewListView = () => import('@/v46/views/InterviewListView.vue')
const WorkbenchInterviewListView = () => import('@/views/InterviewListView.vue')
const ClassicCreditView = () => import('@/v46/views/CreditView.vue')
const WorkbenchCreditView = () => import('@/views/CreditView.vue')
const WorkbenchSettingsView = () => import('@/views/SettingsView.vue')
const ClassicAdminDashboardView = () => import('@/v46/views/AdminDashboardView.vue')
const WorkbenchAdminDashboardView = () => import('@/views/AdminDashboardView.vue')

const experienceLayoutRegistry: Record<ExperienceRouteLayout, ExperienceComponentLoader> = {
  classic: ClassicMainLayout,
  workbench: WorkbenchMainLayout,
  shared: WorkbenchMainLayout
}

export const experienceRegistry: ExperienceRegistry = {
  landing: hybridRoute('landing', ClassicLandingView, WorkbenchLandingView),
  login: hybridRoute('login', ClassicLoginView, WorkbenchLoginView),
  register: hybridRoute('register', ClassicRegisterView, WorkbenchRegisterView),
  'public-report': hybridRoute('public-report', ClassicPublicReportView, WorkbenchPublicReportView),
  dashboard: hybridRoute('dashboard', ClassicDashboardView, WorkbenchDashboardView),
  projects: hybridRoute('projects', ClassicProjectListView, WorkbenchProjectListView),
  'project-create': hybridRoute('project-create', ClassicProjectCreateView, WorkbenchProjectCreateView),
  'project-detail': hybridRoute('project-detail', ClassicProjectDetailView, WorkbenchProjectDetailView),
  'project-defense': {
    key: 'project-defense',
    workbench: WorkbenchProjectDefenseView,
    unavailablePolicy: 'show-boundary',
    layout: 'workbench'
  },
  reports: hybridRoute('reports', ClassicReportListView, WorkbenchReportListView),
  'report-detail': hybridRoute('report-detail', ClassicReportDetailView, WorkbenchReportDetailView),
  hallucination: hybridRoute('hallucination', ClassicHallucinationCheckView, WorkbenchHallucinationCheckView),
  interview: hybridRoute('interview', ClassicInterviewView, WorkbenchInterviewView),
  interviews: hybridRoute('interviews', ClassicInterviewListView, WorkbenchInterviewListView),
  credits: hybridRoute('credits', ClassicCreditView, WorkbenchCreditView),
  settings: {
    key: 'settings',
    workbench: WorkbenchSettingsView,
    unavailablePolicy: 'show-boundary',
    layout: 'workbench'
  },
  admin: hybridRoute('admin', ClassicAdminDashboardView, WorkbenchAdminDashboardView),
  'experience-workbench-only-test': {
    key: 'experience-workbench-only-test',
    workbench: WorkbenchSettingsView,
    unavailablePolicy: 'show-boundary',
    layout: 'workbench'
  }
}

function hybridRoute<Key extends ExperienceRouteKey>(
  key: Key,
  classic: ExperienceComponentLoader,
  workbench: ExperienceComponentLoader
): ExperienceRoute & { key: Key } {
  return {
    key,
    classic,
    workbench,
    unavailablePolicy: 'fallback-classic',
    layout: 'shared'
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
  const layoutKey = entry.layout === 'shared' ? resolvedExperience : entry.layout

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
