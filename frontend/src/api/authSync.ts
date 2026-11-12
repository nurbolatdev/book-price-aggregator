import { useAuthStore } from '../stores/auth'
import type { UserResponse } from './types'

const LOCK_NAME = 'kr-refresh'
const CHANNEL_NAME = 'kr-auth'

export type AuthMessage =
  | { type: 'session'; accessToken: string; user: UserResponse }
  | { type: 'logout' }

let channel: BroadcastChannel | null = null

export function withRefreshLock<T>(fn: () => Promise<T>): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request(LOCK_NAME, fn)
  }
  return fn()
}

export function applyAuthMessage(message: AuthMessage): void {
  const store = useAuthStore.getState()
  if (message.type === 'session') {
    if (message.accessToken && message.user) {
      store.setSession({ accessToken: message.accessToken, user: message.user })
    }
  } else if (message.type === 'logout') {
    store.clearSession()
  }
}

export function startAuthSync(): void {
  if (channel || typeof BroadcastChannel === 'undefined') return
  const created = new BroadcastChannel(CHANNEL_NAME)
  created.onmessage = (event: MessageEvent<AuthMessage>) => {
    const data = event.data
    if (data && (data.type === 'session' || data.type === 'logout')) applyAuthMessage(data)
  }
  channel = created
}

export function stopAuthSync(): void {
  channel?.close()
  channel = null
}

export function publishAuth(message: AuthMessage): void {
  channel?.postMessage(message)
}
