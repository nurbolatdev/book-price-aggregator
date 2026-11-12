import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest'
import { server } from '../mocks/server'
import { useAuthStore } from '../stores/auth'
import { resetViewport } from './viewport'

resetViewport()

// jsdom has no layout: report a fixed 800x240 box so chart containers get a size.
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class {
    private readonly callback: ResizeObserverCallback
    constructor(callback: ResizeObserverCallback) {
      this.callback = callback
    }
    observe(target: Element) {
      const rect = { width: 800, height: 240, top: 0, left: 0, right: 800, bottom: 240, x: 0, y: 0 }
      const entry = {
        target,
        contentRect: { ...rect, toJSON: () => rect },
        borderBoxSize: [{ inlineSize: 800, blockSize: 240 }],
        contentBoxSize: [{ inlineSize: 800, blockSize: 240 }],
        devicePixelContentBoxSize: [{ inlineSize: 800, blockSize: 240 }],
      } as unknown as ResizeObserverEntry
      this.callback([entry], this as unknown as ResizeObserver)
    }
    unobserve() {}
    disconnect() {}
  }
}

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

beforeEach(() => {
  useAuthStore.setState({ status: 'anonymous', user: null, accessToken: null })
})

afterEach(() => {
  cleanup()
  resetViewport()
  server.resetHandlers()
  localStorage.clear()
})

afterAll(() => server.close())
