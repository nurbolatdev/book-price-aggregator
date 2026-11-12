import { api, refreshSession } from './client'
import { removeLegacyRefreshToken, useAuthStore } from '../stores/auth'
import type { Session } from '../stores/auth'
import type { AuthResponse, UserResponse } from './types'

export function toSession(data: AuthResponse): Session {
  const { accessToken, user } = data
  if (!accessToken || !user) throw new Error('Incomplete auth response')
  return { accessToken, user }
}

export async function login(email: string, password: string): Promise<UserResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
  const session = toSession(data)
  useAuthStore.getState().setSession(session)
  return session.user
}

export async function logout(): Promise<void> {
  try {
    await api.post('/auth/logout')
  } catch {
    // the local session is dropped regardless of the server answer
  } finally {
    useAuthStore.getState().clearSession()
  }
}

export async function restoreSession(): Promise<void> {
  removeLegacyRefreshToken()
  try {
    await refreshSession()
  } catch {
    if (useAuthStore.getState().status === 'restoring') {
      useAuthStore.setState({ status: 'anonymous' })
    }
  }
}
