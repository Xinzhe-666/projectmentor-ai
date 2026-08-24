import { request } from '@/v46/api/request'
import type { AiStatus } from '@/v46/types/api'

export function getAiStatus() {
  return request<AiStatus>({
    url: '/api/ai/status',
    method: 'GET'
  })
}
