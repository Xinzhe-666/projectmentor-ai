import { request } from './request'
import type { PublicReport, ReportShare } from '@/types/api'

export function getReportShare(reportId: number) {
  return request<ReportShare>({
    url: `/api/reports/${reportId}/share`,
    method: 'get'
  })
}

export function createReportShare(reportId: number) {
  return request<ReportShare>({
    url: `/api/reports/${reportId}/share`,
    method: 'post'
  })
}

export function disableReportShare(reportId: number) {
  return request<void>({
    url: `/api/reports/${reportId}/share`,
    method: 'delete'
  })
}

export function getPublicReport(token: string) {
  return request<PublicReport>({
    url: `/api/share/reports/${token}`,
    method: 'get'
  })
}
