import { api, refreshSession } from './client'
import { getRefreshToken, useAuthStore } from '../stores/auth'
import type { AuthResponse, UserResponse } from './types'

export async function login(email: string, password: string): Promise<UserResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
  useAuthStore.getState().setSession(data)
  const { user } = useAuthStore.getState()
  if (!user) throw new Error('Incomplete auth response')
  return user
}

export async function logout(): Promise<void> {
  const refreshToken = getRefreshToken()
  try {
    if (refreshToken) {
      await api.post('/auth/logout', { refreshToken })
    }
  } catch {
    // the local session is dropped regardless of the server answer
  } finally {
    useAuthStore.getState().clearSession()
  }
}

export async function restoreSession(): Promise<void> {
  if (!getRefreshToken()) {
    useAuthStore.setState({ status: 'anonymous' })
    return
  }
  try {
    await refreshSession()
  } catch {
    useAuthStore.setState({ status: 'anonymous' })
  }
}
