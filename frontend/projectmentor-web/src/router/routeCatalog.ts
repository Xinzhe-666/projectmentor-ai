export type ExperienceLayoutKey = 'standalone' | 'main'

export interface RouteCatalogMeta {
  layout: ExperienceLayoutKey
  requiresAuth?: boolean
  guestOnly?: boolean
}

export interface RouteCatalogEntry {
  key: string
  path: string
  name: string
  meta: RouteCatalogMeta
}

const applicationRouteCatalog = [
  {
    key: 'landing',
    path: '/',
    name: 'landing',
    meta: { layout: 'standalone' }
  },
  {
    key: 'login',
    path: '/login',
    name: 'login',
    meta: { layout: 'standalone', guestOnly: true }
  },
  {
    key: 'register',
    path: '/register',
    name: 'register',
    meta: { layout: 'standalone', guestOnly: true }
  },
  {
    key: 'public-report',
    path: '/share/reports/:token',
    name: 'public-report',
    meta: { layout: 'standalone' }
  },
  {
    key: 'dashboard',
    path: '/dashboard',
    name: 'dashboard',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'projects',
    path: '/projects',
    name: 'projects',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'project-create',
    path: '/projects/create',
    name: 'project-create',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'project-detail',
    path: '/projects/:id',
    name: 'project-detail',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'project-defense',
    path: '/projects/:id/defense',
    name: 'project-defense',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'reports',
    path: '/reports',
    name: 'reports',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'report-detail',
    path: '/reports/:id',
    name: 'report-detail',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'hallucination',
    path: '/hallucination',
    name: 'hallucination',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'interview',
    path: '/interview',
    name: 'interview',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'interviews',
    path: '/interviews',
    name: 'interviews',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'credits',
    path: '/credits',
    name: 'credits',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'settings',
    path: '/settings',
    name: 'settings',
    meta: { layout: 'main', requiresAuth: true }
  },
  {
    key: 'admin',
    path: '/admin',
    name: 'admin',
    meta: { layout: 'main', requiresAuth: true }
  }
] as const satisfies readonly RouteCatalogEntry[]

const developmentExperienceRoutes = [
  {
    key: 'experience-workbench-only-test',
    path: '/__experience/workbench-only',
    name: 'experience-workbench-only-test',
    meta: { layout: 'main' }
  }
] as const satisfies readonly RouteCatalogEntry[]

export const routeCatalog = [
  ...applicationRouteCatalog,
  ...(import.meta.env.DEV ? developmentExperienceRoutes : [])
] as const

export type ExperienceRouteKey =
  | (typeof applicationRouteCatalog)[number]['key']
  | (typeof developmentExperienceRoutes)[number]['key']
export type RouteCatalogItem = (typeof routeCatalog)[number]

declare module 'vue-router' {
  interface RouteMeta {
    experienceRouteKey?: ExperienceRouteKey
    layout?: ExperienceLayoutKey
    requiresAuth?: boolean
    guestOnly?: boolean
  }
}
