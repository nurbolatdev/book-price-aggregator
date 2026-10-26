import { create } from 'zustand'
import type { AuthResponse, UserResponse } from '../api/types'

export const REFRESH_TOKEN_KEY = 'kr.refreshToken'

export type AuthStatus = 'restoring' | 'authenticated' | 'anonymous'

interface AuthState {
  status: AuthStatus
  user: UserResponse | null
  accessToken: string | null
  setSession: (auth: AuthResponse) => void
  clearSession: () => void
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export const useAuthStore = create<AuthState>((set) => ({
  status: getRefreshToken() ? 'restoring' : 'anonymous',
  user: null,
  accessToken: null,
  setSession: (auth) => {
    const { accessToken, refreshToken, user } = auth
    if (!accessToken || !refreshToken || !user) {
      throw new Error('Incomplete auth response')
    }
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    set({ status: 'authenticated', user, accessToken })
  },
  clearSession: () => {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    set({ status: 'anonymous', user: null, accessToken: null })
  },
}))
