import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { useUserStore } from '@/stores/user'
import type { ApiResult } from '@/types/api'

const TOKEN_KEY = 'projectmentor_token'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 60000
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>

    if (typeof result?.code === 'number') {
      if (result.code === 0) {
        return result.data
      }

      if (result.code === 40100) {
        try {
          useUserStore().logout()
        } catch {
          localStorage.removeItem('projectmentor_token')
          localStorage.removeItem('projectmentor_user')
          localStorage.removeItem('projectmentor_credits')
        }
        router.push('/login')
      }

      ElMessage.error(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }

    return response.data
  },
  (error) => {
    const message = error?.response?.data?.message || error.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export default service
