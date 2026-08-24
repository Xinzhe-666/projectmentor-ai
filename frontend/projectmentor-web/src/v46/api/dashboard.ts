import { request } from './request'
import type { DashboardSummary } from '@/v46/types/api'

export function getDashboardSummary() {
  return request<DashboardSummary>({
    url: '/api/dashboard/summary',
    method: 'get'
  })
}
