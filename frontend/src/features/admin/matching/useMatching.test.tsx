import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { queryKeys } from '../../../api/queryKeys'
import type { ReviewOfferPage } from '../../../api/types'
import { server } from '../../../mocks/server'
import { useToastStore } from '../../../stores/toast'
import { useMatching } from './useMatching'

let client: QueryClient

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

const toasts = () => useToastStore.getState().toasts.map((t) => t.message)

beforeEach(() => {
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  useToastStore.setState({ toasts: [] })
})
afterEach(() => {
  server.events.removeAllListeners()
})

async function ready() {
  const hook = renderHook(() => useMatching(0), { wrapper })
  await waitFor(() => expect(hook.result.current.selected?.id).toBe(501))
  return hook
}

describe('useMatching', () => {
  it('selects the first offer by default', async () => {
    const { result } = await ready()
    expect(result.current.query.data?.totalElements).toBe(23)
  })

  it('matches the selected offer and moves to the next one', async () => {
    const bodies: unknown[] = []
    server.events.on('request:start', async ({ request }) => {
      if (new URL(request.url).pathname.endsWith('/admin/offers/501/match')) {
        bodies.push(await request.clone().json())
      }
    })
    const { result } = await ready()
    await act(() => result.current.match(16))
    expect(bodies).toEqual([{ editionId: 16 }])
    expect(result.current.selected?.id).toBe(502)
    const cached = client.getQueryData<ReviewOfferPage>(queryKeys.admin.pendingOffers(0))
    expect(cached?.totalElements).toBe(22)
    expect(toasts()).toEqual(['Предложение привязано к изданию'])
  })

  it('rejects the selected offer and moves to the next one', async () => {
    const { result } = await ready()
    act(() => result.current.select(504))
    await waitFor(() => expect(result.current.selected?.id).toBe(504))
    await act(() => result.current.reject())
    expect(result.current.selected?.id).toBe(505)
    expect(toasts()).toEqual(['Предложение отклонено'])
  })

  it('reports an already handled offer and refetches the list', async () => {
    const { result } = await ready()
    server.use(
      http.post('*/api/v1/admin/offers/501/reject', () =>
        HttpResponse.json({ code: 'NOT_FOUND', message: 'gone' }, { status: 404 }),
      ),
    )
    let listCalls = 0
    server.events.on('request:start', ({ request }) => {
      if (new URL(request.url).pathname.endsWith('/admin/offers/pending-review')) listCalls++
    })
    await act(() => result.current.reject())
    expect(toasts()).toEqual(['Предложение уже обработано'])
    await waitFor(() => expect(listCalls).toBe(1))
  })
})
