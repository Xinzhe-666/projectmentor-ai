import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'

export function getClassicRouteRedirect(
  route: Pick<RouteLocationNormalized, 'name' | 'params' | 'query' | 'hash'>
): RouteLocationRaw | null {
  if (route.name === 'project-defense') {
    return {
      name: 'project-detail',
      params: { id: route.params.id },
      query: route.query,
      hash: route.hash
    }
  }

  if (route.name === 'settings' || route.name === 'experience-workbench-only-test') {
    return { name: 'dashboard' }
  }

  return null
}
