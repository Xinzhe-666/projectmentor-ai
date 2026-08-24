import { request } from './request'
import type { AnalysisReport, AnalysisTask, PageResult, ReportListItem, RuleScanResult } from '@/v46/types/api'

export interface ReportListParams {
  page?: number
  size?: number
  projectId?: number
  keyword?: string
}

export function scanProject(projectId: number) {
  return request<RuleScanResult>({
    url: `/api/projects/${projectId}/scan`,
    method: 'post'
  })
}

export function generateReport(projectId: number) {
  return request<AnalysisReport>({
    url: `/api/projects/${projectId}/reports/generate`,
    method: 'post'
  })
}

export function startAnalyze(projectId: number) {
  return request<AnalysisTask>({
    url: `/api/projects/${projectId}/analyze`,
    method: 'post'
  })
}

export function getTask(taskId: number) {
  return request<AnalysisTask>({
    url: `/api/tasks/${taskId}`,
    method: 'get'
  })
}

export function listReports(projectId: number) {
  return request<AnalysisReport[]>({
    url: `/api/projects/${projectId}/reports`,
    method: 'get'
  })
}

export function listMyReports(params: ReportListParams = {}) {
  return request<PageResult<ReportListItem>>({
    url: '/api/reports',
    method: 'get',
    params
  })
}

export function getReportDetail(reportId: number) {
  return request<AnalysisReport>({
    url: `/api/reports/${reportId}`,
    method: 'get'
  })
}

export function enhanceClaimEvidence(reportId: number) {
  return request<AnalysisReport>({
    url: `/api/reports/${reportId}/claim-evidence/ai-enhance`,
    method: 'post'
  })
}
