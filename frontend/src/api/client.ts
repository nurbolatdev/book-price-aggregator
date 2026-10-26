import axios from 'axios'
import { getRefreshToken, useAuthStore } from '../stores/auth'
import { ApiError, ERROR_MESSAGES, toApiError } from './errors'
import type { AuthResponse } from './types'

declare module 'axios' {
  interface InternalAxiosRequestConfig {
    _retried?: boolean
  }
}

export const api = axios.create({ baseURL: '/api/v1' })

function isAuthUrl(url: string | undefined): boolean {
  return url?.startsWith('/auth/') ?? false
}

api.interceptors.request.use((config) => {
  if (isAuthUrl(config.url)) return config
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`)
  } else {
    config.headers.delete('Authorization')
  }
  return config
})

let inflight: Promise<void> | null = null

async function doRefresh(): Promise<void> {
  const { setSession, clearSession } = useAuthStore.getState()
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    clearSession()
    throw new ApiError(401, 'UNAUTHORIZED', ERROR_MESSAGES.UNAUTHORIZED)
  }
  try {
    const { data } = await api.post<AuthResponse>('/auth/refresh', { refreshToken })
    setSession(data)
  } catch (e) {
    const error = toApiError(e)
    if (error.status >= 400 && error.status < 500) clearSession()
    throw error
  }
}

export function refreshSession(): Promise<void> {
  inflight ??= doRefresh().finally(() => {
    inflight = null
  })
  return inflight
}

api.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const apiError = toApiError(error)
    const config = axios.isAxiosError(error) ? error.config : undefined
    if (
      config &&
      !config._retried &&
      !isAuthUrl(config.url) &&
      apiError.status === 401 &&
      apiError.code === 'TOKEN_EXPIRED'
    ) {
      config._retried = true
      const current = useAuthStore.getState().accessToken
      const sentWith = config.headers.get('Authorization')
      if (!current || sentWith === `Bearer ${current}`) {
        await refreshSession()
      }
      return api(config)
    }
    throw apiError
  },
)
