import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it } from 'vitest'
import type { BeaconResponse } from '../../api/types'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { useBeaconForEdition } from './useBeaconForEdition'

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useBeaconForEdition', () => {
  let requests: string[]
  beforeEach(() => {
    requests = []
  })

  function track(body: BeaconResponse[]) {
    server.use(
      http.get('*/api/v1/beacons', ({ request }) => {
        requests.push(new URL(request.url).search)
        return HttpResponse.json(body)
      }),
    )
  }

  it('does not request anything for an anonymous user', async () => {
    track([])
    useAuthStore.setState({ status: 'anonymous' })
    const { result } = renderHook(() => useBeaconForEdition(41), { wrapper })
    await new Promise((resolve) => setTimeout(resolve, 20))
    expect(requests).toEqual([])
    expect(result.current.data).toBeUndefined()
  })

  it('returns null when an authenticated user has no beacon', async () => {
    track([])
    useAuthStore.setState({ status: 'authenticated' })
    const { result } = renderHook(() => useBeaconForEdition(41), { wrapper })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(requests).toHaveLength(1)
    expect(requests[0]).toContain('editionId=41')
    expect(result.current.data).toBeNull()
  })

  it('returns the beacon when one exists', async () => {
    track([{ id: 5, editionId: 41 } as BeaconResponse])
    useAuthStore.setState({ status: 'authenticated' })
    const { result } = renderHook(() => useBeaconForEdition(41), { wrapper })
    await waitFor(() => expect(result.current.data?.id).toBe(5))
  })
})
