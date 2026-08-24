import { request } from './request'
import type { CreditInfo, CreditLog } from '@/v46/types/api'

export function getMyCredits() {
  return request<CreditInfo>({
    url: '/api/credits/me',
    method: 'get'
  })
}

export function listCreditLogs() {
  return request<CreditLog[]>({
    url: '/api/credits/logs',
    method: 'get'
  })
}
