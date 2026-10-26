import { beforeEach, describe, expect, it } from 'vitest'
import { NETWORK_FAILURE, authBody, mockApi } from '../test/mockAdapter'
import { useAuthStore } from '../stores/auth'
import { login, logout, restoreSession } from './auth'

beforeEach(() => {
  useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
})

describe('auth actions', () => {
  it('login stores the token pair and the user', async () => {
    mockApi(() => [200, authBody('acc', 'ref')])
    const user = await login('reader@example.com', 'secret')
    expect(user.email).toBe('reader@example.com')
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.accessToken).toBe('acc')
    expect(localStorage.getItem('kr.refreshToken')).toBe('ref')
  })

  it('logout sends the stored refresh token and clears the session', async () => {
    useAuthStore.getState().setSession(authBody('acc', 'ref'))
    const bodies: unknown[] = []
    mockApi((config) => {
      bodies.push(JSON.parse(String(config.data)))
      return [204]
    })
    await logout()
    expect(bodies).toEqual([{ refreshToken: 'ref' }])
    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(localStorage.getItem('kr.refreshToken')).toBeNull()
  })

  it('logout clears the session even when the call fails', async () => {
    useAuthStore.getState().setSession(authBody('acc', 'ref'))
    mockApi(() => NETWORK_FAILURE)
    await logout()
    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(localStorage.getItem('kr.refreshToken')).toBeNull()
  })

  it('restoreSession without a token goes anonymous without a request', async () => {
    let calls = 0
    mockApi(() => {
      calls += 1
      return [200, {}]
    })
    useAuthStore.setState({ status: 'restoring' })
    await restoreSession()
    expect(calls).toBe(0)
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('restoreSession with a valid token authenticates', async () => {
    localStorage.setItem('kr.refreshToken', 'ref')
    useAuthStore.setState({ status: 'restoring' })
    mockApi(() => [200, authBody('acc2', 'ref2')])
    await restoreSession()
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.user?.name).toBe('Reader')
    expect(localStorage.getItem('kr.refreshToken')).toBe('ref2')
  })
})
