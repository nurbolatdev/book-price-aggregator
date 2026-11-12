import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getSessionEpoch, useAuthStore } from './auth'

const session = {
  accessToken: 'access-1',
  user: { id: 1, name: 'Aidar', email: 'a@b.kz', role: 'USER' as const },
}

describe('auth store', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('keeps the access token in memory and writes nothing to storage', () => {
    useAuthStore.getState().setSession(session)
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.accessToken).toBe('access-1')
    expect(state.user?.name).toBe('Aidar')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('setUser replaces the user and keeps the session', () => {
    useAuthStore.getState().setSession(session)
    useAuthStore.getState().setUser({ ...session.user, name: 'Aidar B' })
    const state = useAuthStore.getState()
    expect(state.user?.name).toBe('Aidar B')
    expect(state.status).toBe('authenticated')
    expect(state.accessToken).toBe('access-1')
  })

  it('clearSession drops the session and increments the epoch', () => {
    useAuthStore.getState().setSession(session)
    const before = getSessionEpoch()
    useAuthStore.getState().clearSession()
    const state = useAuthStore.getState()
    expect(state.status).toBe('anonymous')
    expect(state.accessToken).toBeNull()
    expect(state.user).toBeNull()
    expect(getSessionEpoch()).toBe(before + 1)
  })

  it('setSession throws when the access token is missing', () => {
    expect(() =>
      useAuthStore.getState().setSession({ ...session, accessToken: '' }),
    ).toThrow()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('initial status is restoring', async () => {
    vi.resetModules()
    const mod = await import('./auth')
    expect(mod.useAuthStore.getState().status).toBe('restoring')
  })
})
