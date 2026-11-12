import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { authBody, mockApi } from '../test/mockAdapter'
import { useAuthStore } from '../stores/auth'
import { login, logout } from './auth'
import { api } from './client'
import { applyAuthMessage, startAuthSync, stopAuthSync, withRefreshLock } from './authSync'

const bus: FakeChannel[] = []

class FakeChannel {
  static posted: unknown[] = []
  onmessage: ((event: MessageEvent) => void) | null = null
  constructor(readonly name: string) {
    bus.push(this)
  }
  postMessage(data: unknown) {
    FakeChannel.posted.push(data)
    for (const other of bus) {
      if (other !== this && other.name === this.name) {
        queueMicrotask(() => other.onmessage?.({ data } as MessageEvent))
      }
    }
  }
  close() {
    const i = bus.indexOf(this)
    if (i >= 0) bus.splice(i, 1)
  }
}

const user = { id: 1, email: 'reader@example.com', name: 'Reader', role: 'USER' as const }

beforeEach(() => {
  FakeChannel.posted = []
  vi.stubGlobal('BroadcastChannel', FakeChannel)
})

afterEach(() => {
  stopAuthSync()
  bus.length = 0
  vi.unstubAllGlobals()
})

const flush = () => new Promise((r) => setTimeout(r, 0))

describe('withRefreshLock', () => {
  it('uses navigator.locks when present', async () => {
    const request = vi.fn((_name: string, fn: () => Promise<string>) => fn())
    vi.stubGlobal('navigator', { locks: { request } })
    await expect(withRefreshLock(async () => 'done')).resolves.toBe('done')
    expect(request).toHaveBeenCalledWith('kr-refresh', expect.any(Function))
  })

  it('runs the callback directly without navigator.locks', async () => {
    vi.stubGlobal('navigator', {})
    await expect(withRefreshLock(async () => 'direct')).resolves.toBe('direct')
  })
})

describe('channel', () => {
  it('adopts a session from another tab', async () => {
    startAuthSync()
    new FakeChannel('kr-auth').postMessage({ type: 'session', accessToken: 'tab2', user })
    await flush()
    const state = useAuthStore.getState()
    expect(state.status).toBe('authenticated')
    expect(state.accessToken).toBe('tab2')
    expect(state.user).toEqual(user)
  })

  it('clears the session on a logout message', async () => {
    startAuthSync()
    useAuthStore.getState().setSession({ accessToken: 'a', user })
    new FakeChannel('kr-auth').postMessage({ type: 'logout' })
    await flush()
    expect(useAuthStore.getState().status).toBe('anonymous')
  })

  it('login publishes a session and logout publishes a logout', async () => {
    startAuthSync()
    mockApi((config) => (config.url === '/auth/login' ? [200, authBody('acc')] : [204]))
    await login('reader@example.com', 'x')
    await logout()
    expect(FakeChannel.posted).toEqual([
      expect.objectContaining({ type: 'session', accessToken: 'acc' }),
      { type: 'logout' },
    ])
  })

  it('does not react to its own post', async () => {
    startAuthSync()
    mockApi(() => [200, authBody('acc')])
    await login('reader@example.com', 'x')
    useAuthStore.getState().setSession({ accessToken: 'later', user })
    await flush()
    expect(useAuthStore.getState().accessToken).toBe('later')
  })

  it('applyAuthMessage ignores an incomplete session', () => {
    applyAuthMessage({ type: 'session', accessToken: '', user })
    expect(useAuthStore.getState().status).toBe('anonymous')
  })
})

describe('second tab refreshed first', () => {
  it('retries with the token from the other tab and sends no refresh', async () => {
    useAuthStore.getState().setSession({ accessToken: 'old', user })
    vi.stubGlobal('navigator', {
      locks: {
        request: async (_name: string, fn: () => Promise<unknown>) => {
          applyAuthMessage({ type: 'session', accessToken: 'from-other-tab', user })
          return fn()
        },
      },
    })
    let refreshCalls = 0
    const seen: string[] = []
    mockApi((config) => {
      if (config.url === '/auth/refresh') {
        refreshCalls += 1
        return [200, authBody('never')]
      }
      const header = String(config.headers.get('Authorization') ?? '')
      seen.push(header)
      return header === 'Bearer old'
        ? [401, { code: 'TOKEN_EXPIRED', message: 'expired' }]
        : [200, { ok: true }]
    })
    const res = await api.get('/me')
    expect(res.status).toBe(200)
    expect(seen).toEqual(['Bearer old', 'Bearer from-other-tab'])
    expect(refreshCalls).toBe(0)
  })
})
