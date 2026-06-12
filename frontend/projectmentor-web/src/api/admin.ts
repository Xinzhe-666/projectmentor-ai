import { request } from './request'
import type {
  AdminAiUsageOverview,
  AdminCreditAdjustmentPayload,
  AdminCreditAdjustmentResult,
  AdminCreditLogPage,
  AdminCreditLogParams,
  AdminCreditUserListParams,
  AdminCreditUserPage,
  AdminMe,
  AdminCreditUserDetail,
  AdminFeedbackDetail,
  AdminFeedbackListParams,
  AdminFeedbackPage,
  AdminFeedbackStatusPayload,
  AdminGrantCreditPayload,
  AdminGrantCreditResult,
  AdminRecentProject,
  AdminRecentQa,
  AdminRecentReport,
  AdminRecentUser,
  AdminStats
} from '@/types/api'

export function getAdminMe() {
  return request<AdminMe>({
    url: '/api/admin/me',
    method: 'get'
  })
}

export function getAdminStats() {
  return request<AdminStats>({
    url: '/api/admin/stats',
    method: 'get'
  })
}

export function getAdminRecentUsers(limit = 10) {
  return request<AdminRecentUser[]>({
    url: '/api/admin/recent/users',
    method: 'get',
    params: { limit }
  })
}

export function getAdminRecentProjects(limit = 10) {
  return request<AdminRecentProject[]>({
    url: '/api/admin/recent/projects',
    method: 'get',
    params: { limit }
  })
}

export function getAdminRecentReports(limit = 10) {
  return request<AdminRecentReport[]>({
    url: '/api/admin/recent/reports',
    method: 'get',
    params: { limit }
  })
}

export function getAdminRecentQa(limit = 10) {
  return request<AdminRecentQa[]>({
    url: '/api/admin/recent/qa',
    method: 'get',
    params: { limit }
  })
}

export function getAdminCreditUsers(params: AdminCreditUserListParams = {}) {
  return request<AdminCreditUserPage>({
    url: '/api/admin/credits/users',
    method: 'get',
    params
  })
}

export function getAdminCreditUserDetail(userId: number) {
  return request<AdminCreditUserDetail>({
    url: `/api/admin/credits/users/${userId}`,
    method: 'get'
  })
}

export function getAdminCreditLogs(userId: number, params: AdminCreditLogParams = {}) {
  return request<AdminCreditLogPage>({
    url: `/api/admin/credits/users/${userId}/logs`,
    method: 'get',
    params
  })
}

export function grantUserCredits(userId: number, payload: AdminCreditAdjustmentPayload) {
  return request<AdminCreditAdjustmentResult>({
    url: `/api/admin/credits/users/${userId}/grant`,
    method: 'post',
    data: payload
  })
}

export function deductUserCredits(userId: number, payload: AdminCreditAdjustmentPayload) {
  return request<AdminCreditAdjustmentResult>({
    url: `/api/admin/credits/users/${userId}/deduct`,
    method: 'post',
    data: payload
  })
}

export function getAiUsageOverview() {
  return request<AdminAiUsageOverview>({
    url: '/api/admin/ai-usage/overview',
    method: 'get'
  })
}

export function searchAdminCreditUsers(keyword = '', limit = 10) {
  return getAdminCreditUsers({ keyword, size: limit })
}

export function grantAdminCredit(payload: AdminGrantCreditPayload) {
  return request<AdminGrantCreditResult>({
    url: '/api/admin/credits/grant',
    method: 'post',
    data: payload
  })
}

export function getAdminFeedbackList(params: AdminFeedbackListParams = {}) {
  return request<AdminFeedbackPage>({
    url: '/api/admin/feedback',
    method: 'get',
    params
  })
}

export function getAdminFeedbackDetail(id: number) {
  return request<AdminFeedbackDetail>({
    url: `/api/admin/feedback/${id}`,
    method: 'get'
  })
}

export function updateAdminFeedbackStatus(id: number, payload: AdminFeedbackStatusPayload) {
  return request<AdminFeedbackDetail>({
    url: `/api/admin/feedback/${id}/status`,
    method: 'put',
    data: payload
  })
}
