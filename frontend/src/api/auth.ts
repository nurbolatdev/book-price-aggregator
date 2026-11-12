import { api, refreshSession } from './client'
import { publishAuth } from './authSync'
import { removeLegacyRefreshToken, useAuthStore } from '../stores/auth'
import type { Session } from '../stores/auth'
import type { AuthResponse, RegisterRequest, RegisterResponse, UserResponse } from './types'

export function toSession(data: AuthResponse): Session {
  const { accessToken, user } = data
  if (!accessToken || !user) throw new Error('Incomplete auth response')
  return { accessToken, user }
}

export async function login(email: string, password: string): Promise<UserResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password })
  const session = toSession(data)
  useAuthStore.getState().setSession(session)
  publishAuth({ type: 'session', ...session })
  return session.user
}

export async function logout(): Promise<void> {
  try {
    await api.post('/auth/logout')
  } catch {
    // the local session is dropped regardless of the server answer
  } finally {
    useAuthStore.getState().clearSession()
    publishAuth({ type: 'logout' })
  }
}

export async function logoutAll(): Promise<void> {
  await api.post('/auth/logout-all')
  useAuthStore.getState().clearSession()
  publishAuth({ type: 'logout' })
}

export async function register(body: RegisterRequest): Promise<RegisterResponse> {
  const { data } = await api.post<RegisterResponse>('/auth/register', body)
  return data
}

export async function resendVerification(email: string): Promise<void> {
  await api.post('/auth/resend-verification', { email })
}

export async function verifyEmail(token: string): Promise<void> {
  await api.post('/auth/verify-email', { token })
}

export async function forgotPassword(email: string): Promise<void> {
  await api.post('/auth/forgot-password', { email })
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await api.post('/auth/reset-password', { token, newPassword })
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
