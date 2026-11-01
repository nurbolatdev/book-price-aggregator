import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from '../mocks/server'
import { resetViewport } from './viewport'

resetViewport()

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))

afterEach(() => {
  cleanup()
  resetViewport()
  server.resetHandlers()
  localStorage.clear()
})

afterAll(() => server.close())
