import { request } from './request'
import type { HallucinationCheckResult } from '@/v46/types/api'

export interface HallucinationCheckParams {
  projectId?: number
  aiAnswer: string
}

export function checkHallucination(data: HallucinationCheckParams) {
  return request<HallucinationCheckResult>({
    url: '/api/hallucination/check',
    method: 'post',
    data
  })
}
