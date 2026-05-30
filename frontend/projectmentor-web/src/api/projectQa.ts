import { request } from './request'
import type { ProjectQaResponse } from '@/types/api'

export function askProjectQa(projectId: number, question: string) {
  return request<ProjectQaResponse>({
    url: `/api/projects/${projectId}/qa`,
    method: 'post',
    data: { question }
  })
}
