import axios from 'axios'
import { getSessionEpoch, useAuthStore } from '../stores/auth'
import { toast } from '../stores/toast'
import { ApiError, ERROR_MESSAGES, toApiError } from './errors'
import { toSession } from './auth'
import { publishAuth, withRefreshLock } from './authSync'
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

function isCookieOnlyAuthUrl(url: string | undefined): boolean {
  return isAuthUrl(url) && url !== '/auth/logout-all'
}

api.interceptors.request.use((config) => {
  if (isAuthUrl(config.url)) config.withCredentials = true
  if (isCookieOnlyAuthUrl(config.url)) return config
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
  const before = useAuthStore.getState().accessToken
  const wasAuthenticated = useAuthStore.getState().status === 'authenticated'
  const epoch = getSessionEpoch()
  await withRefreshLock(async () => {
    const current = useAuthStore.getState().accessToken
    if (current && current !== before) return
    let data: AuthResponse
    try {
      data = (await api.post<AuthResponse>('/auth/refresh')).data
    } catch (e) {
      const error = toApiError(e)
      if (error.status === 401 && getSessionEpoch() === epoch) {
        clearSession()
        if (wasAuthenticated) toast.info('Сессия закончилась, войдите снова')
      }
      throw error
    }
    if (getSessionEpoch() !== epoch) {
      throw new ApiError(401, 'UNAUTHORIZED', ERROR_MESSAGES.UNAUTHORIZED)
    }
    try {
      const session = toSession(data)
      setSession(session)
      publishAuth({ type: 'session', ...session })
    } catch {
      clearSession()
      throw new ApiError(500, 'INTERNAL_ERROR', ERROR_MESSAGES.INTERNAL_ERROR)
    }
  })
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
      !isCookieOnlyAuthUrl(config.url) &&
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
