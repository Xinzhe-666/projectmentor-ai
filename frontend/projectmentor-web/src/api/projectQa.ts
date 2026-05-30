import { request } from './request'
import type { ProjectQaHistoryRecord, ProjectQaResponse } from '@/types/api'

export function askProjectQa(projectId: number, question: string) {
  return request<ProjectQaResponse>({
    url: `/api/projects/${projectId}/qa`,
    method: 'post',
    data: { question }
  })
}

export const askProjectQuestion = askProjectQa

export function getProjectQaHistory(projectId: number) {
  return request<ProjectQaHistoryRecord[]>({
    url: `/api/projects/${projectId}/qa/history`,
    method: 'get'
  })
}

export function deleteProjectQaRecord(projectId: number, recordId: number) {
  return request<void>({
    url: `/api/projects/${projectId}/qa/history/${recordId}`,
    method: 'delete'
  })
}
