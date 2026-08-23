import { request } from '@/api/request'
import type { AiStatus } from '@/types/api'

export function getAiStatus() {
  return request<AiStatus>({
    url: '/api/ai/status',
    method: 'GET',
    silentError: true
  })
}
