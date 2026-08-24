import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import ExperienceLayoutHost from '@/experiences/ExperienceLayoutHost.vue'
import ExperienceRouteHost from '@/experiences/ExperienceRouteHost.vue'
import { getClassicRouteRedirect } from '@/experiences/classicRedirect'
import { routeCatalog, type RouteCatalogItem } from '@/router/routeCatalog'
import { useExperienceStore } from '@/stores/experience'
import { useUserStore } from '@/stores/user'

function createRouteRecord(entry: RouteCatalogItem): RouteRecordRaw {
  const meta = {
    ...entry.meta,
    experienceRouteKey: entry.key
  }

  if (entry.meta.layout === 'main') {
    return {
      path: entry.path,
      component: ExperienceLayoutHost,
      props: { routeKey: entry.key },
      meta,
      children: [
        {
          path: '',
          name: entry.name,
          component: ExperienceRouteHost,
          props: { routeKey: entry.key }
        }
      ]
    }
  }

  return {
    path: entry.path,
    name: entry.name,
    component: ExperienceRouteHost,
    props: { routeKey: entry.key },
    meta
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: routeCatalog.map(createRouteRecord),
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const experienceStore = useExperienceStore()
  const userStore = useUserStore()

  if (experienceStore.experienceMode === 'classic') {
    const classicRedirect = getClassicRouteRedirect(to)

    if (classicRedirect) {
      return classicRedirect
    }
  }

  if (to.matched.some((route) => route.meta.requiresAuth) && !userStore.isLoggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (to.matched.some((route) => route.meta.guestOnly) && userStore.isLoggedIn) {
    return '/dashboard'
  }

  return true
})

export default router
