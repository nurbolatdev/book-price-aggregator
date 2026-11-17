import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { queryKeys } from '../../api/queryKeys'
import type { BeaconResponse } from '../../api/types'
import { mockBeacons, setMockBeacons } from '../../mocks/beaconFixtures'
import { MOCK_ACCESS_TOKEN } from '../../mocks/authFixtures'
import { editionDetail } from '../../mocks/editionFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { useToastStore } from '../../stores/toast'
import { bookFromBeacon, bookFromEdition } from './beaconBook'
import { useBeacons } from './useBeacons'
import { useCreateBeacon } from './useCreateBeacon'
import { useUpdateBeacon } from './useUpdateBeacon'

let location = '/'
function Probe() {
  const { pathname } = useLocation()
  location = pathname
  return null
}

let client: QueryClient
function wrapper({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <Probe />
        {children}
      </MemoryRouter>
    </QueryClientProvider>
  )
}

function signIn() {
  useAuthStore.setState({
    status: 'authenticated',
    accessToken: MOCK_ACCESS_TOKEN,
    user: { id: 1, email: 'nurbo@example.com' },
  })
}

function deferred() {
  let release: () => void = () => {}
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  return { gate, release }
}

const toasts = () => useToastStore.getState().toasts
const list = () => client.getQueryData<BeaconResponse[]>(queryKeys.beacons)
const forEdition = (id: number) =>
  client.getQueryData<BeaconResponse | null>(queryKeys.beaconForEdition(id))

function setup<T>(hook: () => T) {
  return renderHook(() => ({ beacons: useBeacons(), mutation: hook() }), { wrapper })
}

const serverError = () => HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status: 500 })

