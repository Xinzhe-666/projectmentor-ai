import { request } from './request'
import type { InterviewSession, InterviewSessionListItem, PageResult } from '@/v46/types/api'

export interface StartInterviewParams {
  projectId: number
  mode?: string
}

export interface InterviewListParams {
  page?: number
  size?: number
  projectId?: number
  keyword?: string
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

export function listInterviewSessions(params: InterviewListParams = {}) {
  return request<PageResult<InterviewSessionListItem>>({
    url: '/api/interviews',
    method: 'get',
    params
  })
}
