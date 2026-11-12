import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import { HttpResponse, http } from 'msw'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { MOCK_ACCESS_TOKEN } from '../../mocks/authFixtures'
import { server } from '../../mocks/server'
import { useAuthStore } from '../../stores/auth'
import { useMe } from './useMe'

function wrapper() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
}

describe('useMe', () => {
  it('starts with the store user and then follows the /me response', async () => {
    useAuthStore.getState().setSession({
      accessToken: MOCK_ACCESS_TOKEN,
      user: { id: 7, email: 'old@example.com', name: 'Старое', role: 'USER' },
    })
    server.use(
      http.get('*/api/v1/me', () =>
        HttpResponse.json({ id: 7, email: 'new@example.com', name: 'Новое', role: 'USER' }),
      ),
    )
    const { result } = renderHook(() => useMe(), { wrapper: wrapper() })
    expect(result.current.data?.name).toBe('Старое')
    await waitFor(() => expect(result.current.data?.name).toBe('Новое'))
    expect(useAuthStore.getState().user?.name).toBe('Новое')
  })

  it('does not request /me for a guest', async () => {
    let requests = 0
    server.use(
      http.get('*/api/v1/me', () => {
        requests += 1
        return HttpResponse.json({})
      }),
    )
    const { result } = renderHook(() => useMe(), { wrapper: wrapper() })
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(requests).toBe(0)
    expect(result.current.data).toBeUndefined()
  })
})
