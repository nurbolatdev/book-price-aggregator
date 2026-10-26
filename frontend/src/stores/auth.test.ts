import { beforeEach, describe, expect, it, vi } from 'vitest'
import { REFRESH_TOKEN_KEY, useAuthStore } from './auth'

const auth = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  expiresIn: 900,
  user: { id: 1, name: 'Aidar', email: 'a@b.kz', role: 'USER' as const },
}

describe('auth store', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('keeps access token in memory and only refresh token in localStorage', () => {
    useAuthStore.getState().setSession(auth)
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.accessToken).toBe('access-1')
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBe('refresh-1')
    const stored = JSON.stringify({ ...localStorage })
    expect(stored).not.toContain('access-1')
  })

  it('clearSession removes tokens and sets anonymous', () => {
    useAuthStore.getState().setSession(auth)
    useAuthStore.getState().clearSession()
    const state = useAuthStore.getState()
    expect(state.status).toBe('anonymous')
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
    expect(localStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull()
  })

  it('setSession throws when accessToken is missing', () => {
    expect(() => useAuthStore.getState().setSession({ ...auth, accessToken: undefined })).toThrow()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('initial status is restoring when a refresh token is stored', async () => {
    localStorage.setItem(REFRESH_TOKEN_KEY, 'stored')
    vi.resetModules()
    const mod = await import('./auth')
    expect(mod.useAuthStore.getState().status).toBe('restoring')
  })

  it('initial status is anonymous without a stored token', async () => {
    vi.resetModules()
    const mod = await import('./auth')
    expect(mod.useAuthStore.getState().status).toBe('anonymous')
  })
})
