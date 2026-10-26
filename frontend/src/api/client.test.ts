import { beforeEach, describe, expect, it } from 'vitest'
import { NETWORK_FAILURE, authBody, mockApi } from '../test/mockAdapter'
import type { MockReply } from '../test/mockAdapter'
import { useAuthStore } from '../stores/auth'
import { api } from './client'
import { ApiError } from './errors'

function authorizationOf(headers: { get(name: string): unknown }): string {
  return String(headers.get('Authorization') ?? '')
}

function startSession(access = 'old', refresh = 'r1') {
  useAuthStore.getState().setSession(authBody(access, refresh))
}

beforeEach(() => {
  useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
})

describe('api client', () => {
  it('adds the bearer token to protected requests but not to /auth/*', async () => {
    startSession('tok')
    const seen: Record<string, string> = {}
    mockApi((config) => {
      seen[config.url ?? ''] = authorizationOf(config.headers)
      return [200, {}]
    })
    await api.get('/me')
    await api.post('/auth/login', {})
    expect(seen['/me']).toBe('Bearer tok')
    expect(seen['/auth/login']).toBe('')
  })

  it('refreshes once for parallel expired requests and retries them all', async () => {
    startSession()
    let refreshCalls = 0
    mockApi(async (config) => {
      if (config.url === '/auth/refresh') {
        refreshCalls += 1
        await Promise.resolve()
        return [200, authBody('new', 'r2')]
      }
      return authorizationOf(config.headers) === 'Bearer new'
        ? [200, { ok: true }]
        : [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }]
    })

    const results = await Promise.all([api.get('/a'), api.get('/b'), api.get('/c')])

    expect(refreshCalls).toBe(1)
    expect(results.map((r) => r.status)).toEqual([200, 200, 200])
    expect(localStorage.getItem('kr.refreshToken')).toBe('r2')
    expect(useAuthStore.getState().accessToken).toBe('new')
  })

  it('rejects all waiting requests and clears the session when refresh is refused', async () => {
    startSession()
    const hrefBefore = window.location.href
    mockApi((config) =>
      config.url === '/auth/refresh'
        ? [401, { status: 401, code: 'UNAUTHORIZED', message: 'revoked' }]
        : [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }],
    )

    const results = await Promise.allSettled([api.get('/a'), api.get('/b'), api.get('/c')])

    for (const result of results) {
      expect(result.status).toBe('rejected')
      const reason = (result as PromiseRejectedResult).reason
      expect(reason).toBeInstanceOf(ApiError)
      expect((reason as ApiError).status).toBe(401)
    }
    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(localStorage.getItem('kr.refreshToken')).toBeNull()
    expect(window.location.href).toBe(hrefBefore)
  })

  it('does not refresh on a failed login', async () => {
    let refreshCalls = 0
    mockApi((config) => {
      if (config.url === '/auth/refresh') refreshCalls += 1
      return [401, { status: 401, code: 'INVALID_CREDENTIALS', message: 'bad' }]
    })
    await expect(api.post('/auth/login', {})).rejects.toMatchObject({
      code: 'INVALID_CREDENTIALS',
    })
    expect(refreshCalls).toBe(0)
  })

  it('does not refresh on 401 UNAUTHORIZED', async () => {
    startSession()
    let refreshCalls = 0
    mockApi((config) => {
      if (config.url === '/auth/refresh') refreshCalls += 1
      return [401, { status: 401, code: 'UNAUTHORIZED', message: 'no' }]
    })
    await expect(api.get('/me')).rejects.toMatchObject({ code: 'UNAUTHORIZED' })
    expect(refreshCalls).toBe(0)
  })

  it('retries an expired request only once', async () => {
    startSession()
    let meCalls = 0
    let refreshCalls = 0
    mockApi((config): MockReply => {
      if (config.url === '/auth/refresh') {
        refreshCalls += 1
        return [200, authBody('new', 'r2')]
      }
      meCalls += 1
      return [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }]
    })
    await expect(api.get('/me')).rejects.toMatchObject({ code: 'TOKEN_EXPIRED' })
    expect(meCalls).toBe(2)
    expect(refreshCalls).toBe(1)
  })

  it('keeps the refresh token when refresh fails with a network error', async () => {
    startSession()
    mockApi((config) =>
      config.url === '/auth/refresh'
        ? NETWORK_FAILURE
        : [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }],
    )
    await expect(api.get('/me')).rejects.toMatchObject({ code: 'NETWORK_ERROR' })
    expect(localStorage.getItem('kr.refreshToken')).toBe('r1')
  })

  it('keeps the refresh token on 429 and rejects with RATE_LIMITED', async () => {
    startSession()
    mockApi((config) =>
      config.url === '/auth/refresh'
        ? [429, { status: 429, code: 'RATE_LIMITED', message: 'slow down' }]
        : [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }],
    )
    await expect(api.get('/me')).rejects.toMatchObject({ code: 'RATE_LIMITED' })
    expect(localStorage.getItem('kr.refreshToken')).toBe('r1')
  })

  it('keeps the refresh token and status on 503', async () => {
    startSession()
    mockApi((config) =>
      config.url === '/auth/refresh'
        ? [503, { status: 503, code: 'SERVICE_UNAVAILABLE', message: 'down' }]
        : [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }],
    )
    await expect(api.get('/me')).rejects.toMatchObject({ status: 503 })
    expect(localStorage.getItem('kr.refreshToken')).toBe('r1')
    expect(useAuthStore.getState().status).toBe('authenticated')
  })

  it('drops the refresh result when the session was cleared meanwhile', async () => {
    startSession()
    mockApi(async (config) => {
      if (config.url === '/auth/refresh') {
        useAuthStore.getState().clearSession()
        return [200, authBody('new', 'r2')]
      }
      return [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }]
    })
    await expect(api.get('/me')).rejects.toBeInstanceOf(ApiError)
    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(localStorage.getItem('kr.refreshToken')).toBeNull()
  })

  it('clears the session when a 2xx refresh body is incomplete', async () => {
    startSession()
    mockApi((config) =>
      config.url === '/auth/refresh'
        ? [200, { accessToken: 'new' }]
        : [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }],
    )
    await expect(api.get('/me')).rejects.toBeInstanceOf(ApiError)
    expect(useAuthStore.getState().status).toBe('anonymous')
    expect(localStorage.getItem('kr.refreshToken')).toBeNull()
  })

  it('retries with the current token without refreshing when it changed meanwhile', async () => {
    startSession('old', 'r1')
    let refreshCalls = 0
    const seen: string[] = []
    mockApi((config) => {
      if (config.url === '/auth/refresh') {
        refreshCalls += 1
        return [200, authBody('x', 'rx')]
      }
      const auth = authorizationOf(config.headers)
      seen.push(auth)
      if (auth === 'Bearer old') {
        startSession('new', 'r2')
        return [401, { status: 401, code: 'TOKEN_EXPIRED', message: 'expired' }]
      }
      return [200, {}]
    })
    await api.get('/me')
    expect(refreshCalls).toBe(0)
    expect(seen).toEqual(['Bearer old', 'Bearer new'])
  })
})