describe('beacon queries and optimistic mutations', () => {
  beforeEach(() => {
    client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } })
    useToastStore.setState({ toasts: [] })
    location = '/'
  })

  it('does not request anything for an anonymous user', async () => {
    let requests = 0
    server.use(
      http.get('*/api/v1/beacons', () => {
        requests += 1
        return HttpResponse.json([])
      }),
    )
    renderHook(() => useBeacons(), { wrapper })
    await new Promise((resolve) => setTimeout(resolve, 20))
    expect(requests).toBe(0)
  })

  it('loads the list for a signed-in user', async () => {
    signIn()
    const { result } = renderHook(() => useBeacons(), { wrapper })
    await waitFor(() => expect(result.current.data).toHaveLength(5))
  })

  describe('create', () => {
    const book = () => bookFromEdition(editionDetail(41)!)

    it('shows the beacon before the answer, swaps the temp id and offers «Мои маячки»', async () => {
      signIn()
      const { gate, release } = deferred()
      server.use(
        http.post('*/api/v1/beacons', async ({ request }) => {
          await gate
          const body = (await request.json()) as { editionId: number }
          const created: BeaconResponse = {
            id: 99,
            editionId: body.editionId,
            title: 'Мастер и Маргарита',
            status: 'ACTIVE',
          }
          setMockBeacons([...mockBeacons, created])
          return HttpResponse.json(created, { status: 201 })
        }),
      )
      const { result } = setup(useCreateBeacon)
      await waitFor(() => expect(list()).toHaveLength(5))

      act(() => {
        result.current.mutation.mutate({
          request: { editionId: 41, conditionType: 'BELOW_PRICE', targetPrice: 2840 },
          book: book(),
        })
      })
      await waitFor(() => expect(list()).toHaveLength(6))
      expect(list()![0]?.status).toBe('TRIGGERED')
      expect(list()![1]?.id).toBeLessThan(0)
      expect(list()![1]?.title).toBe('Мастер и Маргарита')
      expect(forEdition(41)?.id).toBeLessThan(0)

      release()
      await waitFor(() => expect(toasts().map((t) => t.message)).toContain('Маячок поставлен'))
      expect(list()!.filter((b) => b.editionId === 41)).toHaveLength(1)
      expect(list()!.some((b) => (b.id ?? 0) < 0)).toBe(false)
      const item = toasts().find((t) => t.message === 'Маячок поставлен')!
      expect(item.action?.label).toBe('Мои маячки')
      act(() => item.action!.onClick())
      expect(location).toBe('/beacons')
    })

    it('rolls back and reports a failed save', async () => {
      signIn()
      server.use(http.post('*/api/v1/beacons', serverError))
      const { result } = setup(useCreateBeacon)
      await waitFor(() => expect(list()).toHaveLength(5))
      await act(async () => {
        await result.current.mutation
          .mutateAsync({ request: { editionId: 41, conditionType: 'ANY_DROP' }, book: book() })
          .catch(() => undefined)
      })
      expect(list()!.some((b) => b.editionId === 41)).toBe(false)
      expect(forEdition(41) ?? null).toBeNull()
      expect(toasts().map((t) => t.message)).toContain('Не удалось сохранить. Изменения отменены.')
    })

    it('reports a duplicate with an «Изменить» action', async () => {
      signIn()
      const { result } = setup(useCreateBeacon)
      await waitFor(() => expect(list()).toHaveLength(5))
      await act(async () => {
        await result.current.mutation
          .mutateAsync({
            request: { editionId: 16, conditionType: 'ANY_DROP' },
            book: { ...book(), editionId: 16 },
          })
          .catch(() => undefined)
      })
      expect(list()).toHaveLength(5)
      const item = toasts().find((t) => t.message === 'На эту книгу маячок уже стоит')
      expect(item?.kind).toBe('info')
      expect(item?.action?.label).toBe('Изменить')
    })

    it('rolls back silently on a validation error', async () => {
      signIn()
      const { result } = setup(useCreateBeacon)
      await waitFor(() => expect(list()).toHaveLength(5))
      await act(async () => {
        await result.current.mutation
          .mutateAsync({ request: { editionId: 41, conditionType: 'BELOW_PRICE' }, book: book() })
          .catch(() => undefined)
      })
      expect(list()).toHaveLength(5)
      expect(toasts()).toHaveLength(0)
    })

    it('refetches the lists once it settles', async () => {
      signIn()
      let gets = 0
      server.use(
        http.get('*/api/v1/beacons', () => {
          gets += 1
          return HttpResponse.json([])
        }),
      )
      const { result } = setup(useCreateBeacon)
      await waitFor(() => expect(gets).toBe(1))
      await act(async () => {
        await result.current.mutation.mutateAsync({
          request: { editionId: 41, conditionType: 'ANY_DROP' },
          book: book(),
        })
      })
      await waitFor(() => expect(gets).toBe(2))
    })
  })

  describe('update', () => {
    it('pauses before the answer with the exact body', async () => {
      signIn()
      const { gate, release } = deferred()
      let body: unknown
      server.use(
        http.patch('*/api/v1/beacons/:id', async ({ request }) => {
          body = await request.json()
          await gate
          return HttpResponse.json({ id: 2, editionId: 7, status: 'PAUSED' })
        }),
      )
      const { result } = setup(useUpdateBeacon)
      await waitFor(() => expect(list()).toHaveLength(5))
      act(() => {
        result.current.mutation.mutate({ id: 2, editionId: 7, patch: { status: 'PAUSED' } })
      })
      await waitFor(() => expect(list()!.find((b) => b.id === 2)?.status).toBe('PAUSED'))
      expect(forEdition(7)?.status).toBe('PAUSED')
      await waitFor(() => expect(body).toEqual({ status: 'PAUSED' }))
      release()
      await waitFor(() => expect(result.current.mutation.isSuccess).toBe(true))
      expect(toasts()).toHaveLength(0)
    })

    it('goes back to ACTIVE and reports on a server error', async () => {
      signIn()
      server.use(http.patch('*/api/v1/beacons/:id', serverError))
      const { result } = setup(useUpdateBeacon)
      await waitFor(() => expect(list()).toHaveLength(5))
      await act(async () => {
        await result.current.mutation
          .mutateAsync({ id: 2, editionId: 7, patch: { status: 'PAUSED' } })
          .catch(() => undefined)
      })
      expect(list()!.find((b) => b.id === 2)?.status).toBe('ACTIVE')
      expect(toasts().map((t) => t.message)).toContain('Не удалось сохранить. Изменения отменены.')
    })
  })

  describe('books', () => {
    it('builds the book from an edition', () => {
      const book = bookFromEdition(editionDetail(41)!)
      expect(book.editionId).toBe(41)
      expect(book.bestPrice).toBe(3150)
      expect(book.subtitle).not.toBe('')
    })

    it('uses the author as the subtitle for a beacon', () => {
      const book = bookFromBeacon({
        editionId: 7,
        title: 'Атомные привычки',
        author: 'Джеймс Клир',
        currentBestPrice: 5290,
      })
      expect(book).toMatchObject({ editionId: 7, subtitle: 'Джеймс Клир', bestPrice: 5290 })
    })
  })
})
