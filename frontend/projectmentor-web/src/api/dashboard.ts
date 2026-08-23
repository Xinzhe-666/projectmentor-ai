import { request } from './request'
import type { DashboardSummary } from '@/types/api'

export function getDashboardSummary() {
  return request<DashboardSummary>({
    url: '/api/dashboard/summary',
    method: 'get',
    silentError: true
  })
}
