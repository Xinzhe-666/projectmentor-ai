import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const LandingView = () => import('@/views/LandingView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const MainLayout = () => import('@/layouts/MainLayout.vue')
const DashboardView = () => import('@/views/DashboardView.vue')
const ProjectListView = () => import('@/views/ProjectListView.vue')
const ProjectCreateView = () => import('@/views/ProjectCreateView.vue')
const ProjectDetailView = () => import('@/views/ProjectDetailView.vue')
const ReportDetailView = () => import('@/views/ReportDetailView.vue')
const PublicReportView = () => import('@/views/PublicReportView.vue')
const HallucinationCheckView = () => import('@/views/HallucinationCheckView.vue')
const InterviewView = () => import('@/views/InterviewView.vue')
const CreditView = () => import('@/views/CreditView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'landing',
      component: LandingView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView
    },
    {
      path: '/share/reports/:token',
      name: 'public-report',
      component: PublicReportView
    },
    {
      path: '/dashboard',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'dashboard', component: DashboardView }]
    },
    {
      path: '/projects',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'projects', component: ProjectListView }]
    },
    {
      path: '/projects/create',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'project-create', component: ProjectCreateView }]
    },
    {
      path: '/projects/:id',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'project-detail', component: ProjectDetailView }]
    },
    {
      path: '/reports/:id',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'report-detail', component: ReportDetailView }]
    },
    {
      path: '/hallucination',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'hallucination', component: HallucinationCheckView }]
    },
    {
      path: '/interview',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'interview', component: InterviewView }]
    },
    {
      path: '/credits',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [{ path: '', name: 'credits', component: CreditView }]
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  const publicPages = ['/', '/login', '/register']

  if (!publicPages.includes(to.path) && to.matched.some((route) => route.meta.requiresAuth) && !userStore.isLoggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    return '/dashboard'
  }

  return true
})

export default router
