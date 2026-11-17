import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { queryKeys } from '../../api/queryKeys'
import type { BeaconResponse } from '../../api/types'
import { mockBeacons, setMockBeacons } from '../../mocks/beaconFixtures'
import { MOCK_ACCESS_TOKEN } from '../../mocks/authFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { useToastStore } from '../../stores/toast'
import { pendingDeletes, useBeacons } from './useBeacons'
import { useDeleteBeacon } from './useDeleteBeacon'

let client: QueryClient
function wrapper({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={client}>
      <MemoryRouter>{children}</MemoryRouter>
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

const toasts = () => useToastStore.getState().toasts
const list = () => client.getQueryData<BeaconResponse[]>(queryKeys.beacons)
const ids = () => list()!.map((b) => b.id)

let deletes: number[]
function trackDeletes(status = 204) {
  server.use(
    http.delete('*/api/v1/beacons/:id', ({ params }) => {
      deletes.push(Number(params.id))
      if (status === 204) setMockBeacons(mockBeacons.filter((b) => b.id !== Number(params.id)))
      return status === 204
        ? new HttpResponse(null, { status })
        : HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status })
    }),
  )
}

function setup() {
  return renderHook(
    () => {
      const { data } = useBeacons()
      return { beacons: { data }, del: useDeleteBeacon() }
    },
    { wrapper },
  )
}

async function loaded(result: ReturnType<typeof setup>['result']) {
  await waitFor(() => expect(result.current.beacons.data).toHaveLength(5))
  return result.current.beacons.data!.find((b) => b.id === 3)!
}

describe('useDeleteBeacon', () => {
  beforeEach(() => {
    client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: Infinity } } })
    useToastStore.setState({ toasts: [] })
    pendingDeletes.clear()
    deletes = []
    signIn()
    trackDeletes()
  })
  afterEach(async () => {
    vi.useRealTimers()
    cleanup()
    await waitFor(() => expect(pendingDeletes.size).toBe(0))
  })

  it('removes the row at once and offers «Отменить» without sending anything', async () => {
    const { result } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    expect(ids()).toEqual([1, 2, 4, 5])
    expect(toasts()[0]?.message).toBe('Маячок удалён')
    expect(toasts()[0]?.action?.label).toBe('Отменить')
    expect(deletes).toEqual([])
  })

  it('restores the row at the same index on undo and never sends a request', async () => {
    const { result } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    const item = toasts()[0]!
    act(() => {
      item.action!.onClick()
      useToastStore.getState().dismiss(item.id, 'action')
    })
    expect(ids()).toEqual([1, 2, 3, 4, 5])
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(deletes).toEqual([])
  })

  it('sends exactly one DELETE when the toast times out and refetches without the row', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    const { result } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5100)
    })
    await waitFor(() => expect(deletes).toEqual([3]))
    await waitFor(() => expect(pendingDeletes.size).toBe(0))
    await waitFor(() => expect(ids()).toEqual([1, 2, 4, 5]))
    expect(result.current.beacons.data!.map((b) => b.id)).toEqual([1, 2, 4, 5])
  })

  it('sends the DELETE when the toast is dismissed', async () => {
    const { result } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    act(() => useToastStore.getState().dismiss(toasts()[0]!.id))
    await waitFor(() => expect(deletes).toEqual([3]))
  })

  it('flushes on unmount at once and removes the toast silently', async () => {
    const { result, unmount } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    unmount()
    expect(toasts()).toHaveLength(0)
    await waitFor(() => expect(deletes).toEqual([3]))
  })

  it('sends nothing on unmount after sign-out', async () => {
    const { result, unmount } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    act(() => useAuthStore.getState().clearSession())
    unmount()
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(deletes).toEqual([])
    expect(pendingDeletes.size).toBe(0)
  })

  it('brings the beacon back with a toast when the DELETE fails', async () => {
    trackDeletes(500)
    const { result } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    act(() => useToastStore.getState().dismiss(toasts()[0]!.id))
    await waitFor(() =>
      expect(toasts().map((t) => t.message)).toContain('Не удалось удалить маячок'),
    )
    await waitFor(() => expect(result.current.beacons.data).toHaveLength(5))
  })

  it('keeps the removed row hidden when the list is refetched during the undo window', async () => {
    const { result } = setup()
    const beacon = await loaded(result)
    act(() => result.current.del.remove(beacon))
    await act(async () => {
      await client.invalidateQueries({ queryKey: ['beacons'] })
    })
    await waitFor(() => expect(result.current.beacons.data!.map((b) => b.id)).toEqual([1, 2, 4, 5]))
    expect(client.getQueryState(queryKeys.beacons)?.dataUpdateCount).toBeGreaterThan(2)
    expect(ids()).toEqual([1, 2, 3, 4, 5])
    expect(deletes).toEqual([])
  })
})
