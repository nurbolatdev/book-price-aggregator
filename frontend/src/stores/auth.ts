import { create } from 'zustand'
import type { UserResponse } from '../api/types'

export const LEGACY_REFRESH_TOKEN_KEY = 'kr.refreshToken'

export type AuthStatus = 'restoring' | 'authenticated' | 'anonymous'

export type Session = { accessToken: string; user: UserResponse }

interface AuthState {
  status: AuthStatus
  user: UserResponse | null
  accessToken: string | null
  setSession: (session: Session) => void
  setUser: (user: UserResponse) => void
  clearSession: () => void
}

let sessionEpoch = 0

export function getSessionEpoch(): number {
  return sessionEpoch
}

export function removeLegacyRefreshToken(): void {
  try {
    localStorage.removeItem(LEGACY_REFRESH_TOKEN_KEY)
  } catch {
    // storage may be unavailable; nothing to clean up then
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'restoring',
  user: null,
  accessToken: null,
  setSession: ({ accessToken, user }) => {
    if (!accessToken || !user) {
      throw new Error('Incomplete auth response')
    }
    set({ status: 'authenticated', user, accessToken })
  },
  setUser: (user) => set({ user }),
  clearSession: () => {
    sessionEpoch += 1
    set({ status: 'anonymous', user: null, accessToken: null })
  },
}))
