import { request } from './request'
import type {
  DefenseAnswerResponse,
  DefenseQuestionResponse,
  DefenseSessionResponse,
  DefenseSessionReviewResponse
} from '@/types/api'

export interface CreateDefenseSessionPayload {
  reportId: number
  mode?: 'EVIDENCE_DEFENSE'
}

export function createDefenseSession(projectId: number, data: CreateDefenseSessionPayload) {
  return request<DefenseSessionResponse>({
    url: `/api/projects/${projectId}/defense/sessions`,
    method: 'post',
    data
  })
}

export function getDefenseQuestions(sessionId: number, silentError = false) {
  return request<DefenseQuestionResponse[]>({
    url: `/api/defense/sessions/${sessionId}/questions`,
    method: 'get',
    silentError
  })
}

export function submitDefenseAnswer(questionId: number, answerText: string) {
  return request<DefenseAnswerResponse>({
    url: `/api/defense/questions/${questionId}/answer`,
    method: 'post',
    data: { answerText }
  })
}

export function getDefenseSessionReview(sessionId: number, silentError = false) {
  return request<DefenseSessionReviewResponse>({
    url: `/api/defense/sessions/${sessionId}/review`,
    method: 'get',
    silentError
  })
}
