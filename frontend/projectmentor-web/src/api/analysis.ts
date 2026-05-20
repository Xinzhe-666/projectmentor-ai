import { request } from './request'
import type { AnalysisReport, AnalysisTask, RuleScanResult } from '@/types/api'

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

export function getReportDetail(reportId: number) {
  return request<AnalysisReport>({
    url: `/api/reports/${reportId}`,
    method: 'get'
  })
}
