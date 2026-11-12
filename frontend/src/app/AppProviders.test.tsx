import { QueryClient } from '@tanstack/react-query'
import { act, render } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { createQueryClient } from './queryClient'
import { AppProviders } from './AppProviders'
import { useAuthStore } from '../stores/auth'

vi.mock('./queryClient', async (importOriginal) => ({
  ...(await importOriginal<typeof import('./queryClient')>()),
  createQueryClient: vi.fn(),
}))

const user = { id: 1, email: 'reader@example.com', name: 'Reader', role: 'USER' as const }

function setup() {
  const client = new QueryClient()
  vi.mocked(createQueryClient).mockReturnValue(client)
  client.setQueryData(['me'], user)
  client.setQueryData(['search', 'popular', 8], ['x'])
  render(<AppProviders>{null}</AppProviders>)
  return client
}

describe('AppProviders', () => {
  it('drops user-scoped caches on sign-out and keeps public ones', () => {
    useAuthStore.getState().setSession({ accessToken: 'a', user })
    const client = setup()
    act(() => useAuthStore.getState().clearSession())
    expect(client.getQueryData(['me'])).toBeUndefined()
    expect(client.getQueryData(['search', 'popular', 8])).toEqual(['x'])
  })

  it('removes nothing when anonymous stays anonymous', () => {
    const client = setup()
    act(() => useAuthStore.getState().clearSession())
    expect(client.getQueryData(['me'])).toEqual(user)
  })
})
