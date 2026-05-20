import { request } from './request'
import type { InterviewSession } from '@/types/api'

export interface StartInterviewParams {
  projectId: number
  mode?: string
}

export function startInterview(data: StartInterviewParams) {
  return request<InterviewSession>({
    url: '/api/interview/start',
    method: 'post',
    data
  })
}

export function submitAnswer(sessionId: number, answer: string) {
  return request<InterviewSession>({
    url: `/api/interview/${sessionId}/answer`,
    method: 'post',
    data: { answer }
  })
}

export function getInterviewSession(sessionId: number) {
  return request<InterviewSession>({
    url: `/api/interview/${sessionId}`,
    method: 'get'
  })
}

export function finishInterview(sessionId: number) {
  return request<InterviewSession>({
    url: `/api/interview/${sessionId}/finish`,
    method: 'post'
  })
}
