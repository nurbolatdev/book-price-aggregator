import { publishAuth } from './authSync'
import { api } from './client'
import { useAuthStore } from '../stores/auth'
import type { UpdateProfileRequest, UserResponse } from './types'

export async function fetchMe(signal?: AbortSignal): Promise<UserResponse> {
  const { data } = await api.get<UserResponse>('/me', { signal })
  return data
}

export async function updateMe(patch: UpdateProfileRequest): Promise<UserResponse> {
  const { data } = await api.patch<UserResponse>('/me', patch)
  return data
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api.post('/me/password', { currentPassword, newPassword })
}

export async function deleteAccount(password: string): Promise<void> {
  await api.delete('/me', { data: { password } })
  useAuthStore.getState().clearSession()
  publishAuth({ type: 'logout' })
}
