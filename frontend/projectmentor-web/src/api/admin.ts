import { request } from './request'
import type {
  AdminMe,
  AdminRecentProject,
  AdminRecentQa,
  AdminRecentReport,
  AdminRecentUser,
  AdminStats
} from '@/types/api'

export function getAdminMe() {
  return request<AdminMe>({
    url: '/api/admin/me',
    method: 'get'
  })
}

export function getAdminStats() {
  return request<AdminStats>({
    url: '/api/admin/stats',
    method: 'get'
  })
}

export function getAdminRecentUsers(limit = 10) {
  return request<AdminRecentUser[]>({
    url: '/api/admin/recent/users',
    method: 'get',
    params: { limit }
  })
}

export function getAdminRecentProjects(limit = 10) {
  return request<AdminRecentProject[]>({
    url: '/api/admin/recent/projects',
    method: 'get',
    params: { limit }
  })
}

export function getAdminRecentReports(limit = 10) {
  return request<AdminRecentReport[]>({
    url: '/api/admin/recent/reports',
    method: 'get',
    params: { limit }
  })
}

export function getAdminRecentQa(limit = 10) {
  return request<AdminRecentQa[]>({
    url: '/api/admin/recent/qa',
    method: 'get',
    params: { limit }
  })
}
