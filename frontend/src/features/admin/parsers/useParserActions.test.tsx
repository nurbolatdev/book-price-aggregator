import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mockAdmin } from '../../../mocks/adminFixtures'
import { server } from '../../../mocks/server'
import { useToastStore } from '../../../stores/toast'
import { useParserActions } from './useParserActions'

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

const toasts = () => useToastStore.getState().toasts
const serviceDown = (code: string) =>
  http.post(`*/api/v1/admin/parsers/${code}/run`, () =>
    HttpResponse.json({ code: 'SERVICE_UNAVAILABLE', message: 'down' }, { status: 503 }),
  )

function logRuns(): string[] {
  const codes: string[] = []
  server.events.on('request:start', ({ request }) => {
    const match = new URL(request.url).pathname.match(/parsers\/([A-Z]+)\/run$/)
    if (match?.[1]) codes.push(match[1])
  })
  return codes
}

beforeEach(() => {
  useToastStore.setState({ toasts: [] })
})
afterEach(() => {
  server.events.removeAllListeners()
})

describe('useParserActions', () => {
  it('toasts after a queued run', async () => {
    const { result } = renderHook(() => useParserActions(), { wrapper })
    await act(() => result.current.run('FLIP'))
    expect(toasts().map((t) => t.message)).toEqual(['Задача в очереди'])
  })

  it('keeps a conflict in the row and shows no toast', async () => {
    const { result } = renderHook(() => useParserActions(), { wrapper })
    await act(() => result.current.run('KASPI'))
    expect(result.current.rowError.KASPI).toBe('Магазин выключен — сначала включите его')
    expect(toasts()).toHaveLength(0)
  })

  it('explains an unavailable queue in the row', async () => {
    server.use(serviceDown('OZON'))
    const { result } = renderHook(() => useParserActions(), { wrapper })
    await act(() => result.current.run('OZON'))
    expect(result.current.rowError.OZON).toBe('Очередь недоступна, попробуйте позже')
  })

  it('runs all enabled stores one by one', async () => {
    const codes = logRuns()
    const { result } = renderHook(() => useParserActions(), { wrapper })
    await act(() => result.current.runAll(mockAdmin.parsers))
    expect(codes).toEqual(['FLIP', 'MELOMAN', 'OZON'])
    expect(toasts().map((t) => [t.kind, t.message])).toEqual([['success', 'Поставлено в очередь: 3 из 3']])
  })

  it('reports partial success with a row message', async () => {
    server.use(serviceDown('OZON'))
    const { result } = renderHook(() => useParserActions(), { wrapper })
    await act(() => result.current.runAll(mockAdmin.parsers))
    expect(toasts().map((t) => [t.kind, t.message])).toEqual([['error', 'Поставлено в очередь: 2 из 3']])
    expect(result.current.rowError.OZON).toBe('Очередь недоступна, попробуйте позже')
  })
})
