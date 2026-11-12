import { beforeEach, describe, expect, it } from 'vitest'
import { NETWORK_FAILURE, authBody, mockApi } from '../test/mockAdapter'
import { useAuthStore } from '../stores/auth'
import { login, logout, restoreSession } from './auth'

function startSession() {
  useAuthStore.getState().setSession({
    accessToken: 'acc',
    user: { id: 1, email: 'reader@example.com', name: 'Reader', role: 'USER' },
  })
}

beforeEach(() => {
  useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
})

describe('auth actions', () => {
  it('login stores the access token and the user and writes nothing to storage', async () => {
    mockApi(() => [200, authBody('acc')])
    const user = await login('reader@example.com', 'secret')
    expect(user.email).toBe('reader@example.com')
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.accessToken).toBe('acc')
    expect(localStorage.length).toBe(0)
  })

  it('logout posts with no body and clears the session', async () => {
    startSession()
    const bodies: unknown[] = []
    mockApi((config) => {
      bodies.push(config.data)
      return [204]
    })
    await logout()
    expect(bodies).toEqual([undefined])
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('logout clears the session even when the call fails', async () => {
    startSession()
    mockApi(() => NETWORK_FAILURE)
    await logout()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('restoreSession removes a stale refresh token, posts with no body and authenticates', async () => {
    localStorage.setItem('kr.refreshToken', 'stale')
    useAuthStore.setState({ status: 'restoring' })
    const seen: { url?: string; data: unknown }[] = []
    mockApi((config) => {
      seen.push({ url: config.url, data: config.data })
      return [200, authBody('acc2')]
    })
    await restoreSession()
    expect(localStorage.getItem('kr.refreshToken')).toBeNull()
    expect(seen).toEqual([{ url: '/auth/refresh', data: undefined }])
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.user?.name).toBe('Reader')
  })

  it('restoreSession goes anonymous on 401', async () => {
    useAuthStore.setState({ status: 'restoring' })
    mockApi(() => [401, { status: 401, code: 'UNAUTHORIZED', message: 'no' }])
    await restoreSession()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })
})
