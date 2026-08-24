import { request } from './request'
import type { FeedbackSubmitPayload, FeedbackSubmitResult } from '@/v46/types/api'

export function submitFeedback(payload: FeedbackSubmitPayload) {
  return request<FeedbackSubmitResult>({
    url: '/api/feedback',
    method: 'post',
    data: payload
  })
}
