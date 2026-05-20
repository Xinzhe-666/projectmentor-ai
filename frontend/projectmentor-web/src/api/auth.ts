import { request } from './request'
import type { LoginResponse, UserInfo } from '@/types/api'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  email: string
  password: string
}

export function register(data: RegisterParams) {
  return request<LoginResponse>({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}

export function login(data: LoginParams) {
  return request<LoginResponse>({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

export function getCurrentUser() {
  return request<UserInfo>({
    url: '/api/auth/me',
    method: 'get'
  })
}

export function logout() {
  return request<void>({
    url: '/api/auth/logout',
    method: 'post'
  })
}
