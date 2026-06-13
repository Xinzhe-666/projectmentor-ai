import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import router from '@/router'
import { i18n } from '@/locales'
import { useUserStore } from '@/stores/user'
import type { ApiResult } from '@/types/api'

const TOKEN_KEY = 'projectmentor_token'
const configuredBaseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export interface RequestConfig extends AxiosRequestConfig {
  silentError?: boolean
}

const service = axios.create({
  baseURL: configuredBaseURL === '/api' ? '' : configuredBaseURL,
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

      const localizedMessage = result.code === 60001
        ? String(i18n.global.t('credits.insufficient'))
        : String(result.message || '').includes('注册过于频繁')
          ? String(i18n.global.t('auth.registrationTooFrequent'))
        : String(result.message || '').includes('额度已返还')
          ? String(i18n.global.t('credits.aiFailedRefunded'))
          : result.message || String(i18n.global.t('common.requestFailed'))

      if (!(response.config as RequestConfig).silentError) {
        ElMessage.error(localizedMessage)
      }
      return Promise.reject(new Error(localizedMessage))
    }

    return response.data
  },
  (error) => {
    const requestUrl = String(error?.config?.url || '')
    const isTimeout = error?.code === 'ECONNABORTED' || String(error?.message || '').toLowerCase().includes('timeout')
    const isZipUploadTimeout = isTimeout && requestUrl.includes('/upload-zip')
    const message = isZipUploadTimeout
      ? '大项目上传超时，请检查网络，或删除 node_modules / target / dist / .git 后重新上传。'
      : error?.response?.data?.message || error.message || '网络异常，请稍后重试'
    if (!(error?.config as RequestConfig | undefined)?.silentError) {
      ElMessage.error(message)
    }
    return Promise.reject(isZipUploadTimeout ? new Error(message) : error)
  }
)

export function request<T>(config: RequestConfig): Promise<T> {
  return service.request<unknown, T>(config)
}

export default service
