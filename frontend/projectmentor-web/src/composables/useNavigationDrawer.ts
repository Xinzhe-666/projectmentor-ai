import { nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

export function useNavigationDrawer(
  route: RouteLocationNormalizedLoaded,
  closeButtonSelector = '.sidebar-close',
  toggleButtonSelector = '.workspace-nav-toggle'
) {
  const navigationOpen = ref(false)

  function openNavigation() {
    navigationOpen.value = true
    nextTick(() => document.querySelector<HTMLButtonElement>(closeButtonSelector)?.focus())
  }

  function closeNavigation(restoreFocus = true) {
    const wasOpen = navigationOpen.value
    navigationOpen.value = false
    if (wasOpen && restoreFocus) {
      nextTick(() => document.querySelector<HTMLButtonElement>(toggleButtonSelector)?.focus())
    }
  }

  watch(() => route.fullPath, () => closeNavigation(false))
  watch(navigationOpen, (open) => {
    document.documentElement.classList.toggle('workspace-nav-open', open)
  })
  onBeforeUnmount(() => document.documentElement.classList.remove('workspace-nav-open'))

  return { navigationOpen: navigationOpen as Ref<boolean>, openNavigation, closeNavigation }
}
